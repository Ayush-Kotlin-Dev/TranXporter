package com.ayush.tranxporter.auth.domain

import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(code: String): Result<Unit> =
        authRepository.verifyCode(code)
}