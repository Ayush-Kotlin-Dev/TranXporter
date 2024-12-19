package com.ayush.tranxporter.user.presentation.location

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ayush.tranxporter.user.data.BookingStateHolder
import com.ayush.tranxporter.user.presentation.bookingdetails.TransportItemDetails
import com.google.android.gms.maps.model.LatLng

class LocationSelectionViewModel(
    private val stateHolder: BookingStateHolder
) : ViewModel() {
    private val _pickupLocation = mutableStateOf<LocationDetails?>(null)
    val pickupLocation: LocationDetails? get() = _pickupLocation.value

    private val _dropLocation = mutableStateOf<LocationDetails?>(null)
    val dropLocation: LocationDetails? get() = _dropLocation.value

    private val _isUsingCurrentLocation = mutableStateOf(true)
    val isUsingCurrentLocation: Boolean get() = _isUsingCurrentLocation.value

    private var hasSetInitialLocation = false
    fun getBookingDetails(): TransportItemDetails? = stateHolder.bookingDetails



    fun setPickupLocation(latLng: LatLng?, address: String) {
        _pickupLocation.value = if (latLng != null && address.isNotEmpty()) {
            LocationDetails(latLng, address)
        } else {
            null
        }
        _isUsingCurrentLocation.value = false
        hasSetInitialLocation = true
        Log.d("LocationSelectionViewModel", "setPickupLocation: $latLng, $address")
    }

    fun setDropLocation(latLng: LatLng?, address: String) {
        _dropLocation.value = if (latLng != null && address.isNotEmpty()) {
            LocationDetails(latLng, address)
        } else {
            null
        }
        Log.d("LocationSelectionViewModel", "setDropLocation: $latLng, $address")
    }

    fun updateCurrentLocation(location: LatLng, address: String) {
        // Only update if we're using current location and haven't set a pickup location
        if (_isUsingCurrentLocation.value && _pickupLocation.value == null && !hasSetInitialLocation) {
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