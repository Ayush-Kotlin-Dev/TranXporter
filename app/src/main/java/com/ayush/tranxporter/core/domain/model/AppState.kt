package com.ayush.tranxporter.core.domain.model

sealed class AppState {
    object Loading : AppState()
    object NeedsOnboarding : AppState()
    object Ready : AppState()
}