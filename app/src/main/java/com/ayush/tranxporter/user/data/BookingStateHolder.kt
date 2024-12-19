package com.ayush.tranxporter.user.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ayush.tranxporter.user.presentation.bookingdetails.TransportItemDetails

class BookingStateHolder {
    var bookingDetails by mutableStateOf<TransportItemDetails?>(null)
        private set

    fun updateBookingDetails(details: TransportItemDetails?) {
        bookingDetails = details
    }
}