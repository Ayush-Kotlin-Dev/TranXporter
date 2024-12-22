package com.ayush.tranxporter.user

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.tranxporter.R
import com.ayush.tranxporter.core.presentation.util.PermissionUtils
import com.ayush.tranxporter.core.presentation.util.PermissionUtils.getAddressFromLocation
import com.ayush.tranxporter.user.presentation.location.BookingScreen
import com.ayush.tranxporter.user.presentation.location.LocationSelectionViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await
import org.koin.androidx.compose.koinViewModel

enum class TextFieldType {
    PICKUP, DROP, NONE
}

data class LocationScreenState(
    val searchQuery: String = "",
    val pickupSearchQuery: String = "",
    val predictions: List<PlacePrediction> = emptyList(),
    val isSearching: Boolean = false,
    val currentLocation: LatLng? = null,
    val currentAddress: String = "Location permission required",
    val hasLocationPermission: Boolean = false,
    val showPermissionDialog: Boolean = false,
    val activeTextField: TextFieldType = TextFieldType.NONE,
    val isPickupFieldActive: Boolean = false,
    val hasInitializedLocation: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
class LocationSelectionScreen: Screen{
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<LocationSelectionViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        
         val initialState = LocationScreenState(
            hasLocationPermission = PermissionUtils.checkLocationPermission(context)
        )

        var state by remember { mutableStateOf(initialState) }
        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            state = state.copy(
                hasLocationPermission = isGranted,
                currentAddress = if (isGranted) "Fetching location..." else "Location permission required"
            )
        }

        if (state.showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { state = state.copy(showPermissionDialog = false) },
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
                            state = state.copy(showPermissionDialog = false)
                            locationPermissionLauncher.launch(PermissionUtils.LOCATION_PERMISSION)
                        }
                    ) {
                        Text("Grant Permission")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { state = state.copy(showPermissionDialog = false) }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        LaunchedEffect(state.hasLocationPermission, Unit) {
            if (state.hasLocationPermission) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        val address = getAddressFromLocation(context, latLng)
                        state = state.copy(
                            currentLocation = latLng,
                            currentAddress = address,
                            hasInitializedLocation = true
                        )
                        if (viewModel.pickupLocation == null) {
                            viewModel.setPickupLocation(latLng, address)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Location", "Error getting location", e)
                    state = state.copy(currentAddress = "Unable to get location")
                }
            }
        }

        val scope = rememberCoroutineScope()
        LaunchedEffect(state.searchQuery, state.pickupSearchQuery, state.activeTextField) {
            val queryToUse = when (state.activeTextField) {
                TextFieldType.PICKUP -> state.pickupSearchQuery
                TextFieldType.DROP -> state.searchQuery
                TextFieldType.NONE -> return@LaunchedEffect
            }
            
            if (queryToUse.length >= 2) {
                state = state.copy(isSearching = true)
                searchPlaces(queryToUse, context, scope) {
                    state = state.copy(predictions = it, isSearching = false)
                }
            } else {
                state = state.copy(predictions = emptyList(), isSearching = false)
            }
        }

        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(state.activeTextField) {
            keyboardController?.let { keyboard ->
                when (state.activeTextField) {
                    TextFieldType.PICKUP, TextFieldType.DROP -> keyboard.show()
                    TextFieldType.NONE -> keyboard.hide()
                }
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
                            onClick = { }
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
                        val pickupInteractionSource = remember { MutableInteractionSource() }

                        TextField(
                            value = when {
                                state.activeTextField == TextFieldType.PICKUP -> state.pickupSearchQuery
                                viewModel.pickupLocation != null -> viewModel.pickupLocation?.address ?: ""
                                else -> state.currentAddress
                            },
                            onValueChange = { 
                                state = state.copy(
                                    pickupSearchQuery = it,
                                    activeTextField = TextFieldType.PICKUP
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        state = state.copy(
                                            activeTextField = TextFieldType.PICKUP,
                                            pickupSearchQuery = "" // Clear the search query when focused
                                        )
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

                        TextField(
                            value = when {
                                state.activeTextField == TextFieldType.DROP -> state.searchQuery
                                viewModel.dropLocation != null -> viewModel.dropLocation?.address ?: ""
                                else -> ""
                            },
                            onValueChange = { 
                                state = state.copy(
                                    searchQuery = it,
                                    activeTextField = TextFieldType.DROP
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        state = state.copy(
                                            activeTextField = TextFieldType.DROP,
                                            searchQuery = ""
                                        )
                                    }
                                },
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
                                        .background(
                                            color = Color(0xFFE53935).copy(alpha = 0.1f),
                                            shape = CircleShape
                                        ),
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

                if (state.isSearching) {
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

                OutlinedButton(
                    onClick = {
                        navigator.push(BookingScreen(
                            initialPickup = viewModel.pickupLocation?.latLng,
                            initialDropoff = viewModel.dropLocation?.latLng,
                        ))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    enabled = viewModel.pickupLocation != null
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select on map")
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.predictions) { prediction ->
                        LocationSuggestionItem(
                            prediction = prediction,
                            onItemClick = {
                                val latLng = LatLng(prediction.latitude, prediction.longitude)
                                if (state.activeTextField == TextFieldType.PICKUP) {
                                    viewModel.setPickupLocation(latLng, prediction.mainText)
                                } else {
                                    viewModel.setDropLocation(latLng, prediction.mainText)
                                    if (viewModel.pickupLocation != null) {
                                        navigator.push(BookingScreen(
                                            initialPickup = viewModel.pickupLocation?.latLng,
                                            initialDropoff = viewModel.dropLocation?.latLng,
                                        ))
                                    }
                                }
                            }
                        )
                    }
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