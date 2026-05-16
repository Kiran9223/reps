package com.reps.app.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object MealLog : Screen("meal_log")

    object FoodSearch : Screen("food_search?date={date}&slot={slot}&planDay={planDay}") {
        fun createRoute(date: String? = null, slot: String? = null, planDay: Int = -1) =
            "food_search?date=${date.orEmpty()}&slot=${slot.orEmpty()}&planDay=$planDay"
    }

    object FoodDetail : Screen("food_detail/{foodId}?date={date}&slot={slot}&planDay={planDay}") {
        fun createRoute(foodId: Long, date: String? = null, slot: String? = null, planDay: Int = -1) =
            "food_detail/$foodId?date=${date.orEmpty()}&slot=${slot.orEmpty()}&planDay=$planDay"
    }

    object BarcodeScanner : Screen("barcode_scanner?date={date}&slot={slot}") {
        fun createRoute(date: String? = null, slot: String? = null) =
            "barcode_scanner?date=${date.orEmpty()}&slot=${slot.orEmpty()}"
    }

    object CreateMealPlan : Screen("create_meal_plan?templateId={templateId}") {
        fun createRoute(templateId: Long? = null) = "create_meal_plan?templateId=${templateId ?: -1L}"
    }

    object CustomFoodCreation : Screen("custom_food_creation")

    object NaturalLanguageEntry : Screen("natural_language_entry/{date}/{slot}") {
        fun createRoute(date: String, slot: String) = "natural_language_entry/$date/$slot"
    }

    object WorkoutLog : Screen("workout_log")
    object ActiveWorkout : Screen("active_workout/{workoutLogId}") {
        fun createRoute(workoutLogId: Long) = "active_workout/$workoutLogId"
    }
    object CreateWorkoutTemplate : Screen("create_workout_template?templateId={templateId}") {
        fun createRoute(templateId: Long? = null) = "create_workout_template?templateId=${templateId ?: -1L}"
    }
    object ExerciseLibrary : Screen("exercise_library?pickMode={pickMode}") {
        fun createRoute(pickMode: Boolean = false) = "exercise_library?pickMode=$pickMode"
    }
    object Progress : Screen("progress")
    object BodyMeasurements : Screen("body_measurements")
    object GroceryList : Screen("grocery_list")
    object MealPlan : Screen("meal_plan")
    object BatchCook : Screen("batch_cook")
    object Settings : Screen("settings")
    object AICoach : Screen("ai_coach")
    object More : Screen("more")
}
