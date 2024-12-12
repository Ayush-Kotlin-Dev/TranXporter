package com.ayush.tranxporter.core.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ayush.tranxporter.MainActivityViewModel
import com.ayush.tranxporter.auth.di.authModule
import com.ayush.tranxporter.user.presentation.location.LocationSelectionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val appModule = module {
    includes(authModule)
    includes(coreModule)
    viewModel { MainActivityViewModel(get()) }
    viewModel { (handle: SavedStateHandle) ->
        LocationSelectionViewModel()
    }
}