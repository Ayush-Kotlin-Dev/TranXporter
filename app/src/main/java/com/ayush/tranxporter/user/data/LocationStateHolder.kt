package com.ayush.tranxporter.user.data

import androidx.compose.runtime.mutableStateOf
import com.ayush.tranxporter.user.presentation.location.LocationDetails

class LocationStateHolder {
    private val _pickupLocation = mutableStateOf<LocationDetails?>(null)
    val pickupLocation: LocationDetails? get() = _pickupLocation.value

    private val _dropLocation = mutableStateOf<LocationDetails?>(null)
    val dropLocation: LocationDetails? get() = _dropLocation.value

    private val _isUsingCurrentLocation = mutableStateOf(true)
    val isUsingCurrentLocation: Boolean get() = _isUsingCurrentLocation.value

    fun updatePickupLocation(location: LocationDetails?) {
        _pickupLocation.value = location
    }

    fun updateDropLocation(location: LocationDetails?) {
        _dropLocation.value = location
    }

    fun setUsingCurrentLocation(using: Boolean) {
        _isUsingCurrentLocation.value = using
    }

    fun resetLocations() {
        _pickupLocation.value = null
        _dropLocation.value = null
        _isUsingCurrentLocation.value = true
    }
}