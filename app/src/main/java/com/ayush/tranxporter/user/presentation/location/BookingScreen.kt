package com.ayush.tranxporter.user.presentation.location

import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ayush.tranxporter.R
import com.ayush.tranxporter.core.presentation.util.PermissionUtils.getAddressFromLocation
import com.ayush.tranxporter.user.presentation.bookingdetails.BookingDetailsViewModel
import com.ayush.tranxporter.user.presentation.bookingdetails.TransportItemDetails
import com.ayush.tranxporter.user.presentation.bookingdetails.TruckType
import com.ayush.tranxporter.utils.VibratorService
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
import java.time.format.DateTimeFormatter

data class BookingScreen(
    @Transient
    val initialPickup: LatLng? = null,
    @Transient
    val initialDropoff: LatLng? = null,
    @Transient
    val locationType: String? = null
) : Screen {
    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val bookingDetailsViewModel: BookingDetailsViewModel = koinViewModel()
        val locationViewModel: LocationSelectionViewModel = koinViewModel()

        val permissionState = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        val context = LocalContext.current

        val coroutineScope = rememberCoroutineScope()
        val cameraPositionState = rememberCameraPositionState()
        var currentLocation by remember { mutableStateOf<LatLng?>(null) }


        var pickupLocation by remember(locationViewModel.pickupLocation) {
            mutableStateOf(locationViewModel.pickupLocation?.latLng)
        }
        var dropOffLocation by remember(locationViewModel.dropLocation) {
            mutableStateOf(locationViewModel.dropLocation?.latLng)
        }
        val fusedLocationClient = remember {
            LocationServices.getFusedLocationProviderClient(context)
        }
        var travelTime by remember { mutableStateOf<String?>(null) }
        var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

        val scope = rememberCoroutineScope()
        var hasAnimatedToInitialLocation by remember { mutableStateOf(false) }


//    LaunchedEffect(Unit) {
//        when (locationType?.lowercase()) {
//            "pickup" -> {
//                // Do nothing, wait for map selection
//            }
//
//            "drop" -> {
//                // Do nothing, wait for map selection
//            }
//
//            else -> {
//                // Only initialize if we're coming from deep link or direct navigation
//                if (initialPickup != null && initialDropoff != null) {
//                    try {
//                        val points = getRoutePoints(initialPickup, initialDropoff)
//                        routePoints = points
//                        travelTime = getTravelTime(initialPickup, initialDropoff)
//
//                        val bounds = LatLngBounds.builder()
//                            .include(initialPickup)
//                            .include(initialDropoff)
//                            .build()
//                        cameraPositionState.animate(
//                            CameraUpdateFactory.newLatLngBounds(bounds, 100)
//                        )
//                    } catch (e: Exception) {
//                        Log.e("Route", "Failed to initialize route", e)
//                    }
//                }
//            }
//        }
//    }

        var drivingDistance by remember { mutableStateOf<Double?>(null) }
        var smallTruckFare by remember { mutableStateOf<Double?>(null) }
        var largeTruckFare by remember { mutableStateOf<Double?>(null) }
        var bookingDetails by remember { mutableStateOf<TransportItemDetails?>(null) }

        LaunchedEffect(pickupLocation, dropOffLocation) {
            if (pickupLocation != null && dropOffLocation != null) {
                try {
                    drivingDistance = null
                    smallTruckFare = null
                    largeTruckFare = null

                    val points = getRoutePoints(pickupLocation!!, dropOffLocation!!)
                    routePoints = points

                    // Get driving distance first
                    drivingDistance = getDrivingDistance(pickupLocation!!, dropOffLocation!!)

                    // Get booking details from BookingDetailsViewModel
                    val details = bookingDetailsViewModel.getSubmittedDetails()
                    if (details == null) {
                        Toast.makeText(
                            context,
                            "Please complete booking details first",
                            Toast.LENGTH_LONG
                        ).show()
                        navigator?.pop()
                        return@LaunchedEffect
                    }

                    // Update the global bookingDetails
                    bookingDetails = details

                    // Use the local 'details' variable for calculations
                    smallTruckFare = calculateFare(
                        start = pickupLocation!!,
                        end = dropOffLocation!!,
                        bookingDetails = details.copy(truckType = TruckType.LORRY),
                        timeOfDay = LocalTime.now()
                    )

                    largeTruckFare = calculateFare(
                        start = pickupLocation!!,
                        end = dropOffLocation!!,
                        bookingDetails = details.copy(truckType = TruckType.SIXTEEN_WHEELER),
                        timeOfDay = LocalTime.now()
                    )

                } catch (e: Exception) {
                    Log.e("Route", "Failed to get route or calculate fare", e)
                }
            }
        }



        LaunchedEffect(pickupLocation, dropOffLocation) {
            if (pickupLocation != null && dropOffLocation != null) {
                travelTime = getTravelTime(pickupLocation!!, dropOffLocation!!)
            }
        }

        LaunchedEffect(Unit) {
            if (!hasAnimatedToInitialLocation) {
                try {
                    if (permissionState.allPermissionsGranted) {
                        val location = fusedLocationClient.lastLocation.await()
                        location?.let {
                            val initialLatLng = LatLng(it.latitude, it.longitude)
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(
                                    initialLatLng,
                                    15f
                                ),
                                durationMs = 1000
                            )
                            hasAnimatedToInitialLocation = true
                        }
                    } else {
                        // If no permission, animate to a default location in India
                        val defaultLocation = LatLng(20.5937, 78.9629) // Center of India
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(
                                defaultLocation,
                                5f
                            ),
                            durationMs = 1000
                        )
                        hasAnimatedToInitialLocation = true
                    }
                } catch (e: Exception) {
                    Log.e("Location", "Error getting initial location", e)
                    // Animate to default location in case of error
                    val defaultLocation = LatLng(20.5937, 78.9629) // Center of India
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(
                            defaultLocation,
                            5f
                        ),
                        durationMs = 1000
                    )
                    hasAnimatedToInitialLocation = true
                }
            }
        }

        // Update the existing LaunchedEffect for permission state
        LaunchedEffect(permissionState.allPermissionsGranted) {
            if (permissionState.allPermissionsGranted &&
                locationViewModel.isUsingCurrentLocation &&
                locationViewModel.pickupLocation == null &&
                locationType?.lowercase() != "drop"
            ) {
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    location?.let {
                        currentLocation = LatLng(it.latitude, it.longitude)
                        val address = getAddressFromLocation(context, currentLocation!!)
                        locationViewModel.updateCurrentLocation(currentLocation!!, address)
                        if (!hasAnimatedToInitialLocation) {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f),
                                durationMs = 1000
                            )
                            hasAnimatedToInitialLocation = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Location", "Error getting current location", e)
                    Toast.makeText(
                        context,
                        "Unable to get current location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Add a check for initial locations from navigation
        LaunchedEffect(initialPickup, initialDropoff) {
            when {
                initialPickup != null && initialDropoff != null -> {
                    try {
                        val points = getRoutePoints(initialPickup, initialDropoff)
                        routePoints = points
                        travelTime = getTravelTime(initialPickup, initialDropoff)

                        val bounds = LatLngBounds.builder()
                            .include(initialPickup)
                            .include(initialDropoff)
                            .build()
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(bounds, 100),
                            durationMs = 1000
                        )
                        hasAnimatedToInitialLocation = true
                    } catch (e: Exception) {
                        Log.e("Route", "Failed to initialize route", e)
                    }
                }

                initialPickup != null -> {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(initialPickup, 15f),
                        durationMs = 1000
                    )
                    hasAnimatedToInitialLocation = true
                }

                initialDropoff != null -> {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(initialDropoff, 15f),
                        durationMs = 1000
                    )
                    hasAnimatedToInitialLocation = true
                }
            }
        }
        LaunchedEffect(routePoints) {
            if (routePoints.isNotEmpty()) {
                val bounds = LatLngBounds.builder().apply {
                    routePoints.forEach { include(it) }
                }.build()
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 100)
                )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(paddingValues)
                        ) {
                            Text(
                                text = instruction,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                    .padding(8.dp)
                            )
                        }
                    }

                    // Combined bottom card UI
                    if (pickupLocation != null && dropOffLocation != null) {

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
                                selectedTruck =  bookingDetails?.truckType ?: TruckType.PICKUP

                            )
                        }


                    }
                }

            }
        }
    }
}
