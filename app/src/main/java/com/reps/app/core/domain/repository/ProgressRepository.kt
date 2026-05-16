package com.reps.app.core.domain.repository

import com.reps.app.core.domain.model.BodyMeasurement
import com.reps.app.core.domain.model.Streaks
import com.reps.app.core.domain.model.WeeklyStats
import com.reps.app.core.domain.model.WeightCheckIn
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    suspend fun logWeight(date: Long, weightKg: Double, notes: String?)
    fun getWeightHistory(): Flow<List<WeightCheckIn>>

    suspend fun logMeasurements(
        date: Long,
        waistCm: Double?,
        chestCm: Double?,
        armsCm: Double?,
        thighsCm: Double?,
        notes: String?
    )
    fun getMeasurementHistory(): Flow<List<BodyMeasurement>>

    fun getStreaks(): Flow<Streaks>
    fun getWeeklyStats(): Flow<WeeklyStats>
}
