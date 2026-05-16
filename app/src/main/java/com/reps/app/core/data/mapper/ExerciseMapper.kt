package com.reps.app.core.data.mapper

import com.reps.app.core.data.entity.ExerciseEntity
import com.reps.app.core.domain.model.Exercise
import com.reps.app.core.network.dto.ExerciseSeedDto
import com.reps.app.core.network.dto.WgerExerciseInfoDto

fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    name = name,
    description = description,
    muscleGroups = muscleGroups,
    equipment = equipment,
    isShoulderSafe = isShoulderSafe,
    restrictedMovements = restrictedMovements,
    instructions = instructions,
    source = source,
    isCustom = isCustom
)

fun ExerciseSeedDto.toEntity(): ExerciseEntity = ExerciseEntity(
    name = name,
    description = description,
    muscleGroups = muscleGroups,
    equipment = equipment,
    isShoulderSafe = isShoulderSafe,
    restrictedMovements = restrictedMovements,
    instructions = instructions,
    source = "LOCAL"
)

fun WgerExerciseInfoDto.toEntity(): ExerciseEntity {
    val englishName = translations.firstOrNull { it.language == 2 }?.name ?: return ExerciseEntity(
        name = "Exercise ${id}",
        muscleGroups = emptyList(),
        equipment = emptyList(),
        isShoulderSafe = true,
        restrictedMovements = emptyList(),
        source = "WGER",
        externalId = id.toString()
    )
    val muscleNames = (muscles + musclesSecondary).map { it.nameEn }.distinct()
    val equipmentNames = equipment.map { it.name }
    val description = translations.firstOrNull { it.language == 2 }?.description

    val hasShoulder = muscleNames.any { m ->
        m.contains("shoulder", ignoreCase = true) || m.contains("deltoid", ignoreCase = true)
    }
    val isBarbell = equipmentNames.any { it.contains("barbell", ignoreCase = true) }
    val isOverheadCategory = category?.name?.contains("shoulder", ignoreCase = true) == true

    val restrictedMovements = mutableListOf<String>()
    val isShoulderSafe = when {
        hasShoulder -> {
            restrictedMovements.add("overhead")
            false
        }
        isBarbell && isOverheadCategory -> {
            restrictedMovements.add("overhead")
            false
        }
        else -> true
    }

    return ExerciseEntity(
        name = englishName,
        description = description?.takeIf { it.isNotBlank() },
        muscleGroups = muscleNames,
        equipment = equipmentNames,
        isShoulderSafe = isShoulderSafe,
        restrictedMovements = restrictedMovements,
        source = "WGER",
        externalId = id.toString()
    )
}
