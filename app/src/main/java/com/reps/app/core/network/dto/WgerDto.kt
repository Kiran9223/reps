package com.reps.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WgerPagedResponse<T>(
    val count: Int = 0,
    val next: String? = null,
    val results: List<T> = emptyList()
)

@Serializable
data class WgerExerciseInfoDto(
    val id: Int = 0,
    val uuid: String = "",
    val category: WgerCategoryDto? = null,
    val muscles: List<WgerMuscleDto> = emptyList(),
    @SerialName("muscles_secondary") val musclesSecondary: List<WgerMuscleDto> = emptyList(),
    val equipment: List<WgerEquipmentDto> = emptyList(),
    val translations: List<WgerTranslationDto> = emptyList()
)

@Serializable
data class WgerCategoryDto(
    val id: Int = 0,
    val name: String = ""
)

@Serializable
data class WgerMuscleDto(
    val id: Int = 0,
    @SerialName("name_en") val nameEn: String = "",
    @SerialName("is_front") val isFront: Boolean = false
)

@Serializable
data class WgerEquipmentDto(
    val id: Int = 0,
    val name: String = ""
)

@Serializable
data class WgerTranslationDto(
    val id: Int = 0,
    val uuid: String = "",
    val name: String = "",
    val description: String = "",
    val language: Int = 0
)

@Serializable
data class WgerSearchResponse(
    val suggestions: List<WgerSuggestion> = emptyList()
)

@Serializable
data class WgerSuggestion(
    val value: String = "",
    val data: WgerSuggestionData = WgerSuggestionData()
)

@Serializable
data class WgerSuggestionData(
    val id: Int = 0,
    @SerialName("base_id") val baseId: Int = 0,
    val name: String = ""
)
