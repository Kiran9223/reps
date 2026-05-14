package com.reps.app.core.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val WATER_DATE = stringPreferencesKey("water_date")
        val WATER_ML = intPreferencesKey("water_ml")
    }

    val isDbSeeded: Flow<Boolean> = dataStore.data.map { it[Keys.IS_DB_SEEDED] ?: false }
    val isOnboardingComplete: Flow<Boolean> = dataStore.data.map { it[Keys.IS_ONBOARDING_COMPLETE] ?: false }

    fun getWaterMl(dateStr: String): Flow<Int> = dataStore.data.map { prefs ->
        if ((prefs[Keys.WATER_DATE] ?: "") == dateStr) prefs[Keys.WATER_ML] ?: 0 else 0
    }

    suspend fun setDbSeeded(seeded: Boolean) = dataStore.edit { it[Keys.IS_DB_SEEDED] = seeded }
    suspend fun setOnboardingComplete(complete: Boolean) = dataStore.edit { it[Keys.IS_ONBOARDING_COMPLETE] = complete }

    suspend fun addWater(dateStr: String, mlToAdd: Int) {
        dataStore.edit { prefs ->
            val storedDate = prefs[Keys.WATER_DATE] ?: ""
            val current = if (storedDate == dateStr) prefs[Keys.WATER_ML] ?: 0 else 0
            prefs[Keys.WATER_DATE] = dateStr
            prefs[Keys.WATER_ML] = (current + mlToAdd).coerceAtLeast(0)
        }
    }
}
