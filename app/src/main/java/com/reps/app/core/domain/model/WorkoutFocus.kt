package com.reps.app.core.domain.model

enum class WorkoutFocus(val displayName: String, val muscleKeywords: List<String>) {
    PUSH("Push", listOf("chest", "shoulder", "tricep")),
    PULL("Pull", listOf("back", "bicep", "lat")),
    LEGS("Legs", listOf("quad", "hamstring", "glute", "calf", "leg")),
    FULL_BODY("Full Body", emptyList())
}
