package com.ayush.tranxporter.user.presentation.location

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class LocationSelectionViewModel : ViewModel() {
    var pickupLocation by mutableStateOf<LocationDetails?>(null)
        private set

    var dropLocation by mutableStateOf<LocationDetails?>(null)
        private set

    private var _currentUserLocation by mutableStateOf<LatLng?>(null)
    val currentUserLocation: LatLng? get() = _currentUserLocation

    // Use backing property for isSelectingPickup as well
    private var _isSelectingPickup by mutableStateOf(true)
    val isSelectingPickup: Boolean get() = _isSelectingPickup

    data class LocationDetails(
        val latLng: LatLng,
        val address: String
    )

    fun setPickupLocation(latLng: LatLng, address: String) {
        pickupLocation = LocationDetails(latLng, address)
    }

    fun setDropLocation(latLng: LatLng, address: String) {
        dropLocation = LocationDetails(latLng, address)
    }

    fun updateCurrentLocation(latLng: LatLng) {
        _currentUserLocation = latLng
    }

    // Renamed to avoid conflict
    fun updateSelectionMode(isPickup: Boolean) {
        _isSelectingPickup = isPickup
    }

    fun clearLocations() {
        pickupLocation = null
        dropLocation = null
    }
}