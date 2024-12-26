package com.ayush.tranxporter.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserStateRepository {
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun getInitialOnboardingState(): Boolean
    suspend fun setOnboardingCompleted(completed: Boolean)


    fun isProfileCompleted(): Flow<Boolean>
    suspend fun getInitialProfileState(): Boolean
    suspend fun setProfileCompleted(completed: Boolean)

    suspend fun logout()
}