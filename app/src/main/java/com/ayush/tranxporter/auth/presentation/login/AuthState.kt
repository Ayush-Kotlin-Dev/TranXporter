package com.ayush.tranxporter.auth.presentation.login

data class AuthState(
    val phoneNumber: String = "",
    val isPhoneValid: Boolean = false,
    val showOtpInput: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val otpState: OtpState = OtpState(
        code = (1..6).map { null },
        focusedIndex = null,
        isValid = null
    ),
    val isAuthenticated: Boolean = false,
    val nextScreen: String? = null
)