package com.ayush.tranxporter.user.presentation.location

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng

class LocationSelectionViewModel : ViewModel() {
    private val _pickupLocation = mutableStateOf<LocationDetails?>(null)
    val pickupLocation: LocationDetails? get() = _pickupLocation.value

    private val _dropLocation = mutableStateOf<LocationDetails?>(null)
    val dropLocation: LocationDetails? get() = _dropLocation.value

    private val _isUsingCurrentLocation = mutableStateOf(true)
    val isUsingCurrentLocation: Boolean get() = _isUsingCurrentLocation.value

    private var hasSetInitialLocation = false

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LocationSelectionViewModel() as T
            }
        }
    }


    fun setPickupLocation(latLng: LatLng, address: String) {
        _pickupLocation.value = LocationDetails(latLng, address)
        _isUsingCurrentLocation.value = false // Set to false when user manually selects location
        Log.d("LocationSelectionViewModel", "setPickupLocation: $latLng, $address")
    }

    fun setDropLocation(latLng: LatLng, address: String) {
        _dropLocation.value = LocationDetails(latLng, address)
        _isUsingCurrentLocation.value = false // Set to false when user manually selects location
        Log.d("LocationSelectionViewModel", "setDropLocation: $latLng, $address")
    }

    fun updateCurrentLocation(location: LatLng, address: String) {
        // Only update if we haven't set any location yet
        if (!hasSetInitialLocation) {
            _pickupLocation.value = LocationDetails(location, address)
            hasSetInitialLocation = true
        }
        Log.d("LocationSelectionViewModel", "updateCurrentLocation: $location, $address")
    }

    fun resetLocations() {
        _pickupLocation.value = null
        _dropLocation.value = null
        _isUsingCurrentLocation.value = true
    }

    override fun onCleared() {
        Log.d("LocationSelectionViewModel", "onCleared")
        super.onCleared()
    }
}
data class LocationDetails(
    val latLng: LatLng,
    val address: String
)