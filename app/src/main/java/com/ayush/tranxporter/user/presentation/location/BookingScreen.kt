package com.ayush.tranxporter.user.presentation.location

import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ayush.tranxporter.R
import com.ayush.tranxporter.core.presentation.util.PermissionUtils.getAddressFromLocation
import com.ayush.tranxporter.user.presentation.bookingdetails.BookingDetailsViewModel
import com.ayush.tranxporter.user.presentation.bookingdetails.TruckType
import com.ayush.tranxporter.utils.calculateFare
import com.ayush.tranxporter.utils.getDrivingDistance
import com.ayush.tranxporter.utils.getRoutePoints
import com.ayush.tranxporter.utils.getTravelTime
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalTime

data class BookingScreen(
    @Transient val initialPickup: LatLng? = null,
    @Transient val initialDropoff: LatLng? = null,
    @Transient val locationType: String? = null
) : Screen {
    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        
        val navigator = LocalNavigator.current
        val bookingDetailsViewModel: BookingDetailsViewModel = koinViewModel()
        val locationViewModel: LocationSelectionViewModel = koinViewModel()
        
        var pickupLocation by remember(locationViewModel) {
            mutableStateOf(locationViewModel.pickupLocation?.latLng ?: initialPickup)
        }
        var dropOffLocation by remember(locationViewModel) {
            mutableStateOf(locationViewModel.dropLocation?.latLng ?: initialDropoff)
        }
        var bookingDetails by remember(bookingDetailsViewModel) {
            mutableStateOf(bookingDetailsViewModel.getSubmittedDetails())
        }
        var travelTime by remember { mutableStateOf<String?>(null) }
        var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
        var drivingDistance by remember { mutableStateOf<Double?>(null) }
        var smallTruckFare by remember { mutableStateOf<Double?>(null) }
        var largeTruckFare by remember { mutableStateOf<Double?>(null) }
        var hasAnimatedToInitialLocation by remember { mutableStateOf(false) }
        
        val permissionState = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val cameraPositionState = rememberCameraPositionState()
        var currentLocation by remember { mutableStateOf<LatLng?>(null) }

        val fusedLocationClient = remember {
            LocationServices.getFusedLocationProviderClient(context)
        }

        // Initial setup and validation
        LaunchedEffect(Unit) {
            Log.d(TAG, "Initializing screen")

            // Validate booking details
            val details = bookingDetailsViewModel.getSubmittedDetails()
            if (details == null) {
                Toast.makeText(context, "Please complete booking details first", Toast.LENGTH_LONG).show()
                navigator?.pop()
                return@LaunchedEffect
            }
            bookingDetails = details

            // Handle initial camera position
            if (!hasAnimatedToInitialLocation) {
                when {
                    initialPickup != null && initialDropoff != null -> {
                        val bounds = LatLngBounds.builder()
                            .include(initialPickup)
                            .include(initialDropoff)
                            .build()
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(bounds, 100),
                            durationMs = 1000
                        )
                    }
                    initialPickup != null -> animateCamera(initialPickup, cameraPositionState)
                    initialDropoff != null -> animateCamera(initialDropoff, cameraPositionState)
                    permissionState.allPermissionsGranted -> {
                        try {
                            val location = fusedLocationClient.lastLocation.await()
                            location?.let {
                                animateCamera(LatLng(it.latitude, it.longitude), cameraPositionState)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting initial location", e)
                            animateCamera(DEFAULT_LOCATION, cameraPositionState)
                        }
                    }
                    else -> animateCamera(DEFAULT_LOCATION, cameraPositionState)
                }
                hasAnimatedToInitialLocation = true
            }
        }

        // Location changes observer
        LaunchedEffect(locationViewModel.pickupLocation, locationViewModel.dropLocation) {
            Log.d(TAG, "Locations updated in ViewModel")
            pickupLocation = locationViewModel.pickupLocation?.latLng
            dropOffLocation = locationViewModel.dropLocation?.latLng
        }

        // Route and fare calculation
        LaunchedEffect(pickupLocation, dropOffLocation) {
            if (pickupLocation != null && dropOffLocation != null) {
                try {
                    // Calculate route
                    routePoints = getRoutePoints(pickupLocation!!, dropOffLocation!!)
                    drivingDistance = getDrivingDistance(pickupLocation!!, dropOffLocation!!)
                    travelTime = getTravelTime(pickupLocation!!, dropOffLocation!!)

                    // Update camera to show route
                    val bounds = LatLngBounds.builder().apply {
                        routePoints.forEach { include(it) }
                    }.build()
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100))

                    // Calculate fares
                    bookingDetails?.let { details ->
                        smallTruckFare = calculateFare(
                            start = pickupLocation!!,
                            end = dropOffLocation!!,
                            bookingDetails = details.copy(truckType = TruckType.LORRY)
                        )
                        largeTruckFare = calculateFare(
                            start = pickupLocation!!,
                            end = dropOffLocation!!,
                            bookingDetails = details.copy(truckType = TruckType.SIXTEEN_WHEELER)
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating route and fares", e)
                }
            }
        }

        // Current location observer
        LaunchedEffect(permissionState.allPermissionsGranted) {
            if (permissionState.allPermissionsGranted &&
                locationViewModel.isUsingCurrentLocation &&
                locationViewModel.pickupLocation == null &&
                locationType?.lowercase() != "drop"
            ) {
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        val address = getAddressFromLocation(context, latLng)
                        locationViewModel.updateCurrentLocation(latLng, address)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting current location", e)
                    Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (pickupLocation == null) "Select Pickup" else "Select Drop-off",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(
                                onClick = { /* Enable location edit */ },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit location",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator?.pop() },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            },
//        floatingActionButton = {
//            if (pickupLocation != null && dropOffLocation != null) {
//                FloatingActionButton(
//                    onClick = { /* Handle booking */ },
//                    containerColor = MaterialTheme.colorScheme.primary
//                ) {
//                    Icon(Icons.Default.Check, contentDescription = "Book Now")
//                }
//            }
//        }
        ) { paddingValues ->
            if (permissionState.allPermissionsGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = true),
                        uiSettings = MapUiSettings(myLocationButtonEnabled = false),
                        onMapClick = { latLng ->
                            scope.launch {
                                val address = getAddressFromLocation(context, latLng)
                                when (locationType?.lowercase()) {
                                    "pickup" -> {
                                        locationViewModel.setPickupLocation(latLng, address)
                                        navigator?.pop()
                                    }

                                    "drop" -> {
                                        locationViewModel.setDropLocation(latLng, address)
                                        navigator?.pop()
                                    }

                                    else -> {
                                        // Normal booking flow
                                        when {
                                            locationViewModel.pickupLocation == null -> {
                                                locationViewModel.setPickupLocation(latLng, address)
                                            }

                                            locationViewModel.dropLocation == null -> {
                                                locationViewModel.setDropLocation(latLng, address)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        pickupLocation?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "Pickup Location",
                                snippet = "Tap to change",
                                onInfoWindowClick = {
                                    locationViewModel.setPickupLocation(
                                        null,
                                        ""
                                    ) // Add this method to ViewModel
                                    pickupLocation = null
                                }
                            )
                        }
                        dropOffLocation?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "Drop-off Location",
                                snippet = "Tap to change",
                                onInfoWindowClick = {
                                    locationViewModel.setDropLocation(
                                        null,
                                        ""
                                    ) // Add this method to ViewModel
                                    dropOffLocation = null
                                }
                            )
                        }
                        // Inside GoogleMap composable
                        if (routePoints.isNotEmpty()) {
                            Polyline(
                                points = routePoints,
                                color = Color.Blue,
                                width = 4f,
                                pattern = listOf(
                                    Dot(),
                                    Gap(10f)
                                )
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                end = 16.dp,
                                bottom = 96.dp
                            ),  // Adjust bottom padding based on your UI
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        FloatingActionButton(
                            onClick = {
                                // Get current location and animate camera
                                scope.launch {
                                    try {
                                        val fusedLocationClient =
                                            LocationServices.getFusedLocationProviderClient(context)
                                        val location = fusedLocationClient.lastLocation.await()
                                        location?.let {
                                            val cameraPosition = CameraPosition.Builder()
                                                .target(LatLng(it.latitude, it.longitude))
                                                .zoom(15f)
                                                .build()
                                            cameraPositionState.animate(
                                                update = CameraUpdateFactory.newCameraPosition(
                                                    cameraPosition
                                                ),
                                                durationMs = 500
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Location", "Error getting location", e)
                                        // Optionally show error message to user
                                        Toast.makeText(
                                            context,
                                            "Unable to get current location",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 6.dp
                            )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.location),
                                contentDescription = "My Location",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Instructions Overlay
                    if (pickupLocation == null || dropOffLocation == null) {
                        val instruction = when {
                            pickupLocation == null -> "Tap on the map to select your pickup location."
                            dropOffLocation == null -> "Tap on the map to select your drop-off location."
                            else -> ""
                        }
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (pickupLocation != null && dropOffLocation != null && bookingDetails != null) {
                                Log.d("BookingScreen", "Showing bottom card")
                                VehicleSelectionCard(
                                    drivingDistance = drivingDistance,
                                    travelTime = travelTime,
                                    smallTruckFare = smallTruckFare,
                                    largeTruckFare = largeTruckFare,
                                    onBack = {
                                        locationViewModel.setDropLocation(null, "")
                                        dropOffLocation = null
                                    },
                                    selectedTruck = bookingDetails?.truckType ?: TruckType.PICKUP
                                )
                            } else {
                                Log.d("BookingScreen", "Bottom card conditions not met: " +
                                        "Pickup=${pickupLocation != null}, " +
                                        "Dropoff=${dropOffLocation != null}, " +
                                        "Details=${bookingDetails != null}")
                            }
                        }
                    }

                    // Combined bottom card UI
                    if (pickupLocation != null && dropOffLocation != null && bookingDetails != null) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            VehicleSelectionCard(
                                drivingDistance = drivingDistance,
                                travelTime = travelTime,
                                smallTruckFare = smallTruckFare,
                                largeTruckFare = largeTruckFare,
                                onBack = {
                                    locationViewModel.setDropLocation(null, "")
                                    dropOffLocation = null
                                },
                                selectedTruck = bookingDetails?.truckType ?: TruckType.PICKUP

                            )
                        }
                    } else {
                        Log.d("BookingScreen", "Bottom card conditions not met - " +
                                "Pickup: $pickupLocation, Dropoff: $dropOffLocation, Details: $bookingDetails")
                    }
                }

            }
        }
    }
    companion object {
        private const val TAG = "BookingScreen"
        private val DEFAULT_LOCATION = LatLng(20.5937, 78.9629)

        private suspend fun animateCamera(
            location: LatLng,
            cameraPositionState: CameraPositionState,
            zoom: Float = 15f
        ) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location, zoom),
                durationMs = 1000
            )
        }
    }
}