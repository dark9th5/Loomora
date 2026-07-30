package com.loomora

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.navigation.compose.rememberNavController
import com.loomora.core.datastore.DarkThemeConfig
import com.loomora.core.datastore.SupportedAppLanguage
import com.loomora.core.designsystem.theme.LoomoraTheme
import com.loomora.feature.onboarding.CURRENT_ONBOARDING_VERSION
import com.loomora.navigation.LoomoraNavHost
import com.loomora.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            val isDarkTheme = when (uiState.darkThemeConfig) {
                DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                DarkThemeConfig.LIGHT -> false
                DarkThemeConfig.DARK -> true
            }

            LoomoraTheme(darkTheme = isDarkTheme) {
                if (uiState.isLoading) {
                    LoomoraSplashScreen()
                } else {
                    LoomoraRoot(
                        uiState = uiState,
                        onSystemLanguageChanged = viewModel::syncAppLanguage
                    )
                }
            }
        }
    }
}

@Composable
private fun LoomoraRoot(
    uiState: MainUiState,
    onSystemLanguageChanged: (SupportedAppLanguage) -> Unit
) {
    LaunchedEffect(uiState.languageCode) {
        val applicationLocales = AppCompatDelegate.getApplicationLocales()
        val appliedLanguage = applicationLocales.get(0)?.toLanguageTag()
        if (appliedLanguage == null) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(uiState.languageCode)
            )
        } else {
            val normalizedAppliedLanguage = SupportedAppLanguage.fromLanguageTag(appliedLanguage)
            if (normalizedAppliedLanguage.tag != uiState.languageCode) {
                onSystemLanguageChanged(normalizedAppliedLanguage)
            }
        }
    }

    val startDestination = if (
        uiState.hasCompletedOnboarding ||
        uiState.onboardingVersionSeen >= CURRENT_ONBOARDING_VERSION
    ) {
        Screen.Home.route
    } else {
        Screen.Onboarding.route
    }
    val navController = rememberNavController()
    LoomoraNavHost(
        navController = navController,
        startDestination = startDestination
    )
}

@Composable
private fun LoomoraSplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
