package com.reps.app.core.data.mapper

import com.reps.app.core.data.entity.BodyMeasurementEntity
import com.reps.app.core.data.entity.WeightLogEntity
import com.reps.app.core.domain.model.BodyMeasurement
import com.reps.app.core.domain.model.WeightCheckIn

fun WeightLogEntity.toWeightCheckIn(deltaKg: Double? = null) = WeightCheckIn(
    id = id,
    date = date,
    weightKg = weightKg,
    notes = notes,
    deltaKg = deltaKg
)

fun BodyMeasurementEntity.toBodyMeasurement(
    prev: BodyMeasurementEntity? = null
) = BodyMeasurement(
    id = id,
    date = date,
    waistCm = waistCm,
    chestCm = chestCm,
    armsCm = armsCm,
    thighsCm = thighsCm,
    notes = notes,
    waistDelta = if (waistCm != null && prev?.waistCm != null) waistCm - prev.waistCm else null,
    chestDelta = if (chestCm != null && prev?.chestCm != null) chestCm - prev.chestCm else null,
    armsDelta = if (armsCm != null && prev?.armsCm != null) armsCm - prev.armsCm else null,
    thighsDelta = if (thighsCm != null && prev?.thighsCm != null) thighsCm - prev.thighsCm else null
)
