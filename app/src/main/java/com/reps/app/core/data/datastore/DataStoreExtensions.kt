package com.reps.app.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.userPreferencesDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "user_preferences")

internal val Context.appSettingsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "app_settings")
