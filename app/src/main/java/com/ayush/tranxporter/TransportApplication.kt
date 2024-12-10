package com.ayush.tranxporter

import android.app.Application
import com.ayush.tranxporter.di.auth.appModule
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TranXporterApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@TranXporterApp)
            modules(appModule)
        }

        FirebaseApp.initializeApp(this)
        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)

    }
}