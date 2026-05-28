package com.reps.app.navigation

/**
 * Routes where the main bottom navigation bar should remain visible.
 */
private val topLevelRouteBases = setOf(
    "dashboard",
    "meal_log",
    "workout_log",
    "ai_coach",
    "more"
)

fun isTopLevelDestination(route: String?): Boolean {
    if (route == null) return false
    val base = route.substringBefore("?").substringBefore("/{")
    return topLevelRouteBases.contains(base)
}
