package com.ayush.tranxporter.auth.di

import com.ayush.tranxporter.auth.domain.FirebaseAuthManager
import com.google.firebase.auth.FirebaseAuth
import org.koin.dsl.module

val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseAuthManager(get()) }
}