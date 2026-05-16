package com.reps.app.core.domain.model

data class BodyMeasurement(
    val id: Long,
    val date: Long,
    val waistCm: Double?,
    val chestCm: Double?,
    val armsCm: Double?,
    val thighsCm: Double?,
    val notes: String?,
    val waistDelta: Double? = null,
    val chestDelta: Double? = null,
    val armsDelta: Double? = null,
    val thighsDelta: Double? = null
) {
    val hasAnyDecrease: Boolean
        get() = listOfNotNull(waistDelta, chestDelta, armsDelta, thighsDelta).any { it < 0 }
}
