package com.loomora.core.model

enum class Capability {
    CORE_RECORDING,
    AUDIO_EDITOR,
    OFFLINE_TRANSCRIPTION,
    SPEAKER_DIARIZATION,
    SMART_INSIGHTS,
    LLM_ENHANCED_INSIGHTS,
    MODEL_PACK_STANDARD,
    MODEL_PACK_ADVANCED
}

sealed interface EntitlementStatus {
    data class FreeTrial(val remainingUses: Int) : EntitlementStatus
    data class ProActive(val expirationTimestamp: Long) : EntitlementStatus
    data object Expired : EntitlementStatus
}

sealed interface LicenseValidationResult {
    data class Valid(
        val licenseId: String,
        val expiryTimestamp: Long?,
        val capabilities: Set<Capability>
    ) : LicenseValidationResult
    data class Invalid(val reason: String) : LicenseValidationResult
}

enum class EntitlementDecisionCode {
    GRANTED_FREE,
    GRANTED_LICENSED,
    GRANTED_TRIAL,
    DENIED_EXPIRED,
    DENIED_INVALID_LICENSE,
    DENIED_TRIAL_EXHAUSTED,
    DENIED_UNSUPPORTED_DEVICE,
    DENIED_SUSPICIOUS_CLOCK
}

data class EntitlementDecision(
    val code: EntitlementDecisionCode,
    val capability: Capability,
    val reason: String? = null
) {
    val isGranted: Boolean
        get() = code == EntitlementDecisionCode.GRANTED_FREE ||
            code == EntitlementDecisionCode.GRANTED_LICENSED ||
            code == EntitlementDecisionCode.GRANTED_TRIAL
}

data class EntitlementSnapshot(
    val licensedCapabilities: Set<Capability> = emptySet(),
    val freeCapabilities: Set<Capability> = setOf(Capability.CORE_RECORDING, Capability.AUDIO_EDITOR),
    val licenseId: String? = null,
    val licenseExpiresAtEpochMs: Long? = null,
    val invalidLicenseReason: String? = null,
    val suspiciousClock: Boolean = false
)
