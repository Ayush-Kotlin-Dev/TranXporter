package com.ayush.tranxporter.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    fun isOnboardingCompleted(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }
    // For initial state
    suspend fun getInitialOnboardingState(): Boolean =
        context.dataStore.data.first()[PreferencesKeys.ONBOARDING_COMPLETED] ?: false

    fun isProfileCompleted(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.PROFILE_COMPLETED] ?: false
        }

    suspend fun setProfileCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROFILE_COMPLETED] = completed
        }
    }

    suspend fun getInitialProfileState(): Boolean =
        context.dataStore.data.first()[PreferencesKeys.PROFILE_COMPLETED] ?: false

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PROFILE_COMPLETED = booleanPreferencesKey("profile_completed")
    }
}