package com.loomora.core.model

enum class Capability {
    LOCAL_RECORDING,
    AI_TRANSCRIPTION,
    SMART_INSIGHTS,
    ADVANCED_EXPORT
}

sealed interface EntitlementStatus {
    data class FreeTrial(val remainingUses: Int) : EntitlementStatus
    data class ProActive(val expirationTimestamp: Long) : EntitlementStatus
    data object Expired : EntitlementStatus
}

sealed interface LicenseValidationResult {
    data class Valid(val expiryTimestamp: Long) : LicenseValidationResult
    data class Invalid(val reason: String) : LicenseValidationResult
}
