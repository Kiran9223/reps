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
import com.reps.app.feature.meal.NaturalLanguageEntryScreen
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
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToFoodSearch = { date, slot ->
                    navController.navigate(Screen.FoodSearch.createRoute(date, slot))
                },
                onNavigateToBarcode = { date, slot ->
                    navController.navigate(Screen.BarcodeScanner.createRoute(date, slot))
                },
                onNavigateToNaturalLanguage = { date, slot ->
                    navController.navigate(Screen.NaturalLanguageEntry.createRoute(date, slot))
                }
            )
        }

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

        composable(
            route = Screen.FoodSearch.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; defaultValue = "" },
                navArgument("slot") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date").orEmpty()
            val slot = backStackEntry.arguments?.getString("slot").orEmpty()
            FoodSearchScreen(
                onFoodClick = { foodId ->
                    navController.navigate(
                        Screen.FoodDetail.createRoute(
                            foodId,
                            date.takeIf { it.isNotBlank() },
                            slot.takeIf { it.isNotBlank() }
                        )
                    )
                },
                onBarcodeClick = {
                    navController.navigate(
                        Screen.BarcodeScanner.createRoute(
                            date.takeIf { it.isNotBlank() },
                            slot.takeIf { it.isNotBlank() }
                        )
                    )
                },
                onCustomFoodClick = { navController.navigate(Screen.CustomFoodCreation.route) }
            )
        }

        composable(
            route = Screen.FoodDetail.route,
            arguments = listOf(
                navArgument("foodId") { type = NavType.LongType },
                navArgument("date") { type = NavType.StringType; defaultValue = "" },
                navArgument("slot") { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            FoodDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.BarcodeScanner.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; defaultValue = "" },
                navArgument("slot") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date").orEmpty()
            val slot = backStackEntry.arguments?.getString("slot").orEmpty()
            BarcodeScannerScreen(
                onFoodFound = { foodId ->
                    navController.navigate(
                        Screen.FoodDetail.createRoute(
                            foodId,
                            date.takeIf { it.isNotBlank() },
                            slot.takeIf { it.isNotBlank() }
                        )
                    ) {
                        popUpTo(Screen.BarcodeScanner.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomFoodCreation.route) {
            CustomFoodCreationScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.NaturalLanguageEntry.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType },
                navArgument("slot") { type = NavType.StringType }
            )
        ) {
            NaturalLanguageEntryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSearch = {
                    navController.popBackStack()
                    // Slot context is already in back stack (FoodSearch was navigated before NL)
                }
            )
        }

        composable(Screen.GroceryList.route) { GroceryListScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
        composable(Screen.AICoach.route) { AICoachScreen() }
    }
}
