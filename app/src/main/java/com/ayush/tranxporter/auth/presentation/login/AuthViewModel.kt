package com.ayush.tranxporter.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    fun onEvent(event: AuthEvent) {
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
                if (state.value.isPhoneValid) {
                    viewModelScope.launch {
                        _state.update { it.copy(isLoading = true) }
                        try {
                            kotlinx.coroutines.delay(1500)
                            _state.update { it.copy(
                                showOtpInput = true,
                                isLoading = false
                            ) }
                        } catch (e: Exception) {
                            _state.update { it.copy(
                                error = e.message,
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
                if (state.value.otpState.isValid == true) {
                    viewModelScope.launch {
                        _state.update { it.copy(isLoading = true) }
                        try {
                            kotlinx.coroutines.delay(1500)
                            _state.update { it.copy(
                                isAuthenticated = true,
                                isLoading = false
                            ) }
                        } catch (e: Exception) {
                            _state.update { it.copy(
                                error = e.message,
                                isLoading = false
                            ) }
                        }
                    }
                }
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
                isValid = if(newCode.none { it == null }) {
                    newCode.joinToString("") == "1414"
                } else null
            )
        ) }
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

        if(currentFocusedIndex == 3) {
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