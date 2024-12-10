package com.ayush.tranxporter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.tranxporter.core.data.UserPreferences
import com.ayush.tranxporter.core.domain.model.AppState
import com.ayush.tranxporter.core.domain.repository.UserStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch


class MainActivityViewModel(
    private val userStateRepository: UserStateRepository
) : ViewModel() {

    private val _appState = MutableStateFlow<AppState>(AppState.Loading)
    val appState = _appState.asStateFlow()

    init {
        viewModelScope.launch {
            // Get initial state first
            val initialState = userStateRepository.getInitialOnboardingState()
            _appState.value = if (initialState) AppState.Ready else AppState.NeedsOnboarding

            // Then start observing changes
            userStateRepository.isOnboardingCompleted()
                .collect { isCompleted ->
                    _appState.value = if (isCompleted) AppState.Ready else AppState.NeedsOnboarding
                }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userStateRepository.setOnboardingCompleted(true)
        }
    }
}