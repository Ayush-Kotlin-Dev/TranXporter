package com.ayush.tranxporter.utils

import android.util.Log
import com.ayush.tranxporter.BuildConfig
import com.ayush.tranxporter.user.presentation.location.VehicleType
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalTime
import kotlin.math.round


suspend fun getDrivingDistance(origin: LatLng, destination: LatLng): Double? {
    return withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.MAPS_API_KEY
            val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&key=$apiKey"

            val response = URL(url).readText()
            val jsonObject = JSONObject(response)

            if (jsonObject.getString("status") == "OK") {
                val route = jsonObject.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONArray("legs")
                    .getJSONObject(0)

                val distance = route.getJSONObject("distance")
                    .getInt("value") / 1000.0  // Convert meters to kilometers

                distance
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

suspend fun calculateFare(
    start: LatLng,
    end: LatLng,
    vehicleType: VehicleType = VehicleType.SMALL_TRUCK,
    timeOfDay: LocalTime = LocalTime.now(),
    baseFare: Double = 50.0
): Double {
    // Get actual driving distance
    val distance = getDrivingDistance(start, end) ?: return baseFare // Return base fare if distance calculation fails

    val perKmRate = when (vehicleType) {
        VehicleType.SMALL_TRUCK -> 15.0
        VehicleType.LARGE_TRUCK -> 25.0
    }

    // Peak hour multiplier
    val peakMultiplier = if (timeOfDay.isAfter(LocalTime.of(17, 0)) &&
        timeOfDay.isBefore(LocalTime.of(20, 0))) {
        1.2 // 20% increase during peak hours
    } else {
        1.0
    }

    // Total fare calculation
    val fare = baseFare + (distance * perKmRate * peakMultiplier)
    return fare.roundTo(2)
}

// Helper function to round doubles
fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

