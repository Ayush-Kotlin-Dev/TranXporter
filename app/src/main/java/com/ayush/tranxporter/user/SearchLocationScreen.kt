package com.ayush.tranxporter.user

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
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ayush.tranxporter.user.presentation.bookingdetails.BookingDetailsViewModel
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
class SearchLocationScreen : Screen{


    @Composable
    override fun Content() {
        val viewModel: BookingDetailsViewModel = koinViewModel()
        val state = viewModel.state
        val context = LocalContext.current
        var searchQuery by remember { mutableStateOf("") }
        var predictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
        var isSearching by remember { mutableStateOf(false) }
        val recentLocations = listOf(
            SavedLocation("Home", "123, TranXporter St"),
            SavedLocation("Work", "456, TranXporter St")
        )
        val navigator = LocalNavigator.current

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
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { newQuery ->
                        searchQuery = newQuery
                        scope.launch {
                            searchPlaces(newQuery, context, scope) { results ->
                                predictions = results
                            }
                        }
                    },
                    onNavigateBack = { navigator?.pop() },
                )
            }
        ) { padding ->
            state.submittedDetails?.let { details ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Transport Details:",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = buildString {
                                    append("Vehicle Type: ${details.truckType}\n")
                                    append("Category: ${details.category}\n")
                                    append("Weight: ${details.weight} kg\n")
                                    append("Dimensions: ${details.dimensions}\n")
                                    if (details.specialHandling) {
                                        append("Special Handling Required")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                    if (searchQuery.isEmpty()) {
                        // Show recent locations
                        items(recentLocations) { location ->
                            RecentLocationItem(
                                location = location,
                                onLocationClick = {

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
                                    scope.launch {
                                        val placeDetails = getPlaceDetails(prediction.placeId, context)
                                    }
                                }
                            )
                        }
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transport details available")
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
) {
    val navigator = LocalNavigator.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                // Navigate to LocationSelectionScreen when clicked
                navigator?.push(LocationSelectionScreen())
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

// The PlacePrediction data class remains the same

data class PlacePrediction(
    val placeId: String,
    val description: String,
    val mainText: String,
    val secondaryText: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val relevanceScore: Double = 0.0,
    val distanceInKm: Double = 0.0,
    val types: List<String> = emptyList(),
    val structuredFormatting: Map<String, String> = emptyMap()
)

data class SavedLocation(
    val title: String,
    val subtitle: String
)

data class PlaceDetails(
    val latitude: Double,
    val longitude: Double,
    val address: String
)

// These functions remain the same as in the original implementation
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
            val currentLocation = getCurrentLocation(context)
            val placesClient = Places.createClient(context)
            val token = AutocompleteSessionToken.newInstance()

            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setCountries("IN") // India
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setSessionToken(token)
                .apply {
                    currentLocation?.let { location ->
                        // Improved location bias with dynamic radius based on urban/rural area
                        val bounds = calculateSearchBounds(location, isDenseUrbanArea(context))
                        setLocationBias(bounds)
                    }
                }
                .build()

            val predictions = placesClient.findAutocompletePredictions(request)
                .await()
                .autocompletePredictions

            // Then update the relevance calculation in the searchPlaces function
            val placeResults = predictions.take(5).map { prediction ->
                scope.async {
                    try {
                        val placeDetails = getPlaceDetails(prediction.placeId, context)
                        val distance = currentLocation?.let { location ->
                            calculateHaversineDistance(
                                location.latitude,
                                location.longitude,
                                placeDetails?.latitude ?: 0.0,
                                placeDetails?.longitude ?: 0.0
                            )
                        } ?: 0.0

                        // Convert Place.Type to String list
                        val placeTypeStrings = prediction.placeTypes?.map { it.name } ?: emptyList()

                        // Enhanced relevance scoring
                        val relevanceScore = calculateRelevanceScore(
                            prediction = prediction,
                            query = query,
                            currentLocation = currentLocation,
                            distance = distance,
                            placeTypes = placeTypeStrings
                        )

                        PlacePrediction(
                            placeId = prediction.placeId,
                            description = prediction.getFullText(null).toString(),
                            mainText = prediction.getPrimaryText(null).toString(),
                            secondaryText = prediction.getSecondaryText(null).toString(),
                            latitude = placeDetails?.latitude ?: 0.0,
                            longitude = placeDetails?.longitude ?: 0.0,
                            relevanceScore = relevanceScore,
                            distanceInKm = distance,
                            types = placeTypeStrings,
                            structuredFormatting = mapOf(
                                "main_text" to prediction.getPrimaryText(null).toString(),
                                "secondary_text" to prediction.getSecondaryText(null).toString()
                            )
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }.mapNotNull { it.await() }

            val sortedResults = placeResults
                .sortedWith(
                    compareByDescending<PlacePrediction> { it.relevanceScore }
                        .thenBy { it.distanceInKm }
                        .thenBy { it.mainText.length } // Prefer shorter, more precise names
                )
                .take(5)

            onResult(sortedResults)
        } catch (e: Exception) {
            Log.e("Places", "Error fetching predictions", e)
            onResult(emptyList())
        }
    }
}

// Helper function to determine if the area is densely urban
private fun isDenseUrbanArea(context: Context): Boolean {
    // You can implement this based on population density data or
    // number of places in the area. For now, using a simple implementation:
    return true // Assuming urban area for better results
}

// Calculate dynamic search bounds based on area type
private fun calculateSearchBounds(
    center: LatLng,
    isDenseUrban: Boolean
): RectangularBounds {
    val radius = if (isDenseUrban) 0.1 else 0.3 // Smaller radius for urban areas
    return RectangularBounds.newInstance(
        LatLng(
            center.latitude - radius,
            center.longitude - radius
        ),
        LatLng(
            center.latitude + radius,
            center.longitude + radius
        )
    )
}

// Improved relevance scoring
private fun calculateRelevanceScore(
    prediction: AutocompletePrediction,
    query: String,
    currentLocation: LatLng?,
    distance: Double,
    placeTypes: List<String>
): Double {
    var score = 0.0

    // Text matching score (0-40 points)
    val queryLower = query.lowercase()
    val mainTextLower = prediction.getPrimaryText(null).toString().lowercase()
    val secondaryTextLower = prediction.getSecondaryText(null).toString().lowercase()

    when {
        mainTextLower == queryLower -> score += 40.0 // Exact match
        mainTextLower.startsWith(queryLower) -> score += 35.0 // Starts with query
        mainTextLower.contains(queryLower) -> score += 30.0 // Contains query
        secondaryTextLower.contains(queryLower) -> score += 25.0 // Match in secondary text
    }

    // Distance score (0-30 points)
    score += when {
        distance < 1.0 -> 30.0
        distance < 2.0 -> 25.0
        distance < 5.0 -> 20.0
        distance < 10.0 -> 15.0
        distance < 20.0 -> 10.0
        else -> 5.0
    }

    // Place type relevance (0-20 points)
    val importantTypes = mapOf(
        "point_of_interest" to 20.0,
        "establishment" to 18.0,
        "route" to 15.0,
        "street_address" to 15.0,
        "sublocality" to 12.0,
        "neighborhood" to 10.0
    )

    placeTypes.forEach { type ->
        importantTypes[type]?.let { typeScore ->
            score += typeScore
        }
    }

    // Popularity indicators (0-10 points)
    if (prediction.getDistanceMeters() != null) {
        score += 10.0 // Place has distance information
    }

    return score
}

// Optimized location retrieval
private suspend fun getCurrentLocation(context: Context): LatLng? {
    return try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Use a single checkpoint for location permission
        if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {

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

private fun calculateHaversineDistance(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val R = 6371.0 // Earth's radius in kilometers

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return R * c // Distance in kilometers
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