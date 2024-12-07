package com.ayush.tranxporter.utils

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

suspend fun getTravelTime(start: LatLng, end: LatLng): String {
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
                    "&key=AIzaSyBRz8M5idMeC-7mYe5y2BOao8PuV84ZGeM"  // Use BuildConfig for API key

            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val jsonData = response.body?.string()

            if (jsonData.isNullOrEmpty()) {
                Log.e("getTravelTime", "Empty response from Google Maps API")
                return@withContext "Unable to calculate"
            }

            val jsonObject = JSONObject(jsonData)
            val status = jsonObject.getString("status")

            if (status != "OK") {
                Log.e("getTravelTime", "API Error: $status")
                return@withContext "Unable to calculate"
            }

            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                if (legs.length() > 0) {
                    val duration = legs.getJSONObject(0).getJSONObject("duration")
                    return@withContext duration.getString("text")
                }
            }

            Log.w("getTravelTime", "No route found")
            "Unable to calculate"
        } catch (e: Exception) {
            Log.e("getTravelTime", "Exception in travel time calculation", e)
            "Unable to calculate"
        }
    }
}

