package com.ayush.tranxporter.utils

import android.util.Log
import com.ayush.tranxporter.BuildConfig
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Add this function to decode the polyline and get route points
suspend fun getRoutePoints(start: LatLng, end: LatLng): List<LatLng> {
    return withContext(Dispatchers.IO) {
        try {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${start.latitude},${start.longitude}" +
                    "&destination=${end.latitude},${end.longitude}" +
                    "&mode=driving" +
                    "&key=${BuildConfig.MAPS_API_KEY}"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val jsonData = response.body?.string()

            if (jsonData.isNullOrEmpty()) {
                Log.e("Route", "Empty response from Google Maps API")
                return@withContext emptyList()
            }

            val jsonObject = JSONObject(jsonData)
            val status = jsonObject.getString("status")

            if (status != "OK") {
                Log.e("Route", "API Error: $status")
                return@withContext emptyList()
            }

            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val points = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")

                val decodedPoints = decodePolyline(points)
                Log.d("Route", "Decoded ${decodedPoints.size} points") // Debug log
                return@withContext decodedPoints
            }

            emptyList()
        } catch (e: Exception) {
            Log.e("Route", "Failed to get route points", e)
            emptyList()
        }
    }
}
// Add this function to decode the polyline string
fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val p = LatLng(
            lat.toDouble() / 1E5,
            lng.toDouble() / 1E5
        )
        poly.add(p)
    }

    return poly
}