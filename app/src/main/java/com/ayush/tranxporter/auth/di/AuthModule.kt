package com.ayush.tranxporter.auth.di

import com.ayush.tranxporter.auth.data.UserRepository
import com.ayush.tranxporter.auth.domain.FirebaseAuthManager
import com.ayush.tranxporter.auth.presentation.login.AuthViewModel
import com.ayush.tranxporter.auth.presentation.service_selection.UserDetailsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    includes(firebaseModule)
    viewModel { AuthViewModel(get()) }
    viewModel { UserDetailsViewModel(get()) }
    single { FirebaseAuthManager(get()) }
    single { UserRepository(get()) }
}