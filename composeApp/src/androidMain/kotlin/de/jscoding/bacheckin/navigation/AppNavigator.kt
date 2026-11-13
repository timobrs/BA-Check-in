package de.jscoding.bacheckin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlin.reflect.KClass

class AppNavigator(
    val navController: NavHostController
) {

    fun navigate(
        route: AppRoute,
        launchSingleTop: Boolean = true
    ) {
        navController.navigate(route) {
            this.launchSingleTop = launchSingleTop
        }
    }

    fun popUpTo(
        route: KClass<out AppRoute>,
        inclusive: Boolean = false
    ) {
        navController.popBackStack(route, inclusive)
    }

    fun navigateAndClear(
        route: AppRoute,
        popUpTo: KClass<out AppRoute>,
        inclusive: Boolean = true
    ) {
        navController.navigate(route) {
            popUpTo(popUpTo) {
                this.inclusive = inclusive
            }
            launchSingleTop = true
        }
    }

    fun navigateAndClearBackStack(
        route: AppRoute
    ) {
        navController.navigate(route) {
            popUpTo(0) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    fun back() {
        navController.popBackStack()
    }
}

@Composable
fun rememberAppNavigator(): AppNavigator {
    val navController = rememberNavController()

    return remember(navController) {
        AppNavigator(navController)
    }
}