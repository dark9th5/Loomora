package com.loomora.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.loomora.core.model.Capability
import com.loomora.core.model.EntitlementDecision
import com.loomora.core.model.EntitlementDecisionCode
import com.loomora.core.model.EntitlementSnapshot
import com.loomora.core.model.LicenseValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntitlementRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val verifier: OfflineLicenseVerifier
) {
    fun observeEntitlements(): Flow<EntitlementSnapshot> {
        return dataStore.data.map { preferences -> snapshotFrom(preferences, System.currentTimeMillis()) }
    }

    suspend fun canUse(capability: Capability): EntitlementDecision {
        val now = System.currentTimeMillis()
        val preferences = dataStore.data.first()
        rememberClock(now, preferences)
        val snapshot = snapshotFrom(preferences, now)
        return decisionFrom(snapshot, capability)
    }

    suspend fun importLicense(envelopeJson: String): LicenseValidationResult {
        val verification = verifier.verify(envelopeJson)
        if (verification !is OfflineLicenseVerification.Valid) {
            val reason = (verification as OfflineLicenseVerification.Invalid).reason
            dataStore.edit { preferences ->
                preferences[Keys.INVALID_LICENSE_REASON] = reason
                preferences.remove(Keys.SIGNED_LICENSE_ENVELOPE)
                preferences.remove(Keys.LICENSE_ID)
                preferences.remove(Keys.LICENSE_CAPABILITIES)
                preferences.remove(Keys.LICENSE_EXPIRES_AT)
            }
            return LicenseValidationResult.Invalid(reason)
        }

        dataStore.edit { preferences ->
            preferences[Keys.SIGNED_LICENSE_ENVELOPE] = verification.envelopeJson
            preferences[Keys.LICENSE_ID] = verification.payload.licenseId
            preferences[Keys.LICENSE_CAPABILITIES] = verification.capabilities.joinToString(",") { it.name }
            verification.expiresAtEpochMs?.let { preferences[Keys.LICENSE_EXPIRES_AT] = it }
                ?: preferences.remove(Keys.LICENSE_EXPIRES_AT)
            preferences.remove(Keys.INVALID_LICENSE_REASON)
        }
        return LicenseValidationResult.Valid(
            licenseId = verification.payload.licenseId,
            expiryTimestamp = verification.expiresAtEpochMs,
            capabilities = verification.capabilities
        )
    }

    suspend fun removeLicense() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.SIGNED_LICENSE_ENVELOPE)
            preferences.remove(Keys.LICENSE_ID)
            preferences.remove(Keys.LICENSE_CAPABILITIES)
            preferences.remove(Keys.LICENSE_EXPIRES_AT)
            preferences.remove(Keys.INVALID_LICENSE_REASON)
        }
    }

    private suspend fun rememberClock(now: Long, preferences: Preferences) {
        val lastSeen = preferences[Keys.LAST_SEEN_WALL_CLOCK] ?: 0L
        if (now >= lastSeen) {
            dataStore.edit { it[Keys.LAST_SEEN_WALL_CLOCK] = now }
        }
    }

    private fun snapshotFrom(preferences: Preferences, now: Long): EntitlementSnapshot {
        val lastSeen = preferences[Keys.LAST_SEEN_WALL_CLOCK] ?: 0L
        val suspiciousClock = lastSeen > 0L && now + CLOCK_ROLLBACK_TOLERANCE_MS < lastSeen
        val envelope = preferences[Keys.SIGNED_LICENSE_ENVELOPE]
        val verification = envelope?.let { verifier.verify(it, nowEpochMs = now) }
        return when (verification) {
            is OfflineLicenseVerification.Valid -> EntitlementSnapshot(
                licensedCapabilities = verification.capabilities,
                licenseId = verification.payload.licenseId,
                licenseExpiresAtEpochMs = verification.expiresAtEpochMs,
                suspiciousClock = suspiciousClock
            )
            is OfflineLicenseVerification.Invalid -> EntitlementSnapshot(
                invalidLicenseReason = verification.reason,
                suspiciousClock = suspiciousClock
            )
            null -> EntitlementSnapshot(
                invalidLicenseReason = preferences[Keys.INVALID_LICENSE_REASON],
                suspiciousClock = suspiciousClock
            )
        }
    }

    private fun decisionFrom(
        snapshot: EntitlementSnapshot,
        capability: Capability
    ): EntitlementDecision {
        if (capability in snapshot.freeCapabilities) {
            return EntitlementDecision(EntitlementDecisionCode.GRANTED_FREE, capability)
        }
        if (snapshot.suspiciousClock) {
            return EntitlementDecision(
                EntitlementDecisionCode.DENIED_SUSPICIOUS_CLOCK,
                capability,
                "Device clock moved backwards."
            )
        }
        if (capability in snapshot.licensedCapabilities) {
            return EntitlementDecision(EntitlementDecisionCode.GRANTED_LICENSED, capability)
        }
        val invalidReason = snapshot.invalidLicenseReason
        if (invalidReason != null) {
            if (invalidReason.contains("expired", ignoreCase = true)) {
                return EntitlementDecision(
                    EntitlementDecisionCode.DENIED_EXPIRED,
                    capability,
                    invalidReason
                )
            }
            return EntitlementDecision(
                EntitlementDecisionCode.DENIED_INVALID_LICENSE,
                capability,
                invalidReason
            )
        }
        return EntitlementDecision(
            EntitlementDecisionCode.DENIED_TRIAL_EXHAUSTED,
            capability,
            "No valid license or trial reservation is available."
        )
    }

    private object Keys {
        val SIGNED_LICENSE_ENVELOPE = stringPreferencesKey("signed_license_envelope_v1")
        val LICENSE_ID = stringPreferencesKey("license_id")
        val LICENSE_CAPABILITIES = stringPreferencesKey("license_capabilities")
        val LICENSE_EXPIRES_AT = longPreferencesKey("license_expires_at")
        val INVALID_LICENSE_REASON = stringPreferencesKey("invalid_license_reason")
        val LAST_SEEN_WALL_CLOCK = longPreferencesKey("entitlement_last_seen_wall_clock")
    }

    private companion object {
        const val CLOCK_ROLLBACK_TOLERANCE_MS = 5 * 60 * 1000L
    }
}
