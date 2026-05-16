package com.reps.app.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.ai.AIRepository
import com.reps.app.ai.ChatMessage
import com.reps.app.core.data.dao.AIChatMessageDao
import com.reps.app.core.data.datastore.UserPreferencesDataStore
import com.reps.app.core.data.entity.AIChatMessageEntity
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.repository.MealLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val streamedText: String = "",
    val isStreaming: Boolean = false,
    val isAiAvailable: Boolean = true
)

@HiltViewModel
class AICoachViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val aiChatMessageDao: AIChatMessageDao,
    private val userPrefs: UserPreferencesDataStore,
    private val mealLogRepository: MealLogRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    private val _streamedText = MutableStateFlow("")
    private val _isStreaming = MutableStateFlow(false)

    val uiState: StateFlow<ChatUiState> = combine(
        aiChatMessageDao.getAll(),
        _inputText,
        _streamedText,
        _isStreaming
    ) { entities, input, streamed, streaming ->
        ChatUiState(
            messages = entities.map { ChatMessage(it.content, it.isFromUser, it.timestamp) },
            inputText = input,
            streamedText = streamed,
            isStreaming = streaming,
            isAiAvailable = aiRepository.isAvailable()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ChatUiState())

    fun onInputChange(text: String) { _inputText.update { text } }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank() || _isStreaming.value) return

        viewModelScope.launch {
            // Read last 8 messages before inserting so the current turn isn't included in history
            val history = withContext(ioDispatcher) {
                aiChatMessageDao.getRecent(8).reversed()
                    .map { ChatMessage(it.content, it.isFromUser, it.timestamp) }
            }

            withContext(ioDispatcher) {
                aiChatMessageDao.insert(AIChatMessageEntity(content = text, isFromUser = true))
            }
            _inputText.value = ""
            _isStreaming.value = true
            _streamedText.value = ""

            val userContext = buildUserContext()

            var fullResponse = ""
            try {
                aiRepository.getChatResponseStream(history, text, userContext)
                    .collect { token ->
                        fullResponse += token
                        _streamedText.value = fullResponse
                    }
            } finally {
                if (fullResponse.isNotBlank()) {
                    withContext(ioDispatcher) {
                        aiChatMessageDao.insert(AIChatMessageEntity(content = fullResponse, isFromUser = false))
                    }
                }
                _streamedText.value = ""
                _isStreaming.value = false
            }
        }
    }

    fun sendSuggestedPrompt(prompt: String) {
        _inputText.value = prompt
        sendMessage()
    }

    fun clearChat() {
        viewModelScope.launch { aiChatMessageDao.clearAll() }
    }

    private suspend fun buildUserContext(): String {
        val name = userPrefs.name.first()
        val weight = userPrefs.weightKg.first()
        val target = userPrefs.targetWeightKg.first()
        val restrictions = userPrefs.dietaryRestrictions.first()
        val shoulder = userPrefs.hasShoulderRestriction.first()
        val today = LocalDate.now().toString()
        val dayLog = mealLogRepository.getDayLog(today).first()

        return buildString {
            appendLine("You are Reps, an AI fitness and nutrition coach. Be concise and actionable.")
            appendLine("User profile: name=$name, current=${weight}kg, target=${target}kg,")
            appendLine("dietary restrictions=${restrictions.joinToString()}, cuisine preference=South Indian,")
            appendLine("shoulder restriction=$shoulder.")
            append("Today's macros: ${dayLog.totalCalories.toInt()}kcal, ${dayLog.totalProtein.toInt()}g protein.")
        }
    }
}
