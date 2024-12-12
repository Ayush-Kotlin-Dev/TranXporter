package com.ayush.tranxporter.core.di

import com.ayush.tranxporter.core.data.UserPreferences
import com.ayush.tranxporter.core.data.repository.UserStateRepositoryImpl
import com.ayush.tranxporter.core.domain.repository.UserStateRepository
import org.koin.dsl.module

val coreModule = module {
    single { UserPreferences(get()) }
    single<UserStateRepository> { UserStateRepositoryImpl(get()) }
}