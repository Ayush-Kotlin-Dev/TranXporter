package com.ayush.tranxporter.auth.data

import com.google.firebase.auth.PhoneAuthCredential

sealed class AuthResult {
    object CodeSent : AuthResult()
    data class VerificationCompleted(val credential: PhoneAuthCredential) : AuthResult()
    data class Error(val message: String) : AuthResult()
}