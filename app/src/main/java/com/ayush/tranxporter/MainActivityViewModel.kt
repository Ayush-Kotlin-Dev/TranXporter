package com.ayush.tranxporter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.tranxporter.auth.data.UserRepository
import com.ayush.tranxporter.core.domain.model.AppState
import com.ayush.tranxporter.core.domain.repository.UserStateRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class MainActivityViewModel(
    private val userStateRepository: UserStateRepository
) : ViewModel() {
    private val _appState = MutableStateFlow<AppState>(AppState.Loading)
    val appState = _appState.asStateFlow()

    private val _needsProfileSetup = MutableStateFlow(false)
    val needsProfileSetup = _needsProfileSetup.asStateFlow()

    init {
        viewModelScope.launch {
            val initialState = userStateRepository.getInitialOnboardingState()
            _appState.value = if (!initialState) {
                AppState.NeedsOnboarding
            } else {
                AppState.Ready
            }

            userStateRepository.isProfileCompleted().collect { isCompleted ->
                _needsProfileSetup.value = !isCompleted
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userStateRepository.setOnboardingCompleted(true)
        }
    }

    fun completeProfile() {
        viewModelScope.launch {
            userStateRepository.setProfileCompleted(true)
        }
    }
}