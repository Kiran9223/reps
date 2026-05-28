package com.reps.app.ai

import com.reps.app.BuildConfig
import com.reps.app.core.data.datastore.AppSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime gate for Gemini / cloud features. Build-time API key must be present;
 * user can disable cloud in Settings without rebuilding.
 */
@Singleton
class CloudAssistGate @Inject constructor(
    private val appSettings: AppSettingsDataStore
) {
    val hasCloudKey: Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()

    val isCloudActive: Flow<Boolean> = appSettings.cloudAssistUserEnabled.map { userEnabled ->
        hasCloudKey && userEnabled
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cloudActiveCache = MutableStateFlow(hasCloudKey)

    init {
        scope.launch {
            isCloudActive.collect { cloudActiveCache.value = it }
        }
    }

    fun isCloudActiveSync(): Boolean = cloudActiveCache.value

    suspend fun isCloudActiveNow(): Boolean = isCloudActive.first()
}
