package com.reps.app.ai

import com.reps.app.core.domain.model.DayLog
import com.reps.app.core.domain.model.Exercise
import com.reps.app.core.domain.model.MacroTargets
import com.reps.app.core.domain.model.MealSlot
import com.reps.app.core.domain.model.TemplateExerciseDraft
import com.reps.app.core.domain.model.WorkoutFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RuleBasedAIRepository : AIRepository {

    override suspend fun parseNaturalLanguageMeal(input: String): Result<List<ParsedMealToken>> =
        Result.failure(UnsupportedOperationException("AI model not available"))

    override suspend fun getDailyInsight(
        dayLog: DayLog,
        targets: MacroTargets,
        currentWeightKg: Double,
        targetWeightKg: Double,
        workoutDone: Boolean,
        waterMl: Int
    ): Result<String> {
        val proteinPct = if (targets.proteinG > 0) dayLog.totalProtein / targets.proteinG else 0.0
        val calPct = if (targets.calories > 0) dayLog.totalCalories / targets.calories else 0.0
        val message = when {
            proteinPct >= 0.9 && workoutDone ->
                "Great work hitting your protein goal and completing a workout today! Keep this momentum going."
            proteinPct >= 0.9 ->
                "You're crushing your protein goal! Add a workout tomorrow to accelerate your progress."
            calPct > 1.1 ->
                "Calories are a bit high today — a lighter dinner will help you stay on track."
            waterMl < targets.waterMl / 2 ->
                "Don't forget to hydrate! Aim for ${targets.waterMl}ml of water today."
            else ->
                "Every meal on target gets you closer to your goal. Stay consistent!"
        }
        return Result.success(message)
    }

    override suspend fun getMealSuggestion(
        mealSlot: MealSlot,
        remainingProteinG: Double,
        availableFoods: List<String>
    ): Result<AiMealSuggestion> {
        val best = availableFoods.firstOrNull()
            ?: return Result.failure(Exception("No foods available"))
        return Result.success(AiMealSuggestion(foodName = best, servings = 1.0, reason = "Good protein source"))
    }

    override fun getChatResponseStream(
        history: List<ChatMessage>,
        userMessage: String,
        userContext: String
    ): Flow<String> = flow {
        emit(
            "AI Coach requires cloud assist. Add GEMINI_API_KEY to local.properties, rebuild the app, " +
                "then open Settings → Privacy & data for details."
        )
    }

    override suspend fun getQuickWorkoutExercises(
        focus: WorkoutFocus,
        timeBudgetMinutes: Int,
        availableExercises: List<Exercise>
    ): Result<List<TemplateExerciseDraft>> {
        val count = when {
            timeBudgetMinutes <= 20 -> 3
            timeBudgetMinutes <= 35 -> 5
            else -> 7
        }
        val sets = if (timeBudgetMinutes >= 45) 4 else 3
        val filtered = if (focus.muscleKeywords.isEmpty()) {
            availableExercises
        } else {
            availableExercises.filter { ex ->
                focus.muscleKeywords.any { kw ->
                    ex.muscleGroups.any { mg -> mg.contains(kw, ignoreCase = true) }
                }
            }
        }
        val selected = filtered.shuffled().take(count)
        return Result.success(
            selected.map { ex ->
                TemplateExerciseDraft(exerciseId = ex.id, targetSets = sets, targetReps = "8-12")
            }
        )
    }

    override suspend fun getShoulderSafeAlternatives(
        exerciseName: String,
        muscleGroup: String
    ): Result<List<ExerciseAlternative>> = Result.success(
        listOf(
            ExerciseAlternative("Cable Row", "Works $muscleGroup without overhead stress"),
            ExerciseAlternative("Dumbbell Row", "Shoulder-safe horizontal pulling movement")
        )
    )

    override suspend fun parseWorkoutPlanText(text: String): Result<ParsedWorkoutPlan> =
        Result.failure(UnsupportedOperationException("AI model not available"))

    override suspend fun parseMealPlanText(text: String): Result<ParsedMealPlan> =
        Result.failure(UnsupportedOperationException("AI model not available"))

    override suspend fun estimateNutrition(foodDescription: String): Result<EstimatedNutrition> {
        val n = foodDescription.lowercase()
        val countMatch = Regex("^(\\d+)").find(n.trim())
        val count = countMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 1.0

        data class MacroRow(val cal: Double, val pro: Double, val car: Double, val fat: Double, val fib: Double, val g: Double)

        val row = when {
            n.contains("poori") || n.contains("puri") -> MacroRow(135.0, 2.0, 18.0, 6.0, 1.0, 45.0)
            n.contains("idli") -> MacroRow(39.0, 2.0, 8.0, 0.4, 0.5, 30.0)
            n.contains("dosa") -> MacroRow(120.0, 3.0, 22.0, 3.0, 1.0, 80.0)
            n.contains("roti") || n.contains("chapati") -> MacroRow(70.0, 3.0, 14.0, 1.0, 2.0, 35.0)
            n.contains("rice") || n.contains("chawal") -> MacroRow(130.0, 2.7, 28.0, 0.3, 0.4, 75.0)
            n.contains("sambar") -> MacroRow(50.0, 3.0, 7.0, 1.5, 2.0, 100.0)
            n.contains("egg") -> MacroRow(78.0, 6.0, 0.6, 5.0, 0.0, 50.0)
            n.contains("chicken") -> MacroRow(165.0, 31.0, 0.0, 3.6, 0.0, 100.0)
            n.contains("paratha") -> MacroRow(180.0, 4.0, 24.0, 8.0, 2.0, 70.0)
            n.contains("upma") -> MacroRow(150.0, 4.0, 24.0, 5.0, 1.5, 120.0)
            n.contains("vada") || n.contains("wada") -> MacroRow(100.0, 3.0, 12.0, 5.0, 1.0, 40.0)
            else -> MacroRow(150.0, 5.0, 20.0, 5.0, 2.0, 100.0)
        }

        return Result.success(
            EstimatedNutrition(
                calories = row.cal * count,
                proteinG = row.pro * count,
                carbsG = row.car * count,
                fatG = row.fat * count,
                fiberG = row.fib * count,
                servingGrams = row.g * count
            )
        )
    }

    override suspend fun categorizeGroceryItem(itemName: String): Result<String> {
        val n = itemName.lowercase()
        val category = when {
            n.contains("chicken") || n.contains("egg") || n.contains("fish") ||
            n.contains("paneer") || n.contains("soya") || n.contains("tofu") ||
            n.contains("whey") || n.contains("tuna") || n.contains("turkey") ||
            n.contains("mutton") || n.contains("prawn") || n.contains("dal") ||
            n.contains("lentil") || n.contains("bean") || n.contains("meat") -> "PROTEIN"

            n.contains("milk") || n.contains("curd") || n.contains("yogurt") ||
            n.contains("butter") || n.contains("ghee") || n.contains("cream") ||
            n.contains("cheese") || n.contains("lassi") || n.contains("paneer") -> "DAIRY"

            n.contains("spinach") || n.contains("broccoli") || n.contains("tomato") ||
            n.contains("onion") || n.contains("banana") || n.contains("apple") ||
            n.contains("vegetable") || n.contains("salad") || n.contains("fruit") ||
            n.contains("carrot") || n.contains("capsicum") || n.contains("mushroom") ||
            n.contains("potato") || n.contains("cucumber") || n.contains("lettuce") -> "PRODUCE"

            n.contains("frozen") || n.contains("ice cream") -> "FROZEN"

            else -> "PANTRY"
        }
        return Result.success(category)
    }

    override suspend fun estimateExerciseDetails(exerciseName: String): Result<EstimatedExercise> {
        val n = exerciseName.lowercase()
        val muscleGroups = when {
            n.contains("squat") || n.contains("leg press") || n.contains("lunge") -> listOf("Quadriceps", "Glutes", "Hamstrings")
            n.contains("deadlift") || n.contains("hip hinge") -> listOf("Hamstrings", "Glutes", "Lower Back")
            n.contains("bench") || n.contains("chest") || n.contains("push up") || n.contains("pushup") || n.contains("fly") -> listOf("Chest", "Triceps", "Shoulders")
            n.contains("row") || n.contains("pull up") || n.contains("pullup") || n.contains("lat") || n.contains("back") -> listOf("Back", "Biceps")
            n.contains("overhead") || n.contains("shoulder press") || n.contains("ohp") -> listOf("Shoulders", "Triceps")
            n.contains("curl") || n.contains("bicep") -> listOf("Biceps")
            n.contains("tricep") || n.contains("extension") || n.contains("pushdown") -> listOf("Triceps")
            n.contains("calf") -> listOf("Calves")
            n.contains("plank") || n.contains("crunch") || n.contains("sit up") || n.contains("ab") -> listOf("Core", "Abs")
            else -> listOf("Full Body")
        }
        val isShoulderSafe = !n.contains("overhead") && !n.contains("military press") &&
            !n.contains("behind neck") && !n.contains("upright row")
        return Result.success(
            EstimatedExercise(
                muscleGroups = muscleGroups,
                equipment = listOf("Varies"),
                isShoulderSafe = isShoulderSafe,
                restrictedMovements = emptyList(),
                description = exerciseName
            )
        )
    }

    override suspend fun estimateWorkoutTemplate(
        name: String,
        availableExercises: List<Exercise>
    ): Result<WorkoutTemplateSuggestion> {
        val n = name.lowercase()
        val muscleKeywords = when {
            n.contains("push") || n.contains("chest") || n.contains("press") ->
                listOf("chest", "tricep", "pectoral", "shoulder")
            n.contains("pull") || n.contains("back") || n.contains("row") || n.contains("lat") ->
                listOf("back", "bicep", "lat")
            n.contains("leg") || n.contains("lower") || n.contains("squat") || n.contains("quad") ->
                listOf("quadricep", "hamstring", "glute", "calf", "leg")
            n.contains("shoulder") || n.contains("delt") ->
                listOf("shoulder", "delt", "tricep")
            n.contains("arm") || n.contains("bicep") || n.contains("tricep") ->
                listOf("bicep", "tricep")
            n.contains("core") || n.contains("abs") ->
                listOf("core", "abs")
            else -> emptyList()
        }
        val description = when {
            n.contains("push") || n.contains("chest") -> "Chest, shoulders, and triceps focused session."
            n.contains("pull") || n.contains("back") -> "Back and biceps focused pulling session."
            n.contains("leg") || n.contains("lower") -> "Legs and glutes focused session."
            n.contains("shoulder") || n.contains("delt") -> "Shoulder-focused pressing and raising session."
            n.contains("arm") -> "Arm isolation — biceps and triceps."
            n.contains("core") || n.contains("abs") -> "Core stability and strength session."
            else -> "Full-body strength training session."
        }
        val filtered = if (muscleKeywords.isEmpty()) availableExercises else {
            availableExercises.filter { ex ->
                muscleKeywords.any { kw ->
                    ex.muscleGroups.any { mg -> mg.contains(kw, ignoreCase = true) }
                }
            }
        }
        val selected = (if (filtered.isEmpty()) availableExercises else filtered).shuffled().take(5)
        return Result.success(
            WorkoutTemplateSuggestion(
                description = description,
                exercises = selected.map { ex ->
                    SuggestedTemplateExercise(
                        exerciseId = ex.id,
                        targetSets = 3,
                        targetReps = "8-12",
                        targetWeightKg = null
                    )
                }
            )
        )
    }

    override fun isAvailable(): Boolean = false
}
