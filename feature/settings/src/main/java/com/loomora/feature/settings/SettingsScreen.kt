package com.loomora.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loomora.core.datastore.DarkThemeConfig
import com.loomora.core.designsystem.R
import com.loomora.core.designsystem.component.LoomoraTopAppBar
import com.loomora.core.designsystem.component.SettingRow
import com.loomora.core.offlineai.ModelInstallState

@Composable
fun SettingsRoute(
    onNavigateToSubscription: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val modelImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importModel(uri)
        }
    }

    SettingsScreen(
        uiState = uiState,
        onSetDarkThemeConfig = viewModel::setDarkThemeConfig,
        onSetLanguageCode = viewModel::setLanguageCode,
        onImportModel = { modelImportLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
        onRemoveModel = viewModel::removeModel,
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
    onImportModel: () -> Unit,
    onRemoveModel: (String) -> Unit,
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

            SectionHeader(title = "Offline AI Models")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Model Manager",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Import and verify local ASR/insight models without network processing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onImportModel) {
                            Text(text = "Import")
                        }
                    }

                    uiState.modelImportError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    uiState.offlineModels.forEach { model ->
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text(
                            text = model.manifest.id,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${model.manifest.capability.name} • ${model.state.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "License: ${model.manifest.licenseName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (model.manifest.sourceUrl != null) {
                            Text(
                                text = "Source: ${model.manifest.sourceUrl}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        model.errorCode?.let { errorCode ->
                            Text(
                                text = "Status detail: $errorCode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (model.state != ModelInstallState.NOT_INSTALLED && model.state != ModelInstallState.IMPORTING && model.state != ModelInstallState.VERIFYING) {
                            TextButton(
                                onClick = { onRemoveModel(model.manifest.id) },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(text = "Remove")
                            }
                        }
                    }
                }
            }

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
