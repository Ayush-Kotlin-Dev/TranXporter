package com.ayush.tranxporter.user

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ayush.tranxporter.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.onFocusChanged
import com.ayush.tranxporter.core.presentation.util.PermissionUtils
import com.ayush.tranxporter.user.presentation.location.LocationSelectionViewModel
import org.koin.androidx.compose.koinViewModel
enum class TextFieldType {
    PICKUP, DROP, NONE
}
// LocationSelectionScreen Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionScreen(navController: NavHostController,viewModel: LocationSelectionViewModel) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var currentAddress by remember { mutableStateOf<String>("Location permission required") }
    var hasLocationPermission by remember {
        mutableStateOf(PermissionUtils.checkLocationPermission(context))
    }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var activeTextField by remember { mutableStateOf<TextFieldType>(TextFieldType.NONE) }
    var pickupSearchQuery by remember { mutableStateOf("") }
    var isPickupFieldActive by remember { mutableStateOf(false) }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            currentAddress = "Fetching location..."
        } else {
            currentAddress = "Location permission required"
        }
    }

    // Add this state to track initial location setup
    var hasInitializedLocation by remember { mutableStateOf(false) }

// Modify the LaunchedEffect
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && !hasInitializedLocation && viewModel.pickupLocation == null) {
            PermissionUtils.getCurrentLocation(context)?.let { location ->
                currentLocation = location
                val address = getAddressFromLocation(context, currentLocation!!)
                viewModel.updateCurrentLocation(location, address)
                currentAddress = PermissionUtils.getAddressFromLocation(context, location)
                // If pickup isn't set yet, set it as current location
                if (viewModel.pickupLocation == null) {
                    viewModel.setPickupLocation(location, currentAddress)
                }
                hasInitializedLocation = true
            } ?: run {
                currentAddress = "Unable to get location"
            }
        }
    }

    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Location Permission Required") },
            text = {
                Text(
                    PermissionUtils.requiredPermissions
                    .find { it.permission == PermissionUtils.LOCATION_PERMISSION }
                    ?.description ?: "We need location permission to show nearby places.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        locationPermissionLauncher.launch(PermissionUtils.LOCATION_PERMISSION)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Get current location and address immediately
    LaunchedEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            val permissionCheck = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
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
    LaunchedEffect(searchQuery, pickupSearchQuery) {
        if ((activeTextField == TextFieldType.PICKUP && pickupSearchQuery.length >= 2) ||
            (activeTextField == TextFieldType.DROP && searchQuery.length >= 2)) {
            isSearching = true
            val queryToUse = if (activeTextField == TextFieldType.PICKUP) pickupSearchQuery else searchQuery
            val results = searchPlaces(queryToUse, context, scope) {
                predictions = it
            }
            isSearching = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Select Location",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Location Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Pickup Location Row
                    // Add this at the top with other state declarations
                    val pickupInteractionSource = remember { MutableInteractionSource() }

// Then update the TextField
                    TextField(
                        value = if (isPickupFieldActive) pickupSearchQuery else viewModel.pickupLocation?.address ?: currentAddress,
                        onValueChange = {
                            pickupSearchQuery = it
                            activeTextField = TextFieldType.PICKUP
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    isPickupFieldActive = true
                                    pickupSearchQuery = ""
                                    activeTextField = TextFieldType.PICKUP
                                }
                            },
                        placeholder = {
                            Text(
                                "Pickup location",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        interactionSource = pickupInteractionSource
                    )

                    Divider(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                    )

                    // Drop Location TextField
                    TextField(
                        value = if (activeTextField == TextFieldType.DROP)
                            searchQuery
                        else
                            viewModel.dropLocation?.address ?: "",  // Display the drop location from ViewModel
                        onValueChange = {
                            searchQuery = it
                            activeTextField = TextFieldType.DROP
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        placeholder = {
                            Text(
                                "Where to?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true
                    )
                }
            }

            // Search Progress Indicator
            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }

            // Select on Map Button
            OutlinedButton(
                onClick = {
                    // Navigate to BookingScreen with the appropriate type parameter
                    navController.navigate(
                        "booking?type=${activeTextField.name.lowercase()}" +
                                // If pickup location exists, pass it as parameters
                                (viewModel.pickupLocation?.let { pickup ->
                                    "&pickup_lat=${pickup.latLng.latitude}" +
                                            "&pickup_lng=${pickup.latLng.longitude}"
                                } ?: "") +
                                // If drop location exists, pass it as parameters
                                (viewModel.dropLocation?.let { drop ->
                                    "&drop_lat=${drop.latLng.latitude}" +
                                            "&drop_lng=${drop.latLng.longitude}"
                                } ?: "")
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select on map")
            }

            // Predictions List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(predictions) { prediction ->
                    LocationSuggestionItem(
                        prediction = prediction,
                        onItemClick = {
                            val latLng = LatLng(prediction.latitude, prediction.longitude)
                            if (activeTextField == TextFieldType.PICKUP) {
                                viewModel.setPickupLocation(latLng, prediction.mainText)
                                isPickupFieldActive = false
                                pickupSearchQuery = ""
                            } else {
                                viewModel.setDropLocation(latLng, prediction.mainText)
                                // When navigating to booking screen
                                if (viewModel.pickupLocation != null) {
                                    navController.navigate(
                                        "booking?" +
                                                "pickup_lat=${viewModel.pickupLocation?.latLng?.latitude}&" +
                                                "pickup_lng=${viewModel.pickupLocation?.latLng?.longitude}&" +
                                                "drop_lat=${latLng.latitude}&" +
                                                "drop_lng=${latLng.longitude}"
                                    ) {
                                        // Add this to prevent going back to location selection
                                        popUpTo("location_selection") { inclusive = true }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// LocationSuggestionItem Composable
@Composable
private fun LocationSuggestionItem(
    prediction: PlacePrediction,
    onItemClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.location_pin),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = prediction.mainText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = prediction.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentLocationItem(
    prediction: PlacePrediction,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
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
        IconButton(onClick = { /* Handle favorite */ }) {
            Icon(
                Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Function to get address from location
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