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
        val ACTIVE_MEAL_PLAN_ID = androidx.datastore.preferences.core.longPreferencesKey("active_meal_plan_id")
        val IS_MEAL_PLAN_SEEDED = booleanPreferencesKey("is_meal_plan_seeded")
        val GROCERY_BOUGHT_KEYS = stringPreferencesKey("grocery_bought_keys")
        val IS_EXERCISE_SEEDED = booleanPreferencesKey("is_exercise_seeded")
        val IS_SHOULDER_SAFE_ONLY = booleanPreferencesKey("is_shoulder_safe_only")
        val DAILY_INSIGHT_DATE = stringPreferencesKey("daily_insight_date")
        val DAILY_INSIGHT_TEXT = stringPreferencesKey("daily_insight_text")
        val CLOUD_ASSIST_USER_ENABLED = booleanPreferencesKey("cloud_assist_user_enabled")
        val PROTEIN_REMINDERS_ENABLED = booleanPreferencesKey("protein_reminders_enabled")
        val PROTEIN_CELEBRATION_DATE = stringPreferencesKey("protein_celebration_date")
    }

    val isDbSeeded: Flow<Boolean> = dataStore.data.map { it[Keys.IS_DB_SEEDED] ?: false }
    val isOnboardingComplete: Flow<Boolean> = dataStore.data.map { it[Keys.IS_ONBOARDING_COMPLETE] ?: false }
    val isMealPlanSeeded: Flow<Boolean> = dataStore.data.map { it[Keys.IS_MEAL_PLAN_SEEDED] ?: false }
    val isExerciseSeeded: Flow<Boolean> = dataStore.data.map { it[Keys.IS_EXERCISE_SEEDED] ?: false }
    val isShoulderSafeOnly: Flow<Boolean> = dataStore.data.map { it[Keys.IS_SHOULDER_SAFE_ONLY] ?: false }
    val activeMealPlanId: Flow<Long?> = dataStore.data.map { it[Keys.ACTIVE_MEAL_PLAN_ID] }
    val groceryBoughtKeys: Flow<Set<String>> = dataStore.data.map { prefs ->
        (prefs[Keys.GROCERY_BOUGHT_KEYS] ?: "")
            .split(",")
            .filter { it.isNotBlank() }
            .toSet()
    }

    val dailyInsight: Flow<String?> = dataStore.data.map { prefs ->
        val today = java.time.LocalDate.now().toString()
        if ((prefs[Keys.DAILY_INSIGHT_DATE] ?: "") == today) prefs[Keys.DAILY_INSIGHT_TEXT] else null
    }

    val cloudAssistUserEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.CLOUD_ASSIST_USER_ENABLED] ?: true
    }

    val proteinRemindersEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.PROTEIN_REMINDERS_ENABLED] ?: true
    }

    val proteinCelebrationDate: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.PROTEIN_CELEBRATION_DATE]
    }

    fun getWaterMl(dateStr: String): Flow<Int> = dataStore.data.map { prefs ->
        if ((prefs[Keys.WATER_DATE] ?: "") == dateStr) prefs[Keys.WATER_ML] ?: 0 else 0
    }

    suspend fun setDbSeeded(seeded: Boolean) = dataStore.edit { it[Keys.IS_DB_SEEDED] = seeded }
    suspend fun setOnboardingComplete(complete: Boolean) = dataStore.edit { it[Keys.IS_ONBOARDING_COMPLETE] = complete }
    suspend fun setMealPlanSeeded(seeded: Boolean) = dataStore.edit { it[Keys.IS_MEAL_PLAN_SEEDED] = seeded }
    suspend fun setActiveMealPlanId(id: Long) = dataStore.edit { it[Keys.ACTIVE_MEAL_PLAN_ID] = id }
    suspend fun clearActiveMealPlanId() = dataStore.edit { it.remove(Keys.ACTIVE_MEAL_PLAN_ID) }
    suspend fun setExerciseSeeded(seeded: Boolean) = dataStore.edit { it[Keys.IS_EXERCISE_SEEDED] = seeded }
    suspend fun setShoulderSafeOnly(enabled: Boolean) = dataStore.edit { it[Keys.IS_SHOULDER_SAFE_ONLY] = enabled }

    suspend fun toggleGroceryBought(key: String) {
        dataStore.edit { prefs ->
            val current = (prefs[Keys.GROCERY_BOUGHT_KEYS] ?: "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableSet()
            if (key in current) current.remove(key) else current.add(key)
            prefs[Keys.GROCERY_BOUGHT_KEYS] = current.joinToString(",")
        }
    }

    suspend fun saveDailyInsight(text: String) {
        dataStore.edit { prefs ->
            prefs[Keys.DAILY_INSIGHT_DATE] = java.time.LocalDate.now().toString()
            prefs[Keys.DAILY_INSIGHT_TEXT] = text
        }
    }

    suspend fun setCloudAssistUserEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.CLOUD_ASSIST_USER_ENABLED] = enabled }
    }

    suspend fun setProteinRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.PROTEIN_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setProteinCelebrationDate(dateStr: String) {
        dataStore.edit { it[Keys.PROTEIN_CELEBRATION_DATE] = dateStr }
    }

    suspend fun addWater(dateStr: String, mlToAdd: Int) {
        dataStore.edit { prefs ->
            val storedDate = prefs[Keys.WATER_DATE] ?: ""
            val current = if (storedDate == dateStr) prefs[Keys.WATER_ML] ?: 0 else 0
            prefs[Keys.WATER_DATE] = dateStr
            prefs[Keys.WATER_ML] = (current + mlToAdd).coerceAtLeast(0)
        }
    }
}
