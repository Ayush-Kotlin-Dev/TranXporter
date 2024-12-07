package com.ayush.tranxporter

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchLocationScreen(navController: NavHostController) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val recentLocations = listOf(
        SavedLocation("Home", "123, TranXporter St"),
        SavedLocation("Work", "456, TranXporter St")
    )
    val scope = rememberCoroutineScope()

    // Debounced search
    val debouncedSearchQuery by rememberUpdatedState(searchQuery)
    LaunchedEffect(debouncedSearchQuery) {
        if (debouncedSearchQuery.isNotEmpty()) {
            delay(300) // Debounce delay
            isSearching = true
            searchPlaces(debouncedSearchQuery, context,scope) { results ->
                predictions = results
                isSearching = false
            }
        } else {
            predictions = emptyList()
        }
    }

    Scaffold(
        topBar = {
            SearchBar(
                query = searchQuery,
                onQueryChange = { newQuery ->
                    searchQuery = newQuery
                    // Call Places API for predictions
                    scope.launch {
                        searchPlaces(newQuery, context,scope) { results ->
                            predictions = results
                        }
                    }
                },
                onNavigateBack = { navController.navigateUp() },
                navController = navController
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (searchQuery.isEmpty()) {
                // Show recent locations
                items(recentLocations) { location ->
                    RecentLocationItem(
                        location = location,
                        onLocationClick = {
                            navController.navigate("booking?location=${location.title}")
                        }
                    )
                }
            } else {
                // Show loading indicator
                if (isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // Show predictions
                items(predictions) { prediction ->
                    PredictionItem(
                        prediction = prediction,
                        onPredictionClick = {
                            // Get place details before navigating
                            scope.launch {
                                val placeDetails = getPlaceDetails(prediction.placeId, context)
                                navController.navigate(
                                    "booking?location=${prediction.description}" +
                                            "&lat=${placeDetails?.latitude ?: 0.0}" +
                                            "&lng=${placeDetails?.longitude ?: 0.0}"
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    navController: NavHostController  // Add navController parameter
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                // Navigate to LocationSelectionScreen when clicked
                navController.navigate("locationSelection")
            },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }

            // Make TextField not focusable since we want to navigate on click
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    "Where are you going?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun RecentLocationItem(
    location: SavedLocation,
    onLocationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLocationClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = location.title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = location.subtitle,
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

@Composable
private fun PredictionItem(
    prediction: PlacePrediction,
    onPredictionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPredictionClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
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

// Data classes
data class SavedLocation(
    val title: String,
    val subtitle: String
)

data class PlacePrediction(
    val placeId: String,
    val description: String,
    val mainText: String,
    val secondaryText: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
// Add function to get place details
data class PlaceDetails(
    val latitude: Double,
    val longitude: Double,
    val address: String
)

suspend fun getPlaceDetails(placeId: String, context: Context): PlaceDetails? {
    return withContext(Dispatchers.IO) {
        try {
            val placesClient = Places.createClient(context)

            // Specify the fields you want to request
            val placeFields = listOf(
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
            )

            val request = FetchPlaceRequest.builder(placeId, placeFields).build()

            suspendCancellableCoroutine { continuation ->
                placesClient.fetchPlace(request)
                    .addOnSuccessListener { response ->
                        val place = response.place
                        val details = PlaceDetails(
                            latitude = place.latLng?.latitude ?: 0.0,
                            longitude = place.latLng?.longitude ?: 0.0,
                            address = place.address ?: ""
                        )
                        continuation.resume(details) {}
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Places", "Error fetching place details", exception)
                        continuation.resume(null) {}
                    }
            }
        } catch (e: Exception) {
            Log.e("Places", "Error fetching place details", e)
            null
        }
    }
}

suspend fun searchPlaces(
    query: String,
    context: Context,
    scope: CoroutineScope,
    onResult: (List<PlacePrediction>) -> Unit
) {
    if (query.length < 2) {
        onResult(emptyList())
        return
    }


    withContext(Dispatchers.IO) {
        try {
            val placesClient = Places.createClient(context)
            val token = AutocompleteSessionToken.newInstance()

            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setCountries("IN")
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(token)
                .build()

            val predictions = suspendCancellableCoroutine { continuation ->
                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        scope.launch {
                            val results = response.autocompletePredictions.map { prediction ->
                                // Get coordinates for each prediction
                                val placeDetails = getPlaceDetails(prediction.placeId, context)

                                PlacePrediction(
                                    placeId = prediction.placeId,
                                    description = prediction.getFullText(null).toString(),
                                    mainText = prediction.getPrimaryText(null).toString(),
                                    secondaryText = prediction.getSecondaryText(null).toString(),
                                    latitude = placeDetails?.latitude ?: 0.0,
                                    longitude = placeDetails?.longitude ?: 0.0
                                )
                            }
                            continuation.resume(results) {}
                        }
                    }
                    .addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }

            onResult(predictions)
        } catch (e: Exception) {
            Log.e("Places", "Error fetching predictions", e)
            onResult(emptyList())
        }
    }
}

