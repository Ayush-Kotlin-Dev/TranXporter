package com.ayush.tranxporter.user

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.navigation.NavHostController
import com.ayush.tranxporter.R
import com.ayush.tranxporter.ui.theme.BlueLight
import com.ayush.tranxporter.utils.getRoutePoints
import com.ayush.tranxporter.utils.getTravelTime
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun BookingScreen(
    navController: NavHostController,
    initialPickup: LatLng? = null,
    initialDropoff: LatLng? = null
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

    // Initialize with passed locations
    var pickupLocation by remember { mutableStateOf(initialPickup) }
    var dropOffLocation by remember { mutableStateOf(initialDropoff) }


    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    var travelTime by remember { mutableStateOf<String?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
// Initialize route if both locations are provided
    LaunchedEffect(Unit) {
        if (initialPickup != null && initialDropoff != null) {
            pickupLocation = initialPickup
            dropOffLocation = initialDropoff

            try {
                val points = getRoutePoints(initialPickup, initialDropoff)
                routePoints = points
                travelTime = getTravelTime(initialPickup, initialDropoff)

                // Adjust camera to show both locations
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
// Add this effect to fetch route points when locations change
    LaunchedEffect(pickupLocation, dropOffLocation) {
        if (pickupLocation != null && dropOffLocation != null) {
            try {
                val points = getRoutePoints(pickupLocation!!, dropOffLocation!!)
                routePoints = points
            } catch (e: Exception) {
                Log.e("Route", "Failed to get route", e)
            }
        }
    }
    LaunchedEffect(pickupLocation, dropOffLocation) {
        if (pickupLocation != null && dropOffLocation != null) {
            travelTime = getTravelTime(pickupLocation!!, dropOffLocation!!)
        }
    }
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                    coroutineScope.launch {
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
            SmallTopAppBar(
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
        }
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
                        when {
                            pickupLocation == null -> pickupLocation = latLng
                            dropOffLocation == null -> dropOffLocation = latLng
                        }
                    }
                ) {
                    pickupLocation?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Pickup Location",
                            snippet = "Tap to change",
                            onInfoWindowClick = {
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
                            .height(320.dp)  // Adjusted height
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
                                .padding(12.dp)  // Reduced padding
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)  // Reduced spacing
                        ) {
                            // Distance Information
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Distance",
                                    style = MaterialTheme.typography.bodyMedium,  // Smaller text
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "%.1f km".format(
                                        haversineDistance(
                                            pickupLocation!!,
                                            dropOffLocation!!
                                        )
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),  // Reduced padding
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)  // Lighter divider
                            )

                            // Vehicle Options
                            VehicleOption(
                                vehicle = "Small Truck",
                                icon = R.drawable.pickup_truck,
                                time = travelTime ?: "Calculating...",
                                price = "${calculateFare(pickupLocation!!, dropOffLocation!!)}",
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
                                price = "${
                                    (calculateFare(
                                        pickupLocation!!,
                                        dropOffLocation!!
                                    ) * 1.5)
                                }",
                                isSelected = false
                            )

                            // Payment and Offers Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PaymentOption(text = "Cash", icon = Icons.Default.Star)
                                PaymentOption(text = "Offers", icon = Icons.Default.Notifications)
                            }

                            // Book Button - Removed Clear button to match reference
                            Button(
                                onClick = { /* Handle booking */ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),  // Slightly reduced height
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BlueLight
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
            .clickable { /* Handle selection */ }
            .padding(vertical = 8.dp),  // Reduced padding
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)  // Reduced size
                    .padding(end = 8.dp)  // Reduced padding
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {  // Reduced spacing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = vehicle,
                        style = MaterialTheme.typography.bodyLarge,  // Smaller text
                        fontWeight = FontWeight.Bold
                    )
                    if (isSelected) {
                        Surface(
                            color = Color(0xFF4B6EFF),  // Blue color from reference
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "FASTEST",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
                Text(
                    text = "$time • Drop ${
                        LocalTime.now().plusMinutes(
                            (time.split(" ")[0].toIntOrNull() ?: 0).toLong()
                        ).format(DateTimeFormatter.ofPattern("hh:mm a"))
                    }",
                    style = MaterialTheme.typography.bodySmall,  // Smaller text
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "₹${price.toDouble().toInt()}",
            style = MaterialTheme.typography.titleMedium,  // Smaller text
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PaymentOption(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .clickable { /* Handle click */ }
            .padding(vertical = 4.dp, horizontal = 8.dp),  // Reduced padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)  // Reduced spacing
    ) {
        Icon(
            painter = painterResource(
                id = if (text == "Cash") R.drawable.cash else R.drawable.offers
            ),
            contentDescription = null,
            modifier = Modifier.size(20.dp),  // Reduced size
            tint = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium  // Smaller text
        )
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),  // Reduced size
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


fun haversineDistance(start: LatLng, end: LatLng): Double {
    val earthRadius = 6371.0 // kilometers

    val dLat = Math.toRadians(end.latitude - start.latitude)
    val dLon = Math.toRadians(end.longitude - start.longitude)

    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(start.latitude)) *
            cos(Math.toRadians(end.latitude)) *
            sin(dLon / 2).pow(2.0)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c
}

fun calculateFare(start: LatLng, end: LatLng): Double {
    val distance = haversineDistance(start, end)
    val baseFare = 50.0
    val perKmRate = 15.0
    return baseFare + (distance * perKmRate)
}
