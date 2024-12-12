package com.ayush.tranxporter.core.domain.repository

import com.ayush.tranxporter.core.components.UserType
import kotlinx.coroutines.flow.Flow

interface UserStateRepository {
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun getInitialOnboardingState(): Boolean
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun logout()
}