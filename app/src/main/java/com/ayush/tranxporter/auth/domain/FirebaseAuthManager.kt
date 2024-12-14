package com.ayush.tranxporter.auth.domain

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthOptions
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FirebaseAuthManager @Inject constructor(
    private val auth: FirebaseAuth
) {
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendVerificationCode(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: () -> Unit,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit,
        onError: (String) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onVerificationCompleted(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onError(e.message ?: "Verification failed")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                this@FirebaseAuthManager.verificationId = verificationId
                this@FirebaseAuthManager.resendToken = token
                onCodeSent()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .apply {
                resendToken?.let { setForceResendingToken(it) }
            }
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode(
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        onError(task.exception?.message ?: "Verification failed")
                    }
                }
        } catch (e: Exception) {
            onError(e.message ?: "Invalid verification code")
        }
    }



    fun isUserSignedIn(): Boolean = auth.currentUser != null

    fun getCurrentUser(): FirebaseUser? = auth.currentUser
}