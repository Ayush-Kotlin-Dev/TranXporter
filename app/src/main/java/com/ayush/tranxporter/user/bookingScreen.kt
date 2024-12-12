package com.ayush.tranxporter.user

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
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
import androidx.navigation.NavHostController
import com.ayush.tranxporter.R
import com.ayush.tranxporter.core.presentation.util.PermissionUtils.getAddressFromLocation
import com.ayush.tranxporter.user.presentation.location.LocationSelectionViewModel
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun BookingScreen(
    navController: NavHostController,
    initialPickup: LatLng? = null,
    initialDropoff: LatLng? = null,
    locationType: String? = null,
    viewModel: LocationSelectionViewModel
) {
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


    var pickupLocation by remember(viewModel.pickupLocation) {
        mutableStateOf(viewModel.pickupLocation?.latLng)
    }
    var dropOffLocation by remember(viewModel.dropLocation) {
        mutableStateOf(viewModel.dropLocation?.latLng)
    }
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    var travelTime by remember { mutableStateOf<String?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    val scope = rememberCoroutineScope()
// Initialize route if both locations are provided
    LaunchedEffect(Unit) {
        when (locationType?.lowercase()) {
            "pickup" -> {
                // Do nothing, wait for map selection
            }
            "drop" -> {
                // Do nothing, wait for map selection
            }
            else -> {
                // Only initialize if we're coming from deep link or direct navigation
                if (initialPickup != null && initialDropoff != null) {
                    try {
                        val points = getRoutePoints(initialPickup, initialDropoff)
                        routePoints = points
                        travelTime = getTravelTime(initialPickup, initialDropoff)

                        val bounds = LatLngBounds.builder()
                            .include(initialPickup)
                            .include(initialDropoff)
                            .build()
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(bounds, 100)
                        )
                    } catch (e: Exception) {
                        Log.e("Route", "Failed to initialize route", e)
                    }
                }
            }
        }
    }

    var drivingDistance by remember { mutableStateOf<Double?>(null) }

// Add this effect to fetch route points when locations change
    var smallTruckFare by remember { mutableStateOf<Double?>(null) }
    var largeTruckFare by remember { mutableStateOf<Double?>(null) }

// Update the LaunchedEffect that handles distance and fare calculations
    LaunchedEffect(pickupLocation, dropOffLocation) {
        if (pickupLocation != null && dropOffLocation != null) {
            try {
                // Reset values while calculating
                drivingDistance = null
                smallTruckFare = null
                largeTruckFare = null

                // Get route points and distance
                val points = getRoutePoints(pickupLocation!!, dropOffLocation!!)
                routePoints = points

                // Get driving distance first
                drivingDistance = getDrivingDistance(pickupLocation!!, dropOffLocation!!)

                // Only calculate fares if we have a valid distance
                if (drivingDistance != null) {
                    // Calculate fares
                    smallTruckFare = calculateFare(
                        pickupLocation!!,
                        dropOffLocation!!,
                        VehicleType.SMALL_TRUCK
                    )
                    largeTruckFare = calculateFare(
                        pickupLocation!!,
                        dropOffLocation!!,
                        VehicleType.LARGE_TRUCK
                    )
                }
            } catch (e: Exception) {
                Log.e("Route", "Failed to get route or calculate fare", e)
                // Handle error state here if needed
            }
        }
    }
    
    

    LaunchedEffect(pickupLocation, dropOffLocation) {
        if (pickupLocation != null && dropOffLocation != null) {
            travelTime = getTravelTime(pickupLocation!!, dropOffLocation!!)
        }
    }
    // Update the current location effect
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted &&
            viewModel.isUsingCurrentLocation &&
            viewModel.pickupLocation == null &&
            locationType?.lowercase() != "drop") {  // Don't update if selecting drop location
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                    coroutineScope.launch {
                        val address = getAddressFromLocation(context, currentLocation!!)
                        viewModel.updateCurrentLocation(currentLocation!!, address)
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f)
                        )
                    }
                }
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
                        onClick = { navController.navigateUp() },
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
                                    viewModel.setPickupLocation(latLng, address)
                                    navController.navigateUp()
                                }
                                "drop" -> {
                                    viewModel.setDropLocation(latLng, address)
                                    navController.navigateUp()
                                }
                                else -> {
                                    // Normal booking flow
                                    when {
                                        viewModel.pickupLocation == null -> {
                                            viewModel.setPickupLocation(latLng, address)
                                        }
                                        viewModel.dropLocation == null -> {
                                            viewModel.setDropLocation(latLng, address)
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
                                viewModel.setPickupLocation(null, "") // Add this method to ViewModel
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
                                viewModel.setDropLocation(null, "") // Add this method to ViewModel
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
                        .padding(end = 16.dp, bottom = 96.dp),  // Adjust bottom padding based on your UI
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(380.dp)  // Increased height to accommodate all elements
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)  // Increased spacing between elements
                        ) {
                            // Distance Information
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Distance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = when {
                                        drivingDistance != null -> "%.1f km".format(drivingDistance)
                                        pickupLocation != null && dropOffLocation != null -> "Calculating..."
                                        else -> "-"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )

                            // Vehicle Options
                            VehicleOption(
                                vehicle = "Small Truck",
                                icon = R.drawable.pickup_truck,
                                time = travelTime ?: "Calculating...",
                                price = smallTruckFare?.let { "₹${it.toInt()}" } ?: "Calculating...",
                                isSelected = true
                            )

                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )

                            VehicleOption(
                                vehicle = "Large Truck",
                                icon = R.drawable.truck,
                                time = travelTime ?: "Calculating...",
                                price = largeTruckFare?.let { "₹${it.toInt()}" } ?: "Calculating...",
                                isSelected = false
                            )

                            // Payment and Offers Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PaymentOption(
                                    text = "Cash",
                                    icon = Icons.Default.Star,
                                    modifier = Modifier.weight(1f)
                                )
                                PaymentOption(
                                    text = "Offers",
                                    icon = Icons.Default.Notifications,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))  // Push button to bottom

                            // Book Button - Removed Clear button to match reference
                            Button(
                                onClick = { /* Handle booking */ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),  // Slightly reduced height
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(
                                    "Book Now",
                                    color = Color.Black,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun VehicleOption(
    vehicle: String,
    @DrawableRes icon: Int,
    time: String,
    price: String,
    isSelected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { /* Handle selection */ }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left section with icon and details
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Vehicle Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .padding(8.dp)
            ) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Vehicle details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = vehicle,
                        style = MaterialTheme.typography.bodyMedium,  // Reduced from bodyLarge
                        fontWeight = FontWeight.SemiBold,  // Changed from Bold
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)  // Take only needed space
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)  // Reduced from 12.dp
                        ) {
                            Text(
                                "FASTEST",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),  // Reduced padding
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,  // Explicitly set smaller font size
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
                Text(
                    text = buildString {
                        append(time)
                        if (time != "Calculating...") {
                            append(" • Drop ")
                            append(
                                LocalTime.now()
                                    .plusMinutes((time.split(" ")[0].toIntOrNull() ?: 0).toLong())
                                    .format(DateTimeFormatter.ofPattern("hh:mm a"))
                            )
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // Right section with price and arrow
        Row(
            modifier = Modifier.widthIn(min = 80.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
// Updated PaymentOption composable
@Composable
private fun PaymentOption(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable { /* Handle click */ }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(
                id = if (text == "Cash") R.drawable.cash else R.drawable.offers
            ),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
enum class VehicleType {
    SMALL_TRUCK,
    LARGE_TRUCK
}
