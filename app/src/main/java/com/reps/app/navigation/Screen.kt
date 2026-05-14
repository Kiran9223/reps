package com.reps.app.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object MealLog : Screen("meal_log")

    object FoodSearch : Screen("food_search?date={date}&slot={slot}") {
        fun createRoute(date: String? = null, slot: String? = null) =
            "food_search?date=${date.orEmpty()}&slot=${slot.orEmpty()}"
    }

    object FoodDetail : Screen("food_detail/{foodId}?date={date}&slot={slot}") {
        fun createRoute(foodId: Long, date: String? = null, slot: String? = null) =
            "food_detail/$foodId?date=${date.orEmpty()}&slot=${slot.orEmpty()}"
    }

    object BarcodeScanner : Screen("barcode_scanner?date={date}&slot={slot}") {
        fun createRoute(date: String? = null, slot: String? = null) =
            "barcode_scanner?date=${date.orEmpty()}&slot=${slot.orEmpty()}"
    }

    object CustomFoodCreation : Screen("custom_food_creation")

    object NaturalLanguageEntry : Screen("natural_language_entry/{date}/{slot}") {
        fun createRoute(date: String, slot: String) = "natural_language_entry/$date/$slot"
    }

    object WorkoutLog : Screen("workout_log")
    object Progress : Screen("progress")
    object GroceryList : Screen("grocery_list")
    object Settings : Screen("settings")
    object AICoach : Screen("ai_coach")
    object More : Screen("more")
}
