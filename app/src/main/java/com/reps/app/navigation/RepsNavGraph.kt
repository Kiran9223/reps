package com.reps.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.reps.app.feature.ai.AICoachScreen
import com.reps.app.feature.dashboard.DashboardScreen
import com.reps.app.feature.food.BarcodeScannerScreen
import com.reps.app.feature.food.CustomFoodCreationScreen
import com.reps.app.feature.food.FoodDetailScreen
import com.reps.app.feature.food.FoodSearchScreen
import com.reps.app.feature.grocery.GroceryListScreen
import com.reps.app.feature.meal.MealLogScreen
import com.reps.app.feature.more.MoreScreen
import com.reps.app.feature.progress.ProgressScreen
import com.reps.app.feature.settings.SettingsScreen
import com.reps.app.feature.workout.WorkoutLogScreen

@Composable
fun RepsNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(Screen.Dashboard.route) { DashboardScreen() }
        composable(Screen.MealLog.route) { MealLogScreen() }
        composable(Screen.WorkoutLog.route) { WorkoutLogScreen() }
        composable(Screen.Progress.route) { ProgressScreen() }
        composable(Screen.More.route) {
            MoreScreen(
                onNavigateToAICoach = { navController.navigate(Screen.AICoach.route) },
                onNavigateToGrocery = { navController.navigate(Screen.GroceryList.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.FoodSearch.route) {
            FoodSearchScreen(
                onFoodClick = { foodId ->
                    navController.navigate(Screen.FoodDetail.createRoute(foodId))
                },
                onBarcodeClick = { navController.navigate(Screen.BarcodeScanner.route) },
                onCustomFoodClick = { navController.navigate(Screen.CustomFoodCreation.route) }
            )
        }
        composable(
            route = Screen.FoodDetail.route,
            arguments = listOf(navArgument("foodId") { type = NavType.LongType })
        ) {
            FoodDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.BarcodeScanner.route) {
            BarcodeScannerScreen(
                onFoodFound = { foodId ->
                    navController.navigate(Screen.FoodDetail.createRoute(foodId)) {
                        popUpTo(Screen.BarcodeScanner.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CustomFoodCreation.route) {
            CustomFoodCreationScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.GroceryList.route) { GroceryListScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
        composable(Screen.AICoach.route) { AICoachScreen() }
    }
}
