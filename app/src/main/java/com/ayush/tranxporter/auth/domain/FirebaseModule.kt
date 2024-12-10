package com.ayush.tranxporter.auth.domain

import com.google.firebase.auth.FirebaseAuth
import org.koin.dsl.module

val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseAuthManager(get()) }
}