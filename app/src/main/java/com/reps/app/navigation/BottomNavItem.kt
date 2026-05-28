package com.reps.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.reps.app.R

sealed class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val labelResId: Int
) {
    object Dashboard : BottomNavItem(Screen.Dashboard, Icons.Filled.Home, R.string.nav_dashboard)
    object Diary : BottomNavItem(Screen.MealLog, Icons.Filled.Restaurant, R.string.nav_diary)
    object Workout : BottomNavItem(Screen.WorkoutLog, Icons.Filled.FitnessCenter, R.string.nav_workout)
    object AICoach : BottomNavItem(Screen.AICoach, Icons.Filled.Psychology, R.string.nav_ai_coach)
    object More : BottomNavItem(Screen.More, Icons.Filled.MoreHoriz, R.string.nav_more)
}

val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Diary,
    BottomNavItem.Workout,
    BottomNavItem.AICoach,
    BottomNavItem.More
)
