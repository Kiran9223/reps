package com.reps.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.reps.app.R

sealed class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val labelResId: Int
) {
    object Dashboard : BottomNavItem(Screen.Dashboard, Icons.Filled.Home, R.string.nav_dashboard)
    object Meals : BottomNavItem(Screen.MealLog, Icons.Filled.Restaurant, R.string.nav_meals)
    object Workout : BottomNavItem(Screen.WorkoutLog, Icons.Filled.FitnessCenter, R.string.nav_workout)
    object Progress : BottomNavItem(Screen.Progress, Icons.Filled.BarChart, R.string.nav_progress)
    object More : BottomNavItem(Screen.More, Icons.Filled.MoreHoriz, R.string.nav_more)
}

val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Meals,
    BottomNavItem.Workout,
    BottomNavItem.Progress,
    BottomNavItem.More
)
