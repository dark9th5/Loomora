package com.loomora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.loomora.core.datastore.DarkThemeConfig
import com.loomora.core.designsystem.theme.LoomoraTheme
import com.loomora.navigation.LoomoraNavHost
import com.loomora.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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

            val startDestination = if (uiState.hasCompletedOnboarding) {
                Screen.Home.route
            } else {
                Screen.Onboarding.route
            }

            LoomoraTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                LoomoraNavHost(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
