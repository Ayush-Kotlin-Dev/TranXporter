package com.ayush.tranxporter.auth.domain

import android.app.Activity
import com.ayush.tranxporter.auth.data.AuthResult
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    suspend fun sendVerificationCode(
        phoneNumber: String,
        activity: Activity
    ): Flow<AuthResult>

    suspend fun verifyCode(code: String): Result<Unit>
    suspend fun isUserSignedIn(): Boolean
    fun getCurrentUserId(): String?
}