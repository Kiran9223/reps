package com.reps.app.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object MealLog : Screen("meal_log")
    object FoodSearch : Screen("food_search")
    object WorkoutLog : Screen("workout_log")
    object Progress : Screen("progress")
    object GroceryList : Screen("grocery_list")
    object Settings : Screen("settings")
    object AICoach : Screen("ai_coach")
    object More : Screen("more")
}
