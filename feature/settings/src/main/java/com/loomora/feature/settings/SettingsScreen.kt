package com.loomora.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loomora.core.datastore.DarkThemeConfig
import com.loomora.core.designsystem.R
import com.loomora.core.designsystem.component.LoomoraTopAppBar
import com.loomora.core.designsystem.component.SettingRow

@Composable
fun SettingsRoute(
    onNavigateToSubscription: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreen(
        uiState = uiState,
        onSetDarkThemeConfig = viewModel::setDarkThemeConfig,
        onSetLanguageCode = viewModel::setLanguageCode,
        onNavigateToSubscription = onNavigateToSubscription,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onSetDarkThemeConfig: (DarkThemeConfig) -> Unit,
    onSetLanguageCode: (String) -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            LoomoraTopAppBar(
                title = stringResource(id = R.string.settings_title),
                onBackClick = onNavigateBack
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Subscription / Pro Upgrade Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(onClick = onNavigateToSubscription),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_pro_status),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(id = R.string.settings_upgrade_pro),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Section: Appearance
            SectionHeader(title = stringResource(id = R.string.settings_section_appearance))
            
            ThemeSelectorRow(
                selectedConfig = uiState.darkThemeConfig,
                onSelectConfig = onSetDarkThemeConfig
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Section: Language
            SectionHeader(title = stringResource(id = R.string.settings_section_language))

            LanguageSelectorRow(
                selectedLanguage = uiState.languageCode,
                onSelectLanguage = onSetLanguageCode
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Section: Privacy & Guarantee
            SectionHeader(title = stringResource(id = R.string.settings_section_privacy))
            
            SettingRow(
                title = stringResource(id = R.string.settings_privacy_guarantee),
                subtitle = stringResource(id = R.string.settings_privacy_guarantee_desc),
                icon = Icons.Default.Shield
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Section: About
            SectionHeader(title = stringResource(id = R.string.settings_section_about))

            SettingRow(
                title = stringResource(id = R.string.app_name),
                subtitle = stringResource(id = R.string.app_tagline),
                icon = Icons.Default.Info,
                trailingContent = {
                    Text(
                        text = "1.0.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ThemeSelectorRow(
    selectedConfig: DarkThemeConfig,
    onSelectConfig: (DarkThemeConfig) -> Unit
) {
    Column {
        SettingRow(
            title = stringResource(id = R.string.settings_theme_system),
            icon = Icons.Default.Palette,
            trailingContent = {
                RadioButton(
                    selected = selectedConfig == DarkThemeConfig.FOLLOW_SYSTEM,
                    onClick = { onSelectConfig(DarkThemeConfig.FOLLOW_SYSTEM) }
                )
            },
            onClick = { onSelectConfig(DarkThemeConfig.FOLLOW_SYSTEM) }
        )
        SettingRow(
            title = stringResource(id = R.string.settings_theme_light),
            trailingContent = {
                RadioButton(
                    selected = selectedConfig == DarkThemeConfig.LIGHT,
                    onClick = { onSelectConfig(DarkThemeConfig.LIGHT) }
                )
            },
            onClick = { onSelectConfig(DarkThemeConfig.LIGHT) }
        )
        SettingRow(
            title = stringResource(id = R.string.settings_theme_dark),
            trailingContent = {
                RadioButton(
                    selected = selectedConfig == DarkThemeConfig.DARK,
                    onClick = { onSelectConfig(DarkThemeConfig.DARK) }
                )
            },
            onClick = { onSelectConfig(DarkThemeConfig.DARK) }
        )
    }
}

@Composable
private fun LanguageSelectorRow(
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit
) {
    Column {
        SettingRow(
            title = stringResource(id = R.string.settings_language_english),
            icon = Icons.Default.Language,
            trailingContent = {
                RadioButton(
                    selected = selectedLanguage == "en",
                    onClick = { onSelectLanguage("en") }
                )
            },
            onClick = { onSelectLanguage("en") }
        )
        SettingRow(
            title = stringResource(id = R.string.settings_language_vietnamese),
            trailingContent = {
                RadioButton(
                    selected = selectedLanguage == "vi",
                    onClick = { onSelectLanguage("vi") }
                )
            },
            onClick = { onSelectLanguage("vi") }
        )
    }
}
