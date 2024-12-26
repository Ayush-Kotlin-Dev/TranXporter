package com.ayush.tranxporter.auth.domain

import android.app.Activity
import com.ayush.tranxporter.auth.data.AuthResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginWithPhoneUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(
        phoneNumber: String,
        activity: Activity
    ): Flow<AuthResult> = authRepository.sendVerificationCode(phoneNumber, activity)
}