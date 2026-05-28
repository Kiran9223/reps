package com.reps.app.core.domain

/**
 * Shared profile field rules for onboarding and settings (kept in sync intentionally).
 */
data class ProfileInput(
    val name: String,
    val age: String,
    val weightKg: String,
    val targetWeightKg: String,
    val heightCm: String
)

enum class ProfileValidationError {
    NAME_REQUIRED,
    AGE_INVALID,
    WEIGHT_INVALID,
    TARGET_WEIGHT_INVALID,
    HEIGHT_INVALID
}

fun ProfileInput.validate(): ProfileValidationError? = when {
    name.isBlank() -> ProfileValidationError.NAME_REQUIRED
    age.toIntOrNull()?.let { it > 0 } != true -> ProfileValidationError.AGE_INVALID
    weightKg.toDoubleOrNull()?.let { it > 0 } != true -> ProfileValidationError.WEIGHT_INVALID
    targetWeightKg.toDoubleOrNull()?.let { it > 0 } != true -> ProfileValidationError.TARGET_WEIGHT_INVALID
    heightCm.toDoubleOrNull()?.let { it > 0 } != true -> ProfileValidationError.HEIGHT_INVALID
    else -> null
}

fun ProfileInput.toPersistedValues(): PersistedProfile? {
    if (validate() != null) return null
    return PersistedProfile(
        name = name.trim(),
        age = age.toIntOrNull()!!,
        weightKg = weightKg.toDoubleOrNull()!!,
        targetWeightKg = targetWeightKg.toDoubleOrNull()!!,
        heightCm = heightCm.toDoubleOrNull()!!
    )
}

data class PersistedProfile(
    val name: String,
    val age: Int,
    val weightKg: Double,
    val targetWeightKg: Double,
    val heightCm: Double
)
