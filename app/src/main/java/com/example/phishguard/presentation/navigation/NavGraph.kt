package com.example.phishguard.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.phishguard.presentation.home.HomeScreen
import com.example.phishguard.presentation.detail.DetailScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Detail : Screen("detail/{threatId}") {
        fun createRoute(threatId: Long) = "detail/$threatId"
    }
}

@Composable
fun PhishGuardNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onThreatClick = { threatId ->
                    navController.navigate(Screen.Detail.createRoute(threatId))
                }
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("threatId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val threatId = backStackEntry.arguments?.getLong("threatId") ?: return@composable
            DetailScreen(
                threatId = threatId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}