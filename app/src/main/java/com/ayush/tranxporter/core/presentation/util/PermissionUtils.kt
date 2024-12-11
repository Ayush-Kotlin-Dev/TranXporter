package com.ayush.tranxporter.core.presentation.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import com.ayush.tranxporter.R
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import com.google.android.gms.maps.model.LatLng

object PermissionUtils {
    // Permission definitions
    val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    val CALL_LOG_PERMISSION = Manifest.permission.READ_CALL_LOG
    val STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE

    data class Permission(
        val permission: String,
        val title: String,
        val description: String,
        val icon: Int
    )

    // List of required permissions with their details
    val requiredPermissions = listOf(
        Permission(
            LOCATION_PERMISSION,
            "Location Access",
            "To track shipments and find nearby drivers",
            R.drawable.ic_location
        ),
        Permission(
            CALL_LOG_PERMISSION,
            "Manage Call Logs",
            "For seamless communication with drivers",
            R.drawable.ic_call
        ),
        Permission(
            STORAGE_PERMISSION,
            "Store Call Logs",
            "To maintain delivery records",
            R.drawable.ic_storage
        )
    )

    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkMultiplePermissions(context: Context, permissions: List<String>): Boolean {
        return permissions.all { permission ->
            checkPermission(context, permission)
        }
    }
    fun checkLocationPermission(context: Context): Boolean {
        return checkPermission(context, LOCATION_PERMISSION)
    }

    suspend fun getCurrentLocation(context: Context): LatLng? {
        if (!checkLocationPermission(context)) return null

        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedLocationClient.lastLocation.await()
            location?.let {
                LatLng(it.latitude, it.longitude)  // This now returns the correct LatLng type
            }
        } catch (e: Exception) {
            Log.e("Location", "Error getting location", e)
            null
        }
    }

    suspend fun getAddressFromLocation(context: Context, latLng: LatLng): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    addresses[0]?.let { address ->
                        address.getAddressLine(0) ?: "Current Location"
                    } ?: "Current Location"
                } else {
                    "Current Location"
                }
            } catch (e: Exception) {
                Log.e("Geocoder", "Error getting address", e)
                "Current Location"
            }
        }
    }
}
