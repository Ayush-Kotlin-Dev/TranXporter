package com.ayush.tranxporter.utils

import android.util.Log
import com.ayush.tranxporter.BuildConfig
import com.ayush.tranxporter.user.presentation.bookingdetails.TransportItemDetails
import com.ayush.tranxporter.user.presentation.bookingdetails.TruckType
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalTime
import kotlin.math.round
import kotlin.math.roundToInt


suspend fun getDrivingDistance(origin: LatLng, destination: LatLng): Double? {
    return withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.MAPS_API_KEY
            val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&mode=driving" +  // Specify driving mode
                    "&alternatives=true" + // Request alternative routes
                    "&optimize=true" + // Request route optimization
                    "&key=$apiKey"

            val response = URL(url).readText()
            val jsonObject = JSONObject(response)

            if (jsonObject.getString("status") == "OK") {
                // Get all available routes
                val routes = jsonObject.getJSONArray("routes")
                var shortestDistance = Double.MAX_VALUE

                // Find the shortest route
                for (i in 0 until routes.length()) {
                    val route = routes.getJSONObject(i)
                        .getJSONArray("legs")
                        .getJSONObject(0)

                    val distance = route.getJSONObject("distance")
                        .getInt("value") / 1000.0  // Convert meters to kilometers

                    if (distance < shortestDistance) {
                        shortestDistance = distance
                    }
                }

                // Round to 1 decimal place for better display
                (shortestDistance * 10).roundToInt() / 10.0
            } else {
                Log.e("Distance", "API Error: ${jsonObject.getString("status")}")
                null
            }
        } catch (e: Exception) {
            Log.e("Distance", "Error calculating distance", e)
            null
        }
    }
}

object PriceMultipliers {
    // Vehicle type multipliers
    val VEHICLE_MULTIPLIERS = mapOf(
        TruckType.PICKUP to 1.0,
        TruckType.LORRY to 1.5,
        TruckType.SIXTEEN_WHEELER to 2.0,
        TruckType.TRACTOR to 1.8
    )

    // Weight-based multipliers (per kg)
    const val WEIGHT_FACTOR = 0.01  // 1 rupee per kg

    // Special handling surcharge
    const val SPECIAL_HANDLING_MULTIPLIER = 1.2

    // Volume-based multiplier (based on dimensions)
    const val VOLUME_FACTOR = 0.001  // 0.1 rupee per cubic cm
}

suspend fun calculateFare(
    start: LatLng,
    end: LatLng,
    bookingDetails: TransportItemDetails,
    timeOfDay: LocalTime = LocalTime.now(),
    baseFare: Double = 50.0
): Double {
    val distance = getDrivingDistance(start, end) ?: return baseFare

    // Calculate volume from dimensions
    val (length, width, height) = bookingDetails.dimensions
        .split("x")
        .map { it.trim().toDouble() }
    val volume = length * width * height

    // Base distance fare
    val distanceFare = distance * 15.0  // Base per-km rate

    // Vehicle type multiplier
    val vehicleMultiplier = PriceMultipliers.VEHICLE_MULTIPLIERS[bookingDetails.truckType] ?: 1.0

    // Weight charge
    val weightCharge = bookingDetails.weight * PriceMultipliers.WEIGHT_FACTOR

    // Volume charge
    val volumeCharge = volume * PriceMultipliers.VOLUME_FACTOR

    // Special handling surcharge
    val specialHandlingMultiplier = if (bookingDetails.specialHandling)
        PriceMultipliers.SPECIAL_HANDLING_MULTIPLIER else 1.0

    // Peak hour multiplier (keeping existing logic)
    val peakMultiplier = if (timeOfDay.isAfter(LocalTime.of(17, 0)) &&
        timeOfDay.isBefore(LocalTime.of(20, 0))) {
        1.2
    } else {
        1.0
    }

    // Calculate total fare
    val totalFare = (baseFare + distanceFare) * vehicleMultiplier +
            weightCharge +
            volumeCharge

    // Apply special handling and peak hour multipliers
    val finalFare = totalFare * specialHandlingMultiplier * peakMultiplier

    return finalFare.roundTo(2)
}

// Helper function to round doubles
fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

