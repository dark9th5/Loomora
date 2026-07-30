package com.loomora.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.loomora.core.datastore.SupportedAppLanguage

enum class OnboardingMode {
    FIRST_RUN,
    TUTORIAL
}

@Composable
fun OnboardingRoute(
    onCompleteOnboarding: () -> Unit,
    mode: OnboardingMode = OnboardingMode.FIRST_RUN,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequested = true
        viewModel.setMicrophonePermissionGranted(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.setMicrophonePermissionGranted(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    LaunchedEffect(uiState.isCompleted, mode) {
        if (mode == OnboardingMode.FIRST_RUN && uiState.isCompleted) {
            onCompleteOnboarding()
        }
    }

    OnboardingScreen(
        uiState = uiState,
        mode = mode,
        permissionRequested = permissionRequested,
        onSelectLanguage = viewModel::selectLanguage,
        onPreviousPage = viewModel::previousPage,
        onNextPage = viewModel::nextPage,
        onRequestMicrophonePermission = {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onSkipPermission = {
            permissionRequested = true
            viewModel.skipOptionalPermission()
        },
        onInstallRecommendedModel = viewModel::installRecommendedModel,
        onCompleteOnboarding = if (mode == OnboardingMode.FIRST_RUN) {
            viewModel::completeOnboarding
        } else {
            onCompleteOnboarding
        },
        modifier = modifier
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    mode: OnboardingMode,
    permissionRequested: Boolean,
    onSelectLanguage: (SupportedAppLanguage) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onSkipPermission: () -> Unit,
    onInstallRecommendedModel: () -> Unit,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = uiState.pageIndex,
        pageCount = { onboardingPages.size }
    )
    LaunchedEffect(uiState.pageIndex) {
        if (pagerState.currentPage != uiState.pageIndex) {
            pagerState.animateScrollToPage(uiState.pageIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_brand),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            OnboardingPageContent(
                page = onboardingPages[pageIndex],
                uiState = uiState,
                permissionRequested = permissionRequested,
                onSelectLanguage = onSelectLanguage,
                onRequestMicrophonePermission = onRequestMicrophonePermission,
                onSkipPermission = onSkipPermission,
                onInstallRecommendedModel = onInstallRecommendedModel
            )
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            onboardingPages.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(
                            width = if (index == uiState.pageIndex) 24.dp else 8.dp,
                            height = 8.dp
                        )
                        .clip(CircleShape)
                        .background(
                            if (index == uiState.pageIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.pageIndex > 0) {
                OutlinedButton(
                    onClick = onPreviousPage,
                    enabled = !uiState.isCompleting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.onboarding_back))
                }
            }
            Button(
                onClick = if (uiState.pageIndex == onboardingPages.lastIndex) {
                    onCompleteOnboarding
                } else {
                    onNextPage
                },
                enabled = !uiState.isCompleting,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    stringResource(
                        if (uiState.pageIndex == onboardingPages.lastIndex) {
                            if (mode == OnboardingMode.FIRST_RUN) {
                                R.string.onboarding_start
                            } else {
                                R.string.onboarding_done
                            }
                        } else {
                            R.string.onboarding_next
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    uiState: OnboardingUiState,
    permissionRequested: Boolean,
    onSelectLanguage: (SupportedAppLanguage) -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onSkipPermission: () -> Unit,
    onInstallRecommendedModel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.illustration.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(page.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        when (page.kind) {
            OnboardingPageKind.LANGUAGE -> LanguageOptions(uiState, onSelectLanguage)
            OnboardingPageKind.PERMISSION -> PermissionOptions(
                granted = uiState.microphonePermissionGranted,
                permissionRequested = permissionRequested,
                onRequest = onRequestMicrophonePermission,
                onSkip = onSkipPermission
            )
            OnboardingPageKind.INFORMATION -> if (page.illustration == OnboardingIllustration.OFFLINE_AI) {
                RecommendedModelOption(uiState, onInstallRecommendedModel)
            } else Unit
        }
    }
}

@Composable
private fun RecommendedModelOption(uiState: OnboardingUiState, onInstall: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    if (uiState.isRecommendedModelInstalled) {
        Text(
            text = stringResource(R.string.onboarding_model_ready),
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        Button(
            onClick = onInstall,
            enabled = uiState.modelDownloadProgress == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.modelDownloadProgress == null) {
                    stringResource(R.string.onboarding_model_install)
                } else {
                    stringResource(R.string.onboarding_model_downloading, uiState.modelDownloadProgress)
                }
            )
        }
        if (uiState.modelDownloadFailed) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_model_download_failed),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_model_optional),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LanguageOptions(
    uiState: OnboardingUiState,
    onSelectLanguage: (SupportedAppLanguage) -> Unit
) {
    Spacer(Modifier.height(20.dp))
    SupportedAppLanguage.entries.forEach { language ->
        val label = when (language) {
            SupportedAppLanguage.ENGLISH -> R.string.onboarding_language_english
            SupportedAppLanguage.VIETNAMESE -> R.string.onboarding_language_vietnamese
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = uiState.selectedLanguage == language,
                onClick = { onSelectLanguage(language) }
            )
            Text(text = stringResource(label))
        }
    }
}

@Composable
private fun PermissionOptions(
    granted: Boolean,
    permissionRequested: Boolean,
    onRequest: () -> Unit,
    onSkip: () -> Unit
) {
    Spacer(Modifier.height(16.dp))
    if (granted) {
        Text(
            text = stringResource(R.string.onboarding_microphone_granted),
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        if (permissionRequested) {
            Text(
                text = stringResource(R.string.onboarding_microphone_denied),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
        }
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_allow_microphone))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_not_now))
        }
    }
}

private fun OnboardingIllustration.icon(): ImageVector = when (this) {
    OnboardingIllustration.LANGUAGE -> Icons.Default.Language
    OnboardingIllustration.WELCOME -> Icons.Default.AutoAwesome
    OnboardingIllustration.RECORD -> Icons.Default.RadioButtonChecked
    OnboardingIllustration.OFFLINE_AI -> Icons.Default.AutoAwesome
    OnboardingIllustration.PRIVACY -> Icons.Default.Shield
}
