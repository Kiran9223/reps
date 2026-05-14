package com.reps.app.core.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.appSettingsDataStore

    private object Keys {
        val IS_DB_SEEDED = booleanPreferencesKey("is_db_seeded")
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
    }

    val isDbSeeded: Flow<Boolean> = dataStore.data.map { it[Keys.IS_DB_SEEDED] ?: false }
    val isOnboardingComplete: Flow<Boolean> = dataStore.data.map { it[Keys.IS_ONBOARDING_COMPLETE] ?: false }

    suspend fun setDbSeeded(seeded: Boolean) {
        dataStore.edit { it[Keys.IS_DB_SEEDED] = seeded }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[Keys.IS_ONBOARDING_COMPLETE] = complete }
    }
}
