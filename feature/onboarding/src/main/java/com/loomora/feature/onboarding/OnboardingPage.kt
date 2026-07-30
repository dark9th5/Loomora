package com.loomora.feature.onboarding

import androidx.annotation.StringRes

internal enum class OnboardingIllustration {
    LANGUAGE,
    WELCOME,
    RECORD,
    OFFLINE_AI,
    PRIVACY
}

internal enum class OnboardingPageKind {
    LANGUAGE,
    INFORMATION,
    PERMISSION
}

internal data class OnboardingPage(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val illustration: OnboardingIllustration,
    val kind: OnboardingPageKind
)

internal val onboardingPages = listOf(
    OnboardingPage(
        titleRes = R.string.onboarding_language_title,
        descriptionRes = R.string.onboarding_language_description,
        illustration = OnboardingIllustration.LANGUAGE,
        kind = OnboardingPageKind.LANGUAGE
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_welcome_title,
        descriptionRes = R.string.onboarding_welcome_description,
        illustration = OnboardingIllustration.WELCOME,
        kind = OnboardingPageKind.INFORMATION
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_record_title,
        descriptionRes = R.string.onboarding_record_description,
        illustration = OnboardingIllustration.RECORD,
        kind = OnboardingPageKind.INFORMATION
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_ai_title,
        descriptionRes = R.string.onboarding_ai_description,
        illustration = OnboardingIllustration.OFFLINE_AI,
        kind = OnboardingPageKind.INFORMATION
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_privacy_title,
        descriptionRes = R.string.onboarding_privacy_description,
        illustration = OnboardingIllustration.PRIVACY,
        kind = OnboardingPageKind.PERMISSION
    )
)
