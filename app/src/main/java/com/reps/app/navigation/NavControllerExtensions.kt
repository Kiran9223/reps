package com.reps.app.navigation

import androidx.navigation.NavHostController

/**
 * Pops nested screens until the current tab's top-level destination is visible.
 * Avoids losing context when switching bottom-nav tabs from a deep link (e.g. food search).
 */
fun NavHostController.popToTopLevelDestination() {
    while (true) {
        val route = currentBackStackEntry?.destination?.route
        if (route == null || isTopLevelDestination(route)) break
        if (!popBackStack()) break
    }
}
