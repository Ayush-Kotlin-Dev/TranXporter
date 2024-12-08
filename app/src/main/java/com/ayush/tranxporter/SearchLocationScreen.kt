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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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
            searchPlaces(debouncedSearchQuery, context, scope) { results ->
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
                        searchPlaces(newQuery, context, scope) { results ->
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
    navController: NavHostController
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    "Where are you going?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                        .padding(TextFieldDefaults.contentPaddingWithoutLabel())
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


// Data classes remain the same as in the original implementation
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
    val longitude: Double = 0.0,
    val relevanceScore: Double = 0.0
)

data class PlaceDetails(
    val latitude: Double,
    val longitude: Double,
    val address: String
)

suspend fun searchPlaces(
    query: String,
    context: Context,
    scope: CoroutineScope,
    onResult: (List<PlacePrediction>) -> Unit
) {
    // Early return for very short queries
    if (query.length < 2) {
        onResult(emptyList())
        return
    }

    withContext(Dispatchers.IO) {
        try {
            // Optimize location retrieval - use a faster, more concise approach
            val currentLocation = getCurrentLocation(context)

            val placesClient = Places.createClient(context)
            val token = AutocompleteSessionToken.newInstance()

            // Build request with optimized parameters
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setCountries("IN")
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(token)
                .apply {
                    currentLocation?.let { location ->
                        // Use a more conservative location bias
                        setLocationBias(
                            RectangularBounds.newInstance(
                                LatLng(
                                    location.latitude - 0.2,
                                    location.longitude - 0.2
                                ),
                                LatLng(
                                    location.latitude + 0.2,
                                    location.longitude + 0.2
                                )
                            )
                        )
                    }
                }
                .build()

            // Use async for concurrent place details fetching
            val predictions = placesClient.findAutocompletePredictions(request)
                .await()
                .autocompletePredictions

            // Parallel place details retrieval with limited concurrency
            val placeResults = predictions.take(5).map { prediction ->
                scope.async {
                    try {
                        val placeDetails = getPlaceDetails(prediction.placeId, context)

                        // Calculate relevance score
                        val relevanceScore = calculateRelevanceScore(
                            prediction,
                            query,
                            currentLocation
                        )

                        PlacePrediction(
                            placeId = prediction.placeId,
                            description = prediction.getFullText(null).toString(),
                            mainText = prediction.getPrimaryText(null).toString(),
                            secondaryText = prediction.getSecondaryText(null).toString(),
                            latitude = placeDetails?.latitude ?: 0.0,
                            longitude = placeDetails?.longitude ?: 0.0,
                            relevanceScore = relevanceScore
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }.mapNotNull { it.await() }

            // Sort results by relevance score
            val sortedResults = placeResults
                .sortedByDescending { it.relevanceScore }
                .take(5)  // Limit to top 5 results

            onResult(sortedResults)
        } catch (e: Exception) {
            Log.e("Places", "Error fetching predictions", e)
            onResult(emptyList())
        }
    }
}

// Optimized location retrieval
private suspend fun getCurrentLocation(context: Context): LatLng? {
    return try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Use a single checkpoint for location permission
        if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            // Use withContext to handle async location retrieval
            withContext(Dispatchers.IO) {
                val location = fusedLocationClient.lastLocation.await()
                location?.let { LatLng(it.latitude, it.longitude) }
            }
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("Location", "Error retrieving location", e)
        null
    }
}

// More sophisticated relevance scoring
private fun calculateRelevanceScore(
    prediction: AutocompletePrediction,
    query: String,
    currentLocation: LatLng?
): Double {
    var score = 0.0

    // Text relevance
    if (prediction.getPrimaryText(null).toString().contains(query, ignoreCase = true)) {
        score += 10.0  // Strong match in primary text
    }

    // Location relevance
    currentLocation?.let { userLocation ->
        if (prediction.placeId.isNotEmpty()) {
            // Penalty for predictions without place details
            score += 5.0
        }

        // Distance impact (closer locations get higher score)
        score += calculateDistanceScore(userLocation, prediction)
    }

    // Additional relevance boosters
    val boostKeywords = listOf(
        "hospital", "station", "airport", "market",
        "mall", "school", "college", "landmark",
        "monument", "park", "plaza", "business"
    )

    boostKeywords.forEach { keyword ->
        if (prediction.getPrimaryText(null).toString().contains(keyword, ignoreCase = true)) {
            score += 2.0
        }
    }

    return score
}

// Distance-based scoring
private fun calculateDistanceScore(
    userLocation: LatLng,
    prediction: AutocompletePrediction
): Double {
    // Placeholder for distance calculation - implement actual distance retrieval
    return try {
        val distance = 10.0 // Placeholder - replace with actual distance calculation
        when {
            distance < 1.0 -> 5.0   // Very close
            distance < 5.0 -> 3.0   // Nearby
            distance < 10.0 -> 1.0  // Within reasonable range
            else -> 0.0              // Far
        }
    } catch (e: Exception) {
        0.0
    }
}

// Existing place details retrieval function remains mostly the same
suspend fun getPlaceDetails(placeId: String, context: Context): PlaceDetails? {
    return withContext(Dispatchers.IO) {
        try {
            val placesClient = Places.createClient(context)

            val placeFields = listOf(
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
            )

            val request = FetchPlaceRequest.builder(placeId, placeFields).build()

            val response = placesClient.fetchPlace(request).await()
            val place = response.place

            PlaceDetails(
                latitude = place.latLng?.latitude ?: 0.0,
                longitude = place.latLng?.longitude ?: 0.0,
                address = place.address ?: ""
            )
        } catch (e: Exception) {
            Log.e("Places", "Error fetching place details", e)
            null
        }
    }
}