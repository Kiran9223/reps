package com.reps.app.core.domain.model

data class WeightCheckIn(
    val id: Long,
    val date: Long,
    val weightKg: Double,
    val notes: String?,
    val deltaKg: Double? = null
)
