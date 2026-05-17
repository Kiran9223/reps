package com.reps.app.ai

import kotlinx.serialization.Serializable

enum class ImportPlanType { WORKOUT, MEAL }

@Serializable data class ParsedWorkoutPlan(val name: String, val days: List<ParsedWorkoutDay>)
@Serializable data class ParsedWorkoutDay(val label: String, val exercises: List<ParsedExerciseItem>)
@Serializable data class ParsedExerciseItem(val name: String, val sets: Int, val reps: String)

@Serializable data class ParsedMealPlan(val name: String, val days: List<ParsedMealDay>)
@Serializable data class ParsedMealDay(val label: String, val slots: List<ParsedMealSlotItem>)
@Serializable data class ParsedMealSlotItem(val slot: String, val foods: List<ParsedMealToken>)
