package com.ayush.tranxporter.user
import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await
import android.util.Log
import androidx.compose.ui.text.style.TextOverflow
import com.ayush.tranxporter.PlacePrediction
import com.ayush.tranxporter.searchPlaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionScreen(navController: NavHostController) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var currentAddress by remember { mutableStateOf<String>("Fetching location...") }

    // Get current location and address immediately
    LaunchedEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            val permissionCheck = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val location = fusedLocationClient.lastLocation.await()
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    currentLocation = latLng
                    // Get address
                    currentAddress = getAddressFromLocation(context, latLng)
                }
            }
        } catch (e: Exception) {
            Log.e("Location", "Error getting location", e)
            currentAddress = "Unable to get location"
        }
    }
    val scope = rememberCoroutineScope()
    // Search predictions
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isSearching = true
            val results = searchPlaces(searchQuery, context, scope ){
                predictions = it
            }
            isSearching = false
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Select Drop Location") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Current Location Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Show current location as pickup with address
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Green
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Current Location (Pickup)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                currentAddress,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Dropoff location search
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter drop location") },
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        }
                    )
                }
            }

            // Show predictions
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
            }

            LazyColumn {
                // Update LocationSelectionScreen navigation
                items(predictions) { prediction ->
                    LocationSuggestionItem(
                        prediction = prediction,
                        onItemClick = {
                            // Navigate to booking with both locations
                            currentLocation?.let { pickup ->
                                Log.d("Navigation", "Navigating with coordinates: " +
                                        "pickup: (${pickup.latitude}, ${pickup.longitude}), " +
                                        "drop: (${prediction.latitude}, ${prediction.longitude})")

                                navController.navigate(
                                    "booking?" +
                                            "pickup_lat=${pickup.latitude}&" +
                                            "pickup_lng=${pickup.longitude}&" +
                                            "drop_lat=${prediction.latitude}&" +
                                            "drop_lng=${prediction.longitude}"
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationSuggestionItem(
    prediction: PlacePrediction,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = prediction.mainText,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = prediction.secondaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Add this function
private suspend fun getAddressFromLocation(context: Context, latLng: LatLng): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0]?.let { address ->
                    buildString {
                        // Add the most detailed part of the address first
                        address.getAddressLine(0)?.let { append(it) }
                    }
                } ?: "Current Location"
            } else {
                "Current Location"
            }
        } catch (e: Exception) {
            Log.e("Geocoder", "Error getting address", e)
            "Current Location"
        }
    }
}