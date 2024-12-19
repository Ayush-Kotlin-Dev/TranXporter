package com.ayush.tranxporter.user.di

import androidx.lifecycle.SavedStateHandle
import com.ayush.tranxporter.user.data.BookingStateHolder
import com.ayush.tranxporter.user.presentation.bookingdetails.BookingDetailsViewModel
import com.ayush.tranxporter.user.presentation.location.LocationSelectionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val userModule = module {
    single { BookingStateHolder() }
    viewModel {
        BookingDetailsViewModel(get())
    }
    viewModel {
        LocationSelectionViewModel(get())
    }
}