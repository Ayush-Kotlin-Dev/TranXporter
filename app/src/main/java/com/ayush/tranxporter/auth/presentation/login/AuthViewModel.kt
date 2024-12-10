package com.ayush.tranxporter.auth.presentation.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.tranxporter.auth.domain.FirebaseAuthManager
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authManager: FirebaseAuthManager
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    init {
        // Check if user is already signed in
        if (authManager.isUserSignedIn()) {
            _state.update { it.copy(isAuthenticated = true) }
        }
    }

    fun onEvent(event: AuthEvent, activity: Activity? = null) {
        when (event) {
            is AuthEvent.OnPhoneNumberChange -> {
                if (event.number.length <= 10) {
                    _state.update { it.copy(
                        phoneNumber = event.number,
                        isPhoneValid = event.number.length == 10
                    ) }
                }
            }

            is AuthEvent.OnSubmitPhone -> {
                if (state.value.isPhoneValid && activity != null) {
                    viewModelScope.launch {
                        _state.update { it.copy(isLoading = true) }
                        try {
                            authManager.sendVerificationCode(
                                phoneNumber = "+91${state.value.phoneNumber}",
                                activity = activity,
                                onCodeSent = {
                                    _state.update { it.copy(
                                        showOtpInput = true,
                                        isLoading = false
                                    ) }
                                },
                                onVerificationCompleted = { credential ->
                                    handleCredential(credential)
                                },
                                onError = { error ->
                                    _state.update { it.copy(
                                        error = error,
                                        isLoading = false
                                    ) }
                                }
                            )
                        } catch (e: Exception) {
                            _state.update { it.copy(
                                error = e.message ?: "Failed to send OTP",
                                isLoading = false
                            ) }
                        }
                    }
                }
            }

            is AuthEvent.OnOtpAction -> {
                when (event.action) {
                    is OtpAction.OnChangeFieldFocused -> {
                        _state.update { it.copy(
                            otpState = it.otpState.copy(
                                focusedIndex = event.action.index
                            )
                        ) }
                    }
                    is OtpAction.OnEnterNumber -> {
                        enterNumber(event.action.number, event.action.index)
                    }
                    OtpAction.OnKeyboardBack -> {
                        val previousIndex = getPreviousFocusedIndex(state.value.otpState.focusedIndex)
                        _state.update { it.copy(
                            otpState = it.otpState.copy(
                                code = it.otpState.code.mapIndexed { index, number ->
                                    if(index == previousIndex) {
                                        null
                                    } else {
                                        number
                                    }
                                },
                                focusedIndex = previousIndex
                            )
                        ) }
                    }
                }
            }

            is AuthEvent.OnVerifyOtp -> {
                val otpCode = state.value.otpState.code.joinToString("")
                if (otpCode.length == 6) {
                    viewModelScope.launch {
                        _state.update { it.copy(isLoading = true) }
                        authManager.verifyCode(
                            code = otpCode,
                            onSuccess = {
                                _state.update { it.copy(
                                    isAuthenticated = true,
                                    isLoading = false
                                ) }
                            },
                            onError = { error ->
                                _state.update { it.copy(
                                    error = error,
                                    isLoading = false
                                ) }
                            }
                        )
                    }
                }
            }

            is AuthEvent.OnResendOtp -> {
                activity?.let { act ->
                    _state.update { it.copy(isLoading = true) }
                    authManager.sendVerificationCode(
                        phoneNumber = "+91${state.value.phoneNumber}",
                        activity = act,
                        onCodeSent = {
                            _state.update { it.copy(
                                isLoading = false,
                                otpState = OtpState(code = (1..6).map { null })
                            ) }
                        },
                        onVerificationCompleted = { credential ->
                            handleCredential(credential)
                        },
                        onError = { error ->
                            _state.update { it.copy(
                                error = error,
                                isLoading = false
                            ) }
                        }
                    )
                }
            }
        }
    }


    private fun handleCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // Auto-fill the OTP if available
                credential.smsCode?.let { smsCode ->
                    val otpDigits = smsCode.map { it.toString().toIntOrNull() }
                    _state.update { it.copy(
                        otpState = it.otpState.copy(
                            code = otpDigits
                        )
                    ) }
                }

                authManager.verifyCode(
                    code = credential.smsCode ?: "",
                    onSuccess = {
                        _state.update { it.copy(
                            isAuthenticated = true,
                            isLoading = false
                        ) }
                    },
                    onError = { error ->
                        _state.update { it.copy(
                            error = error,
                            isLoading = false
                        ) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(
                    error = e.message ?: "Authentication failed",
                    isLoading = false
                ) }
            }
        }
    }

    private fun enterNumber(number: Int?, index: Int) {
        val newCode = state.value.otpState.code.mapIndexed { currentIndex, currentNumber ->
            if(currentIndex == index) {
                number
            } else {
                currentNumber
            }
        }
        val wasNumberRemoved = number == null
        _state.update { it.copy(
            otpState = it.otpState.copy(
                code = newCode,
                focusedIndex = if(wasNumberRemoved || it.otpState.code.getOrNull(index) != null) {
                    it.otpState.focusedIndex
                } else {
                    getNextFocusedTextFieldIndex(
                        currentCode = it.otpState.code,
                        currentFocusedIndex = it.otpState.focusedIndex
                    )
                },
                // Update validation logic
                isValid = if(newCode.none { it == null }) {
                    true  // If all fields are filled, enable verification button
                } else null
            )
        ) }

        // Automatically trigger verification if all digits are entered
        if (newCode.none { it == null }) {
            viewModelScope.launch {
                val otpCode = newCode.joinToString("")
                _state.update { it.copy(isLoading = true) }
                authManager.verifyCode(
                    code = otpCode,
                    onSuccess = {
                        _state.update { it.copy(
                            isAuthenticated = true,
                            isLoading = false
                        ) }
                    },
                    onError = { error ->
                        _state.update { it.copy(
                            error = error,
                            isLoading = false,
                            otpState = it.otpState.copy(isValid = false)
                        ) }
                    }
                )
            }
        }
    }

    private fun getPreviousFocusedIndex(currentIndex: Int?): Int? {
        return currentIndex?.minus(1)?.coerceAtLeast(0)
    }

    private fun getNextFocusedTextFieldIndex(
        currentCode: List<Int?>,
        currentFocusedIndex: Int?
    ): Int? {
        if(currentFocusedIndex == null) {
            return null
        }

        if(currentFocusedIndex == 5) {
            return currentFocusedIndex
        }

        return getFirstEmptyFieldIndexAfterFocusedIndex(
            code = currentCode,
            currentFocusedIndex = currentFocusedIndex
        )
    }

    private fun getFirstEmptyFieldIndexAfterFocusedIndex(
        code: List<Int?>,
        currentFocusedIndex: Int
    ): Int {
        code.forEachIndexed { index, number ->
            if(index <= currentFocusedIndex) {
                return@forEachIndexed
            }
            if(number == null) {
                return index
            }
        }
        return currentFocusedIndex
    }

}