package com.reps.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.reps.app.feature.meal.BatchCookScreen
import com.reps.app.feature.meal.CreateMealPlanScreen
import com.reps.app.feature.meal.MealLogScreen
import com.reps.app.feature.meal.MealPlanScreen
import com.reps.app.feature.meal.NaturalLanguageEntryScreen
import com.reps.app.feature.more.MoreScreen
import com.reps.app.feature.progress.BodyMeasurementsScreen
import com.reps.app.feature.progress.ProgressScreen
import com.reps.app.feature.settings.SettingsScreen
import com.reps.app.feature.workout.ActiveWorkoutScreen
import com.reps.app.feature.workout.CreateWorkoutTemplateScreen
import com.reps.app.feature.workout.ExerciseLibraryScreen
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
                },
                onNavigateToProgress = { navController.navigate(Screen.Progress.route) }
            )
        }

        composable(Screen.MealLog.route) { MealLogScreen() }

        composable(Screen.WorkoutLog.route) {
            WorkoutLogScreen(
                onNavigateToActiveWorkout = { workoutLogId ->
                    navController.navigate(Screen.ActiveWorkout.createRoute(workoutLogId))
                },
                onNavigateToExerciseLibrary = {
                    navController.navigate(Screen.ExerciseLibrary.createRoute())
                },
                onNavigateToCreateTemplate = {
                    navController.navigate(Screen.CreateWorkoutTemplate.createRoute())
                },
                onNavigateToEditTemplate = { templateId ->
                    navController.navigate(Screen.CreateWorkoutTemplate.createRoute(templateId))
                }
            )
        }

        composable(
            route = Screen.ActiveWorkout.route,
            arguments = listOf(
                navArgument("workoutLogId") { type = NavType.LongType }
            )
        ) {
            ActiveWorkoutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CreateWorkoutTemplate.route,
            arguments = listOf(
                navArgument("templateId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val sh = backStackEntry.savedStateHandle
            val pendingExerciseId by sh.getStateFlow("picked_exercise_id", -1L).collectAsStateWithLifecycle()
            val pendingExerciseName by sh.getStateFlow("picked_exercise_name", "").collectAsStateWithLifecycle()

            CreateWorkoutTemplateScreen(
                pendingExerciseId = pendingExerciseId,
                pendingExerciseName = pendingExerciseName,
                onPendingConsumed = { sh["picked_exercise_id"] = -1L },
                onNavigateToExercisePicker = {
                    navController.navigate(Screen.ExerciseLibrary.createRoute(pickMode = true))
                },
                onSaved = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ExerciseLibrary.route,
            arguments = listOf(
                navArgument("pickMode") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val pickMode = backStackEntry.arguments?.getBoolean("pickMode") ?: false
            ExerciseLibraryScreen(
                onNavigateBack = { navController.popBackStack() },
                onExercisePicked = if (pickMode) { exerciseId, exerciseName ->
                    try {
                        val entry = navController.getBackStackEntry(Screen.CreateWorkoutTemplate.route)
                        entry.savedStateHandle["picked_exercise_id"] = exerciseId
                        entry.savedStateHandle["picked_exercise_name"] = exerciseName
                        navController.popBackStack(Screen.CreateWorkoutTemplate.route, inclusive = false)
                    } catch (_: Exception) {
                        navController.popBackStack()
                    }
                } else null
            )
        }

        composable(Screen.Progress.route) {
            ProgressScreen(
                onNavigateToBodyMeasurements = {
                    navController.navigate(Screen.BodyMeasurements.route)
                }
            )
        }

        composable(Screen.BodyMeasurements.route) {
            BodyMeasurementsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.More.route) {
            MoreScreen(
                onNavigateToAICoach = { navController.navigate(Screen.AICoach.route) },
                onNavigateToMealPlan = { navController.navigate(Screen.MealPlan.route) },
                onNavigateToGrocery = { navController.navigate(Screen.GroceryList.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.FoodSearch.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; defaultValue = "" },
                navArgument("slot") { type = NavType.StringType; defaultValue = "" },
                navArgument("planDay") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date").orEmpty()
            val slot = backStackEntry.arguments?.getString("slot").orEmpty()
            val planDay = backStackEntry.arguments?.getInt("planDay") ?: -1
            FoodSearchScreen(
                onFoodClick = { foodId ->
                    navController.navigate(
                        Screen.FoodDetail.createRoute(
                            foodId,
                            date.takeIf { it.isNotBlank() },
                            slot.takeIf { it.isNotBlank() },
                            planDay
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
                navArgument("slot") { type = NavType.StringType; defaultValue = "" },
                navArgument("planDay") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val planDay = backStackEntry.arguments?.getInt("planDay") ?: -1
            val slot = backStackEntry.arguments?.getString("slot").orEmpty()
            FoodDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddToPlan = if (planDay >= 0) { foodId, foodName, servings ->
                    try {
                        val planEntry = navController.getBackStackEntry(Screen.CreateMealPlan.route)
                        planEntry.savedStateHandle["plan_food_id"] = foodId
                        planEntry.savedStateHandle["plan_food_name"] = foodName
                        planEntry.savedStateHandle["plan_food_servings"] = servings
                        planEntry.savedStateHandle["plan_food_day"] = planDay
                        planEntry.savedStateHandle["plan_food_slot"] = slot
                        navController.popBackStack(Screen.CreateMealPlan.route, inclusive = false)
                    } catch (e: Exception) {
                        navController.popBackStack()
                    }
                } else null
            )
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
                onNavigateToSearch = { navController.popBackStack() }
            )
        }

        composable(Screen.MealPlan.route) {
            MealPlanScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreatePlan = { navController.navigate(Screen.CreateMealPlan.createRoute()) },
                onNavigateToEditPlan = { templateId ->
                    navController.navigate(Screen.CreateMealPlan.createRoute(templateId))
                }
            )
        }

        composable(
            route = Screen.CreateMealPlan.route,
            arguments = listOf(
                navArgument("templateId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val sh = backStackEntry.savedStateHandle
            val pendingFoodId by sh.getStateFlow("plan_food_id", -1L).collectAsStateWithLifecycle()
            val pendingFoodName by sh.getStateFlow("plan_food_name", "").collectAsStateWithLifecycle()
            val pendingServings by sh.getStateFlow("plan_food_servings", 1.0).collectAsStateWithLifecycle()
            val pendingDayIndex by sh.getStateFlow("plan_food_day", -1).collectAsStateWithLifecycle()
            val pendingSlotName by sh.getStateFlow("plan_food_slot", "").collectAsStateWithLifecycle()

            CreateMealPlanScreen(
                pendingFoodId = pendingFoodId,
                pendingFoodName = pendingFoodName,
                pendingServings = pendingServings,
                pendingDayIndex = pendingDayIndex,
                pendingSlotName = pendingSlotName,
                onPendingConsumed = { sh["plan_food_id"] = -1L },
                onNavigateToFoodSearch = { dayIndex, slot ->
                    navController.navigate(
                        Screen.FoodSearch.createRoute(planDay = dayIndex, slot = slot.name)
                    )
                },
                onSaved = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BatchCook.route) {
            BatchCookScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.GroceryList.route) {
            GroceryListScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.AICoach.route) {
            AICoachScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
