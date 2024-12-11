package com.ayush.tranxporter.core.di

import com.ayush.tranxporter.MainActivityViewModel
import com.ayush.tranxporter.auth.di.authModule
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val appModule = module {
    includes(authModule)
    includes(coreModule)
    viewModel { MainActivityViewModel(get()) }
}