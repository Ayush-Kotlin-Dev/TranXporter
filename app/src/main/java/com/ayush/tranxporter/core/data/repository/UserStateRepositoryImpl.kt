package com.ayush.tranxporter.core.data.repository

import com.ayush.tranxporter.core.data.UserPreferences
import com.ayush.tranxporter.core.domain.repository.UserStateRepository
import kotlinx.coroutines.flow.Flow

class UserStateRepositoryImpl(
    private val userPreferences: UserPreferences
) : UserStateRepository {

    override fun isOnboardingCompleted(): Flow<Boolean> =
        userPreferences.isOnboardingCompleted()

    override suspend fun getInitialOnboardingState(): Boolean =
        userPreferences.getInitialOnboardingState()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        userPreferences.setOnboardingCompleted(completed)
    }


    override fun isProfileCompleted(): Flow<Boolean> =
        userPreferences.isProfileCompleted()

    override suspend fun getInitialProfileState(): Boolean =
        userPreferences.getInitialProfileState()

    override suspend fun setProfileCompleted(completed: Boolean) {
        userPreferences.setProfileCompleted(completed)
    }

    override suspend fun logout() {
        // Will implement later with Firebase Auth
    }



}