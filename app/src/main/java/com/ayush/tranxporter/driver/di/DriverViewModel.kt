package com.ayush.tranxporter.driver.di

import com.ayush.tranxporter.driver.presentation.home.DriverHomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val driverViewModel = module {
    viewModel { DriverHomeViewModel() }
}