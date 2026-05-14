package com.reps.app.core.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.userPreferencesDataStore

    private object Keys {
        val NAME = stringPreferencesKey("name")
        val AGE = intPreferencesKey("age")
        val WEIGHT_KG = doublePreferencesKey("weight_kg")
        val TARGET_WEIGHT_KG = doublePreferencesKey("target_weight_kg")
        val HEIGHT_CM = doublePreferencesKey("height_cm")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
        val WORKOUT_DAYS_PER_WEEK = intPreferencesKey("workout_days_per_week")
        val HAS_SHOULDER_RESTRICTION = booleanPreferencesKey("has_shoulder_restriction")
        val DIETARY_RESTRICTIONS = stringPreferencesKey("dietary_restrictions")
        val CUISINE_PREFERENCES = stringPreferencesKey("cuisine_preferences")
    }

    val name: Flow<String> = dataStore.data.map { it[Keys.NAME] ?: "" }
    val age: Flow<Int> = dataStore.data.map { it[Keys.AGE] ?: 0 }
    val weightKg: Flow<Double> = dataStore.data.map { it[Keys.WEIGHT_KG] ?: 0.0 }
    val targetWeightKg: Flow<Double> = dataStore.data.map { it[Keys.TARGET_WEIGHT_KG] ?: 0.0 }
    val heightCm: Flow<Double> = dataStore.data.map { it[Keys.HEIGHT_CM] ?: 0.0 }
    val activityLevel: Flow<String> = dataStore.data.map { it[Keys.ACTIVITY_LEVEL] ?: "MODERATE" }
    val workoutDaysPerWeek: Flow<Int> = dataStore.data.map { it[Keys.WORKOUT_DAYS_PER_WEEK] ?: 3 }
    val hasShoulderRestriction: Flow<Boolean> = dataStore.data.map { it[Keys.HAS_SHOULDER_RESTRICTION] ?: false }
    val dietaryRestrictions: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[Keys.DIETARY_RESTRICTIONS]?.let {
            runCatching { Json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
    }
    val cuisinePreferences: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[Keys.CUISINE_PREFERENCES]?.let {
            runCatching { Json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    suspend fun updateProfile(
        name: String,
        age: Int,
        weightKg: Double,
        targetWeightKg: Double,
        heightCm: Double,
        activityLevel: String,
        workoutDaysPerWeek: Int,
        hasShoulderRestriction: Boolean,
        dietaryRestrictions: List<String>,
        cuisinePreferences: List<String>
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.NAME] = name
            prefs[Keys.AGE] = age
            prefs[Keys.WEIGHT_KG] = weightKg
            prefs[Keys.TARGET_WEIGHT_KG] = targetWeightKg
            prefs[Keys.HEIGHT_CM] = heightCm
            prefs[Keys.ACTIVITY_LEVEL] = activityLevel
            prefs[Keys.WORKOUT_DAYS_PER_WEEK] = workoutDaysPerWeek
            prefs[Keys.HAS_SHOULDER_RESTRICTION] = hasShoulderRestriction
            prefs[Keys.DIETARY_RESTRICTIONS] = Json.encodeToString(dietaryRestrictions)
            prefs[Keys.CUISINE_PREFERENCES] = Json.encodeToString(cuisinePreferences)
        }
    }
}
