package com.ayush.tranxporter.utils

import com.ayush.tranxporter.user.VehicleType
import com.google.android.gms.maps.model.LatLng
import java.time.LocalTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun haversineDistance(start: LatLng, end: LatLng): Double {
    val earthRadius = 6371.0 // kilometers

    val dLat = Math.toRadians(end.latitude - start.latitude)
    val dLon = Math.toRadians(end.longitude - start.longitude)

    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(start.latitude)) *
            cos(Math.toRadians(end.latitude)) *
            sin(dLon / 2).pow(2.0)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c
}

fun calculateFare(
    start: LatLng,
    end: LatLng,
    vehicleType: VehicleType = VehicleType.SMALL_TRUCK,
    timeOfDay: LocalTime = LocalTime.now(),
    baseFare: Double = 50.0
): Double {
    val distance = haversineDistance(start, end)
    val perKmRate = when (vehicleType) {
        VehicleType.SMALL_TRUCK -> 15.0
        VehicleType.LARGE_TRUCK -> 25.0
    }

    // Peak hour multiplier
    val peakMultiplier = if (timeOfDay.isAfter(LocalTime.of(17, 0)) && timeOfDay.isBefore(LocalTime.of(20, 0))) {
        1.2 // 20% increase during peak hours
    } else {
        1.0
    }

    // Total fare calculation
    val fare = baseFare + (distance * perKmRate * peakMultiplier)
    return fare
}