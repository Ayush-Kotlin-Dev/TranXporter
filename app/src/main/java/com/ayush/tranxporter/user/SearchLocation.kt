package com.ayush.tranxporter.user

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


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