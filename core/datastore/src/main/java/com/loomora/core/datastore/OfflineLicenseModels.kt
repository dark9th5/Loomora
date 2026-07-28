package com.loomora.core.datastore

import com.loomora.core.model.Capability
import kotlinx.serialization.Serializable

@Serializable
data class SignedLicenseEnvelope(
    val payload: LicensePayload,
    val signatureAlgorithm: String,
    val keyId: String,
    val signature: String
)

@Serializable
data class LicensePayload(
    val schemaVersion: Int,
    val licenseId: String,
    val product: String,
    val edition: String,
    val capabilities: List<String>,
    val issuedAt: String,
    val notBefore: String,
    val expiresAt: String? = null,
    val deviceBinding: String? = null,
    val licenseVersion: Int
)

sealed interface OfflineLicenseVerification {
    data class Valid(
        val envelopeJson: String,
        val payload: LicensePayload,
        val capabilities: Set<Capability>,
        val expiresAtEpochMs: Long?
    ) : OfflineLicenseVerification

    data class Invalid(val reason: String) : OfflineLicenseVerification
}
