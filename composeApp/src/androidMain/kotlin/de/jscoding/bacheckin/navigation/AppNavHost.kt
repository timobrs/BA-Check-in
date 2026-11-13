package de.jscoding.bacheckin.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import de.jscoding.bacheckin.feature.activation.ActivationViewModel
import de.jscoding.bacheckin.feature.activation.activation1Code.Activation1CodeRoute
import de.jscoding.bacheckin.feature.activation.activation2PinSelect.Activation2PinSelectRoute
import de.jscoding.bacheckin.feature.activation.activation3PinValidate.Activation3PinValidateRoute
import de.jscoding.bacheckin.feature.activation.activation4BiometricSelect.Activation4BiometricSelectRoute
import de.jscoding.bacheckin.feature.home.HomeRoute
import de.jscoding.bacheckin.feature.launch.LaunchRoute
import de.jscoding.bacheckin.feature.revoked.RevokedDeviceRoute
import de.jscoding.bacheckin.feature.settings.SettingsRoute
import de.jscoding.bacheckin.feature.unlock.UnlockRoute

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = appNavigator.navController,
        startDestination = AppRoute.Launch(),
        modifier = modifier
    ) {
        composable<AppRoute.Launch> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.Launch>()
            LaunchRoute(
                code = route.code,
                onNeedsActivation = { code ->
                    appNavigator.navigateAndClear(
                        route = AppRoute.ActivationGraph(code),
                        popUpTo = AppRoute.Launch::class
                    )
                },
                onNeedsUnlock = { showWarning ->
                    appNavigator.navigateAndClear(
                        route = AppRoute.Unlock(showAlreadyActivatedWarning = showWarning),
                        popUpTo = AppRoute.Launch::class
                    )
                },
                onDeviceRevoked = {
                    appNavigator.navigateAndClear(
                        route = AppRoute.RevokedDevice,
                        popUpTo = AppRoute.Launch::class
                    )
                }
            )
        }

        navigation<AppRoute.ActivationGraph>(
            startDestination = AppRoute.Activation1Code()
        ) {
            composable<AppRoute.Activation1Code> { backStackEntry ->
                val currentRoute = backStackEntry.toRoute<AppRoute.Activation1Code>()
                val parentEntry = remember(backStackEntry) {
                    appNavigator.navController.getBackStackEntry<AppRoute.ActivationGraph>()
                }
                val parentRoute = parentEntry.toRoute<AppRoute.ActivationGraph>()
                val deepLinkCode = currentRoute.code ?: parentRoute.code

                val viewModel: ActivationViewModel = hiltViewModel(parentEntry)

                LaunchedEffect(deepLinkCode) {
                    deepLinkCode?.let { code ->
                        viewModel.onActivationCodeChange(code)
                    }
                }

                Activation1CodeRoute(
                    viewModel = viewModel,
                    onNext = { appNavigator.navigate(AppRoute.Activation2PinSelect) },
                    onCancel = {
                        appNavigator.navigateAndClearBackStack(AppRoute.ActivationGraph())
                    }
                )
            }

            composable<AppRoute.Activation2PinSelect>(
                enterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { it })
                },
                exitTransition = {
                    slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { -it })
                },
                popEnterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { -it })
                },
                popExitTransition = {
                    slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { it })
                }
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    appNavigator.navController.getBackStackEntry<AppRoute.ActivationGraph>()
                }
                val viewModel: ActivationViewModel = hiltViewModel(parentEntry)

                Activation2PinSelectRoute(
                    viewModel = viewModel,
                    onNext = { appNavigator.navigate(AppRoute.Activation3PinValidate) },
                    onBack = { appNavigator.back() },
                    onCancel = {
                        appNavigator.navigateAndClearBackStack(AppRoute.ActivationGraph())
                    }
                )
            }

            composable<AppRoute.Activation3PinValidate>(
                enterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { it })
                },
                exitTransition = {
                    slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { -it })
                },
                popEnterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { -it })
                },
                popExitTransition = {
                    slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { it })
                }
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    appNavigator.navController.getBackStackEntry<AppRoute.ActivationGraph>()
                }
                val viewModel: ActivationViewModel = hiltViewModel(parentEntry)

                Activation3PinValidateRoute(
                    viewModel = viewModel,
                    onNext = { appNavigator.navigate(AppRoute.Activation4BiometricSelect) },
                    onBack = { appNavigator.back() },
                    onCancel = {
                        appNavigator.navigateAndClearBackStack(AppRoute.ActivationGraph())
                    }
                )
            }

            composable<AppRoute.Activation4BiometricSelect>(
                enterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { it })
                },
                exitTransition = {
                    slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { -it })
                },
                popEnterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { -it })
                },
                popExitTransition = {
                    slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { it })
                }
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    appNavigator.navController.getBackStackEntry<AppRoute.ActivationGraph>()
                }
                val viewModel: ActivationViewModel = hiltViewModel(parentEntry)

                Activation4BiometricSelectRoute(
                    viewModel = viewModel,
                    onSubmit = {
                        appNavigator.navigateAndClear(
                            route = AppRoute.Unlock(),
                            popUpTo = AppRoute.ActivationGraph::class
                        )
                    },
                    onBack = { appNavigator.back() },
                    onCancel = {
                        appNavigator.navigateAndClearBackStack(AppRoute.ActivationGraph())
                    }
                )
            }
        }

        composable<AppRoute.Unlock> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.Unlock>()
            UnlockRoute(
                showAlreadyActivatedWarning = route.showAlreadyActivatedWarning,
                onUnlockSucceeded = {
                    appNavigator.navigateAndClear(
                        route = AppRoute.Home,
                        popUpTo = AppRoute.Unlock::class
                    )
                },
                onDeviceRevoked = {
                    appNavigator.navigateAndClear(
                        route = AppRoute.RevokedDevice,
                        popUpTo = AppRoute.Unlock::class
                    )
                }
            )
        }

        composable<AppRoute.RevokedDevice> {
            RevokedDeviceRoute(
                onResetFinished = {
                    appNavigator.navigateAndClearBackStack(AppRoute.ActivationGraph())
                }
            )
        }

        composable<AppRoute.Home>(
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(300),
                    targetOffsetX = { -it })
            },
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { -it })
            }
        ) {
            HomeRoute(
                onOpenSettings = { appNavigator.navigate(AppRoute.Settings) },
                onSessionExpired = {
                    appNavigator.navigateAndClear(
                        route = AppRoute.Unlock(),
                        popUpTo = AppRoute.Home::class
                    )
                }
            )
        }

        composable<AppRoute.Settings>(
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { it })
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(300),
                    targetOffsetX = { it })
            }
        ) {
            SettingsRoute(
                onBack = { appNavigator.back() },
                onDeviceRemoved = {
                    appNavigator.navigateAndClearBackStack(AppRoute.ActivationGraph())
                }
            )
        }
    }
}
