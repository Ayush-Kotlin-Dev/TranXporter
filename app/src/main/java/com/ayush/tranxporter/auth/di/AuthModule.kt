package com.ayush.tranxporter.auth.di

import com.ayush.tranxporter.auth.data.UserRepository
import com.ayush.tranxporter.auth.data.AuthRepository
import com.ayush.tranxporter.auth.domain.IAuthRepository
import com.ayush.tranxporter.auth.domain.IsUserSignedInUseCase
import com.ayush.tranxporter.auth.domain.LoginWithPhoneUseCase
import com.ayush.tranxporter.auth.domain.VerifyOtpUseCase
import com.ayush.tranxporter.auth.presentation.login.AuthViewModel
import com.ayush.tranxporter.auth.presentation.service_selection.UserDetailsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule = module {
    includes(firebaseModule)

    single<IAuthRepository> { AuthRepository(get()) }
    single { UserRepository(get()) }

    factory { LoginWithPhoneUseCase(get()) }
    factory { VerifyOtpUseCase(get()) }
    factory { IsUserSignedInUseCase(get()) }

    viewModel {
        AuthViewModel(
            loginWithPhoneUseCase = get(),
            verifyOtpUseCase = get(),
            isUserSignedIn = get()
        )
    }
    viewModel{
        UserDetailsViewModel(
            userRepository = get(),
            userStateRepository = get()
        )
    }
}