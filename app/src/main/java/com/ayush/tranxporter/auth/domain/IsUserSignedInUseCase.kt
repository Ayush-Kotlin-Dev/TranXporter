package com.ayush.tranxporter.auth.domain

import javax.inject.Inject

class IsUserSignedInUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(): Boolean = authRepository.isUserSignedIn()
}