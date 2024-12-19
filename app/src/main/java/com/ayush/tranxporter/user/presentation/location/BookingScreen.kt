package com.ayush.tranxporter.user.presentation.location

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import com.ayush.tranxporter.core.presentation.util.PermissionUtils.getAddressFromLocation
import com.ayush.tranxporter.user.presentation.bookingdetails.BookingDetailsViewModel
import com.ayush.tranxporter.user.presentation.bookingdetails.TransportItemDetails
import com.ayush.tranxporter.user.presentation.bookingdetails.TruckType
import com.ayush.tranxporter.utils.calculateFare
import com.ayush.tranxporter.utils.getDrivingDistance
import com.ayush.tranxporter.utils.getRoutePoints
import com.ayush.tranxporter.utils.getTravelTime
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.compose.viewmodel.koinViewModel

data class BookingScreen(
    @Transient val initialPickup: LatLng? = null,
    @Transient val initialDropoff: LatLng? = null,
    @Transient val locationType: String? = null
) : Screen {
    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        // 1. Group related declarations
        val context = LocalContext.current
        val navigator = LocalNavigator.current
        val scope = rememberCoroutineScope()

        // ViewModels
        val bookingDetailsViewModel: BookingDetailsViewModel = koinViewModel()
        val locationViewModel: LocationSelectionViewModel = koinViewModel()

        // Location-related declarations
        val permissionState = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        val fusedLocationClient = remember {
            LocationServices.getFusedLocationProviderClient(context)
        }
        val cameraPositionState = rememberCameraPositionState()

        // 2. Group state declarations
        var currentLocation by remember { mutableStateOf<LatLng?>(null) }
        var pickupLocation by remember(locationViewModel) {
            mutableStateOf(locationViewModel.pickupLocation?.latLng ?: initialPickup)
        }
        var dropOffLocation by remember(locationViewModel) {
            mutableStateOf(locationViewModel.dropLocation?.latLng ?: initialDropoff)
        }
        var bookingDetails by remember(bookingDetailsViewModel) {
            mutableStateOf(bookingDetailsViewModel.getSubmittedDetails())
        }

        // Route and pricing states
        var travelTime by remember { mutableStateOf<String?>(null) }
        var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
        var drivingDistance by remember { mutableStateOf<Double?>(null) }
        var smallTruckFare by remember { mutableStateOf<Double?>(null) }
        var largeTruckFare by remember { mutableStateOf<Double?>(null) }

        val hasAnimatedToInitialLocation by remember { mutableStateOf(false) }
        val bottomSheetState = rememberModalBottomSheetState(
        )

        // Initial setup and validation
        LaunchedEffect(Unit) {
            initializeScreen(
                bookingDetailsViewModel = bookingDetailsViewModel,
                context = context,
                navigator = navigator,
                cameraPositionState = cameraPositionState,
                permissionState = permissionState,
                fusedLocationClient = fusedLocationClient,
                onBookingDetailsUpdate = { bookingDetails = bookingDetailsViewModel.getSubmittedDetails() },
                hasAnimatedToInitialLocation = hasAnimatedToInitialLocation
            )
        }

        LaunchedEffect(locationViewModel.pickupLocation, locationViewModel.dropLocation) {
            pickupLocation = locationViewModel.pickupLocation?.latLng
            dropOffLocation = locationViewModel.dropLocation?.latLng
        }

        LaunchedEffect(pickupLocation, dropOffLocation) {
            updateRouteAndFares(
                pickupLocation = pickupLocation,
                dropOffLocation = dropOffLocation,
                bookingDetails = bookingDetails,
                onRouteUpdate = { points, distance, time ->
                    routePoints = points
                    drivingDistance = distance
                    travelTime = time
                },
                onFareUpdate = { small, large ->
                    smallTruckFare = small
                    largeTruckFare = large
                },
                cameraPositionState = cameraPositionState
            )
        }

        LaunchedEffect(permissionState.allPermissionsGranted) {
            handleLocationPermission(
                permissionState = permissionState,
                locationViewModel = locationViewModel,
                fusedLocationClient = fusedLocationClient,
                context = context,
                locationType = locationType,
                scope = scope
            )
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
                                text = if (pickupLocation == null) "Select Pickup"
                                else if (dropOffLocation == null) "Select Drop Location"
                                else "Tap pointer to reselect locations",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(
                                onClick = {
                                    navigator?.pop()
                                },
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
                    MapContent(
                        cameraPositionState = cameraPositionState,
                        pickupLocation = pickupLocation,
                        dropOffLocation = dropOffLocation,
                        routePoints = routePoints,
                        onMapClick = { latLng ->
                            scope.launch {
                                handleMapClick(
                                    latLng = latLng,
                                    context = context,
                                    locationViewModel = locationViewModel,
                                    navigator = navigator,
                                    locationType = locationType
                                )
                            }
                        },
                        onPickupMarkerClick = {
                            locationViewModel.setPickupLocation(null, "")
                            pickupLocation = null
                        },
                        onDropoffMarkerClick = {
                            locationViewModel.setDropLocation(null, "")
                            dropOffLocation = null
                        }
                    )

                    LocationButton(
                        onClick = {
                            scope.launch {
                                handleCurrentLocationClick(
                                    context = context,
                                    fusedLocationClient = fusedLocationClient,
                                    cameraPositionState = cameraPositionState
                                )
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp, bottom = 96.dp)
                    )
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (pickupLocation == null || dropOffLocation == null) {
                            // Show instructions
                            Text(
                                text = when {
                                    pickupLocation == null -> "Tap on the map to select your pickup location."
                                    else -> "Tap on the map to select your drop-off location."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                    .padding(8.dp)
                            )
                        } else if (bookingDetails != null) {
                            // Show vehicle selection card
                            VehicleSelectionCard(
                                drivingDistance = drivingDistance,
                                travelTime = travelTime,
                                smallTruckFare = smallTruckFare,
                                largeTruckFare = largeTruckFare,
                                onBack = {
                                    locationViewModel.setDropLocation(null, "")
                                    dropOffLocation = null
                                },
                                selectedTruck = bookingDetails!!.truckType
                            )
                        }
                    }

                }

            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    private suspend fun initializeScreen(
        bookingDetailsViewModel: BookingDetailsViewModel,
        context: Context,
        navigator: Navigator?,
        cameraPositionState: CameraPositionState,
        permissionState: MultiplePermissionsState,
        fusedLocationClient: FusedLocationProviderClient,
        onBookingDetailsUpdate: (TransportItemDetails) -> Unit,
        hasAnimatedToInitialLocation: Boolean
    ) {
        Log.d(TAG, "Initializing screen")

        // Check booking details first
        val details = bookingDetailsViewModel.getSubmittedDetails()
        if (details == null) {
            Toast.makeText(context, "Please complete booking details first", Toast.LENGTH_LONG).show()
            navigator?.pop()
            return
        }
        onBookingDetailsUpdate(details)

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
                else -> animateCamera(DEFAULT_LOCATION, cameraPositionState)
            }
        }
    }



    // Add these functions inside the BookingScreen class
    companion object {
        private const val TAG = "BookingScreen"
        private val DEFAULT_LOCATION = LatLng(20.5937, 78.9629)

        private suspend fun handleMapClick(
            latLng: LatLng,
            context: Context,
            locationViewModel: LocationSelectionViewModel,
            navigator: Navigator?,
            locationType: String?
        ) {
            try {
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
            } catch (e: Exception) {
                Log.e(TAG, "Error handling map click", e)
                Toast.makeText(
                    context,
                    "Unable to get address for selected location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        private suspend fun handleCurrentLocationClick(
            context: Context,
            fusedLocationClient: FusedLocationProviderClient,
            cameraPositionState: CameraPositionState
        ) {
            try {
                val location = fusedLocationClient.lastLocation.await()
                location?.let {
                    val cameraPosition = CameraPosition.Builder()
                        .target(LatLng(it.latitude, it.longitude))
                        .zoom(15f)
                        .build()
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newCameraPosition(cameraPosition),
                        durationMs = 500
                    )
                } ?: run {
                    Toast.makeText(
                        context,
                        "Unable to get current location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting current location", e)
                Toast.makeText(
                    context,
                    "Unable to get current location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

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

@OptIn(ExperimentalPermissionsApi::class)
private suspend fun handleLocationPermission(
    permissionState: MultiplePermissionsState,
    locationViewModel: LocationSelectionViewModel,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context,
    locationType: String?,
    scope: CoroutineScope
) {
    if (!permissionState.allPermissionsGranted) {
        Log.d(TAG, "Location permission not granted")
        return
    }

    if (!locationViewModel.isUsingCurrentLocation ||
        locationViewModel.pickupLocation != null ||
        locationType?.lowercase() == "drop") {
        return
    }

    try {
        val location = fusedLocationClient.lastLocation.await() ?: run {
            Log.d(TAG, "No last location available")
            return
        }

        val latLng = LatLng(location.latitude, location.longitude)
        scope.launch {
            try {
                val address = getAddressFromLocation(context, latLng)
                locationViewModel.updateCurrentLocation(latLng, address)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting address", e)
                Toast.makeText(context, "Unable to get address", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting current location", e)
        Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun updateRouteAndFares(
    pickupLocation: LatLng?,
    dropOffLocation: LatLng?,
    bookingDetails: TransportItemDetails?,
    onRouteUpdate: (List<LatLng>, Double, String) -> Unit,
    onFareUpdate: (Double, Double) -> Unit,
    cameraPositionState: CameraPositionState
) {
    if (pickupLocation != null && dropOffLocation != null && bookingDetails != null) {
        try {
            // 1. Calculate route and distance
            val routePoints = getRoutePoints(pickupLocation, dropOffLocation)
            val distance = getDrivingDistance(pickupLocation, dropOffLocation) ?: return
            val travelTime = getTravelTime(pickupLocation, dropOffLocation) ?: return

            // 2. Update route information
            onRouteUpdate(routePoints, distance, travelTime)

            // 3. Update camera to show route
            val bounds = LatLngBounds.builder().apply {
                routePoints.forEach { include(it) }
            }.build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 100)
            )

            // 4. Calculate fares for different truck types
            val smallTruckFare = calculateFare(
                start = pickupLocation,
                end = dropOffLocation,
                bookingDetails = bookingDetails.copy(truckType = TruckType.LORRY)
            )
            val largeTruckFare = calculateFare(
                start = pickupLocation,
                end = dropOffLocation,
                bookingDetails = bookingDetails.copy(truckType = TruckType.SIXTEEN_WHEELER)
            )

            // 5. Update fare information
            onFareUpdate(smallTruckFare, largeTruckFare)

            Log.d(TAG, "Route and fares updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating route and fares", e)
            // Optionally handle error (show toast, etc.)
        }
    }
}

