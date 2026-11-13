package de.jscoding.bacheckin

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jscoding.bacheckin.core.ui.theme.AppTheme
import de.jscoding.bacheckin.core.ui.theme.AppThemeViewModel
import de.jscoding.bacheckin.navigation.AppNavHost
import de.jscoding.bacheckin.navigation.AppRoute
import de.jscoding.bacheckin.navigation.rememberAppNavigator

@Composable
fun AndroidApp(
    deepLinkIntent: Intent? = null,
    appThemeViewModel: AppThemeViewModel = hiltViewModel()
) {
    val settings by appThemeViewModel.settings.collectAsStateWithLifecycle()
    val appNavigator = rememberAppNavigator()

    LaunchedEffect(deepLinkIntent) {
        if (deepLinkIntent?.action == Intent.ACTION_VIEW) {
            deepLinkIntent.data?.let { uri ->
                val code = uri.getQueryParameter("code")
                if (code != null) {
                    appNavigator.navigateAndClearBackStack(
                        route = AppRoute.Launch(code)
                    )
                }
            }
        }
    }

    AppTheme(
        themePreference = settings.themePreference,
        dynamicColor = false
    ) {
        AppNavHost(
            appNavigator = appNavigator
        )
    }
}

@Composable
@Preview(showSystemUi = true)
private fun AndroidAppPreview() {
    AppTheme {
        val appNavigator = rememberAppNavigator()

        AppNavHost(
            appNavigator = appNavigator
        )
    }
}