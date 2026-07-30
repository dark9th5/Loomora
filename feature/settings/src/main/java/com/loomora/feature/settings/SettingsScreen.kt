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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import com.loomora.core.datastore.TranscriptLanguagePreference
import com.loomora.core.datastore.OfflinePerformanceMode
import com.loomora.core.datastore.DefaultAnalysisMode
import com.loomora.core.datastore.RecordingAudioSource
import com.loomora.core.datastore.NoiseReductionLevel
import com.loomora.core.designsystem.R as DesignSystemR
import com.loomora.core.designsystem.component.LoomoraTopAppBar
import com.loomora.core.designsystem.component.SettingRow
import com.loomora.core.offlineai.ModelCapability
import com.loomora.core.offlineai.DefaultOfflineModelCatalog
import com.loomora.core.offlineai.ModelInstallState
import com.loomora.core.offlineai.OfflineModelManifest
import com.loomora.core.offlineai.RuntimeKind
import java.text.NumberFormat

@Composable
fun SettingsRoute(
    onNavigateToSubscription: () -> Unit,
    onNavigateToTutorial: () -> Unit,
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
        onSetTranscriptLanguage = viewModel::setTranscriptLanguage,
        onSetPerformanceMode = viewModel::setPerformanceMode,
        onSetAnalysisMode = viewModel::setAnalysisMode,
        onSetRecordingAudioSource = viewModel::setRecordingAudioSource,
        onSetNoiseReductionLevel = viewModel::setNoiseReductionLevel,
        onImportModel = { modelImportLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
        onInstallRecommendedModel = viewModel::installRecommendedModel,
        onInstallModel = viewModel::installModel,
        onSelectTranscriptionModel = viewModel::setTranscriptionModel,
        onRemoveModel = viewModel::removeModel,
        onNavigateToSubscription = onNavigateToSubscription,
        onNavigateToTutorial = onNavigateToTutorial,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onSetDarkThemeConfig: (DarkThemeConfig) -> Unit,
    onSetLanguageCode: (String) -> Unit,
    onSetTranscriptLanguage: (TranscriptLanguagePreference) -> Unit,
    onSetPerformanceMode: (OfflinePerformanceMode) -> Unit,
    onSetAnalysisMode: (DefaultAnalysisMode) -> Unit,
    onSetRecordingAudioSource: (RecordingAudioSource) -> Unit,
    onSetNoiseReductionLevel: (NoiseReductionLevel) -> Unit,
    onImportModel: () -> Unit,
    onInstallRecommendedModel: () -> Unit,
    onInstallModel: (String) -> Unit,
    onSelectTranscriptionModel: (String) -> Unit,
    onRemoveModel: (String) -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToTutorial: () -> Unit,
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

            SectionHeader(title = stringResource(R.string.settings_recording_section))
            PreferenceSelector(
                title = stringResource(R.string.settings_audio_source),
                options = RecordingAudioSource.entries,
                selected = uiState.recordingAudioSource,
                label = { source ->
                    stringResource(when (source) {
                        RecordingAudioSource.MIC -> R.string.settings_audio_source_mic
                        RecordingAudioSource.VOICE_RECOGNITION -> R.string.settings_audio_source_voice_recognition
                        RecordingAudioSource.CAMCORDER -> R.string.settings_audio_source_camcorder
                    })
                },
                onSelect = onSetRecordingAudioSource
            )
            PreferenceSelector(
                title = stringResource(R.string.settings_noise_reduction),
                options = NoiseReductionLevel.entries,
                selected = uiState.noiseReductionLevel,
                label = { level ->
                    stringResource(when (level) {
                        NoiseReductionLevel.OFF -> R.string.settings_noise_reduction_off
                        NoiseReductionLevel.LIGHT -> R.string.settings_noise_reduction_light
                        NoiseReductionLevel.STRONG -> R.string.settings_noise_reduction_strong
                    })
                },
                onSelect = onSetNoiseReductionLevel
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SectionHeader(title = stringResource(R.string.settings_offline_ai_section))
            PreferenceSelector(
                title = stringResource(R.string.settings_transcript_language),
                options = TranscriptLanguagePreference.entries,
                selected = uiState.transcriptLanguage,
                label = { option ->
                    stringResource(when (option) {
                        TranscriptLanguagePreference.AUTO -> R.string.settings_transcript_auto
                        TranscriptLanguagePreference.ENGLISH -> R.string.settings_language_english
                        TranscriptLanguagePreference.VIETNAMESE -> R.string.settings_language_vietnamese
                    })
                },
                onSelect = onSetTranscriptLanguage
            )
            PreferenceSelector(
                title = stringResource(R.string.settings_performance_mode),
                options = OfflinePerformanceMode.entries,
                selected = uiState.performanceMode,
                label = { option ->
                    stringResource(when (option) {
                        OfflinePerformanceMode.BATTERY_SAVER -> R.string.settings_performance_battery
                        OfflinePerformanceMode.BALANCED -> R.string.settings_performance_balanced
                        OfflinePerformanceMode.FAST -> R.string.settings_performance_fast
                    })
                },
                onSelect = onSetPerformanceMode
            )
            PreferenceSelector(
                title = stringResource(R.string.settings_analysis_mode),
                options = DefaultAnalysisMode.entries,
                selected = uiState.analysisMode,
                label = { option ->
                    stringResource(when (option) {
                        DefaultAnalysisMode.QUICK_TRANSCRIPT -> R.string.settings_analysis_quick
                        DefaultAnalysisMode.TRANSCRIPT_AND_INSIGHTS -> R.string.settings_analysis_insights
                        DefaultAnalysisMode.FULL_ANALYSIS -> R.string.settings_analysis_full
                    })
                },
                onSelect = onSetAnalysisMode
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

            SectionHeader(title = stringResource(R.string.settings_models_section))

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
                                text = stringResource(R.string.settings_models_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_models_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onImportModel) {
                            Text(text = stringResource(R.string.settings_models_import))
                        }
                    }

                    val recommendedReady = setOf(
                        DefaultOfflineModelCatalog.RECOMMENDED_TRANSCRIPTION_MODEL_ID,
                        DefaultOfflineModelCatalog.RECOMMENDED_VAD_MODEL_ID,
                        DefaultOfflineModelCatalog.RECOMMENDED_DIARIZATION_MODEL_ID
                    ).all { modelId ->
                        uiState.offlineModels.any {
                            it.manifest.id == modelId && it.state == ModelInstallState.READY
                        }
                    }
                    if (!recommendedReady) {
                        androidx.compose.material3.Button(
                            onClick = onInstallRecommendedModel,
                            enabled = uiState.modelDownloadProgress == null,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            Text(
                                if (uiState.modelDownloadProgress == null) {
                                    stringResource(R.string.settings_models_install_recommended)
                                } else {
                                    stringResource(
                                        R.string.settings_models_downloading,
                                        uiState.modelDownloadProgress
                                    )
                                }
                            )
                        }
                    }

                    uiState.modelImportError?.let {
                        Text(
                            text = stringResource(R.string.settings_models_import_failed),
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
                            text = stringResource(
                                R.string.settings_models_capability_status,
                                model.manifest.capability.localizedLabel(),
                                model.state.localizedLabel()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_models_runtime_version,
                                model.manifest.runtime.localizedLabel(),
                                model.manifest.version
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_models_size,
                                formatModelSize(model.manifest)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_models_checksum,
                                model.manifest.sha256
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_models_license,
                                model.manifest.licenseName
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        model.manifest.sourceUrl?.let { sourceUrl ->
                            Text(
                                text = stringResource(
                                    R.string.settings_models_source,
                                    sourceUrl
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        model.errorCode?.let { errorCode ->
                            Text(
                                text = stringResource(
                                    R.string.settings_models_status_detail,
                                    errorCode
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (
                            model.manifest.capability == ModelCapability.TRANSCRIPTION &&
                            model.state == ModelInstallState.READY
                        ) {
                            val selected = uiState.transcriptionModelId == model.manifest.id
                            Text(
                                text = stringResource(
                                    if (selected) R.string.settings_models_in_use
                                    else R.string.settings_models_available
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (!selected) {
                                TextButton(onClick = { onSelectTranscriptionModel(model.manifest.id) }) {
                                    Text(text = stringResource(R.string.settings_models_use_this))
                                }
                            }
                        }
                        if (model.state != ModelInstallState.NOT_INSTALLED && model.state != ModelInstallState.IMPORTING && model.state != ModelInstallState.VERIFYING) {
                            TextButton(
                                onClick = { onRemoveModel(model.manifest.id) },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(text = stringResource(R.string.settings_models_remove))
                            }
                        } else if (
                            model.state == ModelInstallState.NOT_INSTALLED &&
                            model.manifest.capability in setOf(
                                ModelCapability.TRANSCRIPTION,
                                ModelCapability.VOICE_ACTIVITY_DETECTION,
                                ModelCapability.DIARIZATION
                            )
                        ) {
                            TextButton(
                                onClick = { onInstallModel(model.manifest.id) },
                                enabled = uiState.modelDownloadProgress == null,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(text = stringResource(R.string.settings_models_install_this))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SectionHeader(title = stringResource(R.string.settings_help_section))
            SettingRow(
                title = stringResource(R.string.settings_help_tutorial),
                subtitle = stringResource(R.string.settings_help_tutorial_description),
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                onClick = onNavigateToTutorial
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Section: About
            SectionHeader(title = stringResource(id = R.string.settings_section_about))

            SettingRow(
                title = stringResource(id = DesignSystemR.string.app_name),
                subtitle = stringResource(id = DesignSystemR.string.app_tagline),
                icon = Icons.Default.Info,
                trailingContent = {
                    Text(
                        text = uiState.appVersion,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@Composable
private fun <T> PreferenceSelector(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    options.forEach { option ->
        SettingRow(
            title = label(option),
            trailingContent = {
                RadioButton(selected = selected == option, onClick = { onSelect(option) })
            },
            onClick = { onSelect(option) }
        )
    }
}

@Composable
private fun ModelCapability.localizedLabel(): String = stringResource(
    when (this) {
        ModelCapability.TRANSCRIPTION -> R.string.settings_model_capability_transcription
        ModelCapability.VOICE_ACTIVITY_DETECTION -> R.string.settings_model_capability_vad
        ModelCapability.DIARIZATION -> R.string.settings_model_capability_diarization
        ModelCapability.INSIGHTS -> R.string.settings_model_capability_insights
        ModelCapability.SPEECH_ENHANCEMENT -> R.string.settings_model_capability_speech_enhancement
    }
)

@Composable
private fun ModelInstallState.localizedLabel(): String = stringResource(
    when (this) {
        ModelInstallState.NOT_INSTALLED -> R.string.settings_model_state_not_installed
        ModelInstallState.IMPORTING -> R.string.settings_model_state_importing
        ModelInstallState.VERIFYING -> R.string.settings_model_state_verifying
        ModelInstallState.READY -> R.string.settings_model_state_ready
        ModelInstallState.INCOMPATIBLE -> R.string.settings_model_state_incompatible
        ModelInstallState.CORRUPT -> R.string.settings_model_state_corrupt
        ModelInstallState.REMOVING -> R.string.settings_model_state_removing
        ModelInstallState.ERROR -> R.string.settings_model_state_error
    }
)

@Composable
private fun RuntimeKind.localizedLabel(): String = stringResource(
    when (this) {
        RuntimeKind.SHERPA_ONNX -> R.string.settings_model_runtime_sherpa
        RuntimeKind.LITERT_LM -> R.string.settings_model_runtime_litert
        RuntimeKind.LLAMA_CPP -> R.string.settings_model_runtime_llama
    }
)

private fun formatModelSize(manifest: OfflineModelManifest): String {
    val fileSizes = listOf(manifest.sizeBytes) + manifest.additionalFiles.map { it.sizeBytes }
    if (fileSizes.any { it < 0L }) return "?"
    val megabytes = fileSizes.sum() / (1024.0 * 1024.0)
    return NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(megabytes)
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
