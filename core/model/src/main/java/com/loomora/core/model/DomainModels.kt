package com.loomora.core.model

data class AudioSegment(
    val id: String,
    val recordingId: String,
    val orderIndex: Int,
    val startOffsetMs: Long,
    val durationMs: Long,
    val filePath: String,
    val sizeBytes: Long,
    val checksum: String,
    val isFinalized: Boolean
)

enum class EntitlementPlan {
    GUEST_FREE,
    TRIAL,
    PRO,
    BUSINESS
}

data class Entitlement(
    val plan: EntitlementPlan = EntitlementPlan.GUEST_FREE,
    val signedToken: String? = null,
    val validUntil: Long? = null,
    val isOfflineGraceValid: Boolean = true
)

data class TrialUsage(
    val capability: String,
    val successfulUses: Int = 0,
    val maxFreeUses: Int = 3
) {
    val remainingUses: Int
        get() = (maxFreeUses - successfulUses).coerceAtLeast(0)

    val isTrialExhausted: Boolean
        get() = remainingUses <= 0
}
