package com.ayush.tranxporter.core.di

import com.ayush.tranxporter.MainActivityViewModel
import com.ayush.tranxporter.auth.di.authModule
import com.ayush.tranxporter.driver.di.driverModule
import com.ayush.tranxporter.user.di.userModule
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val appModule = module {
    includes(authModule)
    includes(coreModule)
    includes(userModule)
    includes(driverModule)
    viewModel { MainActivityViewModel(get()) }

}