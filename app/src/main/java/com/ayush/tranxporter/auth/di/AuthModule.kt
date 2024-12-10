package com.ayush.tranxporter.auth.di

import com.ayush.tranxporter.auth.domain.FirebaseAuthManager
import com.ayush.tranxporter.auth.presentation.login.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    includes(firebaseModule)
    viewModel { AuthViewModel(get()) }
    single { FirebaseAuthManager(get()) }
}