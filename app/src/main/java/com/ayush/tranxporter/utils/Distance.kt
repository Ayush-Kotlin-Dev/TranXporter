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
import kotlin.math.max
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
    // Base fare per vehicle type (in Rupees)
    val BASE_FARE = mapOf(
        TruckType.PICKUP to 300.0,      // Small loads, local delivery
        TruckType.LORRY to 500.0,       // Medium loads, urban delivery
        TruckType.SIXTEEN_WHEELER to 1000.0,  // Heavy loads, interstate
        TruckType.TRACTOR to 800.0      // Rural, agricultural loads
    )

    // Per kilometer rates by vehicle type
    val PER_KM_RATE = mapOf(
        TruckType.PICKUP to 12.0,       // Economical for short distances
        TruckType.LORRY to 18.0,        // Standard rate for medium trucks
        TruckType.SIXTEEN_WHEELER to 25.0, // Higher rate for large trucks
        TruckType.TRACTOR to 20.0       // Off-road capable rate
    )

    // Weight tiers and their multipliers
    val WEIGHT_TIERS = listOf(
        WeightTier(0.0, 500.0, 1.0),      // Base rate up to 500 kg
        WeightTier(500.0, 1000.0, 1.2),   // 20% extra for 500-1000 kg
        WeightTier(1000.0, 5000.0, 1.5),  // 50% extra for 1-5 tons
        WeightTier(5000.0, Double.MAX_VALUE, 2.0) // Double rate for 5+ tons
    )

    // Time-based multipliers
    val TIME_MULTIPLIERS = mapOf(
        TimeSlot.EARLY_MORNING to 1.1,  // 5 AM - 8 AM
        TimeSlot.PEAK_MORNING to 1.3,   // 8 AM - 11 AM
        TimeSlot.AFTERNOON to 1.0,      // 11 AM - 4 PM
        TimeSlot.PEAK_EVENING to 1.3,   // 4 PM - 8 PM
        TimeSlot.NIGHT to 1.2           // 8 PM - 5 AM
    )

    // Additional charges
    const val LOADING_UNLOADING_CHARGE = 200.0  // Per service
    const val SPECIAL_HANDLING_CHARGE = 500.0    // Fragile items
    const val INSURANCE_PERCENTAGE = 0.01        // 1% of declared value
    const val FUEL_SURCHARGE = 0.05             // 5% of base fare
    const val GST_RATE = 0.18                   // 18% GST
}

data class WeightTier(
    val min: Double,
    val max: Double,
    val multiplier: Double
)

enum class TimeSlot {
    EARLY_MORNING,
    PEAK_MORNING,
    AFTERNOON,
    PEAK_EVENING,
    NIGHT
}

suspend fun calculateFare(
    start: LatLng,
    end: LatLng,
    bookingDetails: TransportItemDetails,
    timeOfDay: LocalTime = LocalTime.now(),
    declaredValue: Double = 0.0
): FareBreakdown {
    val distance = getDrivingDistance(start, end) ?: throw IllegalStateException("Unable to calculate distance")

    // Get base fare and per km rate for vehicle type
    val baseFare = PriceMultipliers.BASE_FARE[bookingDetails.truckType]
        ?: throw IllegalArgumentException("Invalid vehicle type")
    val perKmRate = PriceMultipliers.PER_KM_RATE[bookingDetails.truckType] ?: 15.0

    // Calculate distance charge with minimum distance of 5 km
    val effectiveDistance = max(distance, 5.0)
    val distanceCharge = effectiveDistance * perKmRate

    // Calculate weight multiplier
    val weightMultiplier = PriceMultipliers.WEIGHT_TIERS
        .find { bookingDetails.weight >= it.min && bookingDetails.weight < it.max }
        ?.multiplier ?: 2.0

    // Get time multiplier
    val timeMultiplier = getTimeMultiplier(timeOfDay)

    // Calculate additional charges
    val loadingUnloadingCharge = PriceMultipliers.LOADING_UNLOADING_CHARGE

    val specialHandlingCharge = if (bookingDetails.specialHandling)
        PriceMultipliers.SPECIAL_HANDLING_CHARGE else 0.0

    val insuranceCharge = declaredValue * PriceMultipliers.INSURANCE_PERCENTAGE

    // Calculate subtotal
    val subtotal = (baseFare + distanceCharge) * weightMultiplier * timeMultiplier

    // Add fuel surcharge
    val fuelSurcharge = subtotal * PriceMultipliers.FUEL_SURCHARGE

    // Calculate total before tax
    val totalBeforeTax = subtotal +
            loadingUnloadingCharge +
            specialHandlingCharge +
            insuranceCharge +
            fuelSurcharge

    // Calculate GST
    val gst = totalBeforeTax * PriceMultipliers.GST_RATE

    // Calculate final total
    val finalTotal = totalBeforeTax + gst

    return FareBreakdown(
        baseFare = round(baseFare * 100) / 100,
        distanceCharge = round(distanceCharge * 100) / 100,
        weightMultiplier = weightMultiplier,
        timeMultiplier = timeMultiplier,
        loadingUnloadingCharge = loadingUnloadingCharge,
        specialHandlingCharge = specialHandlingCharge,
        insuranceCharge = round(insuranceCharge * 100) / 100,
        fuelSurcharge = round(fuelSurcharge * 100) / 100,
        subtotal = round(totalBeforeTax * 100) / 100,
        gst = round(gst * 100) / 100,
        finalTotal = round(finalTotal * 100) / 100,
        distance = effectiveDistance
    )
}

private fun getTimeMultiplier(time: LocalTime): Double {
    return when {
        time.isInRange(5, 8) -> PriceMultipliers.TIME_MULTIPLIERS[TimeSlot.EARLY_MORNING]
        time.isInRange(8, 11) -> PriceMultipliers.TIME_MULTIPLIERS[TimeSlot.PEAK_MORNING]
        time.isInRange(11, 16) -> PriceMultipliers.TIME_MULTIPLIERS[TimeSlot.AFTERNOON]
        time.isInRange(16, 20) -> PriceMultipliers.TIME_MULTIPLIERS[TimeSlot.PEAK_EVENING]
        else -> PriceMultipliers.TIME_MULTIPLIERS[TimeSlot.NIGHT]
    } ?: 1.0
}

private fun LocalTime.isInRange(startHour: Int, endHour: Int): Boolean {
    return this.isAfter(LocalTime.of(startHour, 0)) &&
            this.isBefore(LocalTime.of(endHour, 0))
}

data class FareBreakdown(
    val baseFare: Double,
    val distanceCharge: Double,
    val weightMultiplier: Double,
    val timeMultiplier: Double,
    val loadingUnloadingCharge: Double,
    val specialHandlingCharge: Double,
    val insuranceCharge: Double,
    val fuelSurcharge: Double,
    val subtotal: Double,
    val gst: Double,
    val finalTotal: Double,
    val distance: Double
)