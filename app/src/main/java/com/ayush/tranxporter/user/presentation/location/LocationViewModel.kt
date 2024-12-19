package com.ayush.tranxporter.user.presentation.location

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ayush.tranxporter.user.data.BookingStateHolder
import com.ayush.tranxporter.user.data.LocationStateHolder
import com.ayush.tranxporter.user.presentation.bookingdetails.TransportItemDetails
import com.google.android.gms.maps.model.LatLng

class LocationSelectionViewModel(
    private val locationStateHolder: LocationStateHolder,
    private val bookingStateHolder: BookingStateHolder
) : ViewModel() {
    // Exposed location getters directly from StateHolder
    val pickupLocation: LocationDetails? get() = locationStateHolder.pickupLocation
    val dropLocation: LocationDetails? get() = locationStateHolder.dropLocation
    val isUsingCurrentLocation: Boolean get() = locationStateHolder.isUsingCurrentLocation

    // Location setters with validation
    fun setPickupLocation(latLng: LatLng?, address: String) {
        Log.d(TAG, "Setting pickup location: $latLng, $address")

        val locationDetails = if (latLng != null && address.isNotEmpty()) {
            LocationDetails(latLng, address)
        } else null

        locationStateHolder.updatePickupLocation(locationDetails)
        locationStateHolder.setUsingCurrentLocation(false)
    }

    fun setDropLocation(latLng: LatLng?, address: String) {
        Log.d(TAG, "Setting drop location: $latLng, $address")

        val locationDetails = if (latLng != null && address.isNotEmpty()) {
            LocationDetails(latLng, address)
        } else null

        locationStateHolder.updateDropLocation(locationDetails)
    }

    fun updateCurrentLocation(location: LatLng, address: String) {
        Log.d(TAG, "Updating current location: $location, $address")

        if (locationStateHolder.isUsingCurrentLocation &&
            locationStateHolder.pickupLocation == null) {

            locationStateHolder.updatePickupLocation(
                LocationDetails(location, address)
            )
        }
    }

    fun resetLocations() {
        Log.d(TAG, "Resetting all locations")
        locationStateHolder.resetLocations()
    }

    // Utility methods
    fun areLocationsSet(): Boolean {
        return locationStateHolder.pickupLocation != null &&
                locationStateHolder.dropLocation != null
    }

    fun getBookingDetails(): TransportItemDetails? = bookingStateHolder.bookingDetails

    companion object {
        private const val TAG = "LocationSelectionVM"
    }
}

// Keep data class separate for clarity
data class LocationDetails(
    val latLng: LatLng,
    val address: String
) {
    override fun toString(): String = "LocationDetails(latLng=$latLng, address='$address')"
}