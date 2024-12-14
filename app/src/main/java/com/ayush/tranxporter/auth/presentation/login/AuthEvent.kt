package com.ayush.tranxporter.auth.presentation.login

sealed interface AuthEvent {
    data class OnPhoneNumberChange(val number: String) : AuthEvent
    object OnSubmitPhone : AuthEvent
    data class OnOtpAction(val action: OtpAction) : AuthEvent
    object OnVerifyOtp : AuthEvent
    object OnResendOtp : AuthEvent
    object ShowPhoneInput : AuthEvent
    object DismissError : AuthEvent


}