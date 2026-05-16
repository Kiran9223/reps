package com.reps.app.core.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.reps.app.core.data.entity.ExerciseEntity
import com.reps.app.core.data.entity.WorkoutTemplateEntity
import com.reps.app.core.data.entity.WorkoutTemplateExerciseEntity

data class WorkoutTemplateWithExercises(
    @Embedded val template: WorkoutTemplateEntity,
    @Relation(
        entity = WorkoutTemplateExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val templateExercises: List<WorkoutTemplateExerciseWithExercise>
)

data class WorkoutTemplateExerciseWithExercise(
    @Embedded val templateExercise: WorkoutTemplateExerciseEntity,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: ExerciseEntity
)
