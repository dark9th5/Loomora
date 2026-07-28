package com.loomora.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.loomora.core.model.Capability
import com.loomora.core.model.EntitlementDecisionCode
import com.loomora.core.model.LicenseValidationResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.KeyPairGenerator
import java.security.Signature
import java.time.Instant
import java.util.Base64

class EntitlementManagerTest {
    private val json = Json {
        encodeDefaults = true
        prettyPrint = false
    }

    @Test
    fun coreRecordingCapability_isAlwaysGrantedFree() = runTest {
        val repository = repository()

        val decision = repository.canUse(Capability.CORE_RECORDING)

        assertEquals(EntitlementDecisionCode.GRANTED_FREE, decision.code)
    }

    @Test
    fun legacyFakeProToken_isRejected() = runTest {
        val manager = EntitlementManager(repository())

        val result = manager.activateLicenseKey("LM-PRO-KEY-1234")

        assertTrue(result is LicenseValidationResult.Invalid)
        assertFalse(manager.isCapabilityGranted(Capability.OFFLINE_TRANSCRIPTION))
    }

    @Test
    fun validSignedLicense_enablesOnlyDeclaredCapabilities() = runTest {
        val keySet = TestKeySet()
        val repository = repository(publicKeys = mapOf(keySet.keyId to keySet.publicKeyBase64))
        val envelope = keySet.sign(
            LicensePayload(
                schemaVersion = 1,
                licenseId = "lic_test",
                product = "loomora",
                edition = "pro",
                capabilities = listOf(Capability.OFFLINE_TRANSCRIPTION.name),
                issuedAt = "2026-07-28T00:00:00Z",
                notBefore = "2026-07-28T00:00:00Z",
                expiresAt = "2026-08-28T00:00:00Z",
                deviceBinding = null,
                licenseVersion = 1
            )
        )

        val result = repository.importLicense(envelope)

        assertTrue(result is LicenseValidationResult.Valid)
        assertEquals(EntitlementDecisionCode.GRANTED_LICENSED, repository.canUse(Capability.OFFLINE_TRANSCRIPTION).code)
        assertEquals(EntitlementDecisionCode.DENIED_TRIAL_EXHAUSTED, repository.canUse(Capability.SPEAKER_DIARIZATION).code)
    }

    @Test
    fun payloadTamper_invalidatesLicense() {
        val keySet = TestKeySet()
        val verifier = OfflineLicenseVerifier(json, mapOf(keySet.keyId to keySet.publicKeyBase64))
        val envelope = keySet.sign(
            LicensePayload(
                schemaVersion = 1,
                licenseId = "lic_test",
                product = "loomora",
                edition = "pro",
                capabilities = listOf(Capability.SMART_INSIGHTS.name),
                issuedAt = "2026-07-28T00:00:00Z",
                notBefore = "2026-07-28T00:00:00Z",
                expiresAt = "2026-08-28T00:00:00Z",
                deviceBinding = null,
                licenseVersion = 1
            )
        )
        val tampered = envelope.replace(Capability.SMART_INSIGHTS.name, Capability.LLM_ENHANCED_INSIGHTS.name)

        val result = verifier.verify(tampered, nowEpochMs = Instant.parse("2026-07-29T00:00:00Z").toEpochMilli())

        assertTrue(result is OfflineLicenseVerification.Invalid)
    }

    private fun repository(
        publicKeys: Map<String, String> = emptyMap()
    ): EntitlementRepository {
        val dataStore = PreferenceDataStoreFactory.create {
            File.createTempFile("loomora-entitlement", ".preferences_pb").apply { deleteOnExit() }
        }
        return EntitlementRepository(
            dataStore = dataStore,
            verifier = OfflineLicenseVerifier(json, publicKeys)
        )
    }

    private inner class TestKeySet {
        val keyId = "loomora-test"
        private val pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val publicKeyBase64: String = Base64.getEncoder().encodeToString(pair.public.encoded)

        fun sign(payload: LicensePayload): String {
            val verifier = OfflineLicenseVerifier(json, mapOf(keyId to publicKeyBase64))
            val canonicalPayload = verifier.canonicalPayload(payload)
            val signature = Signature.getInstance("Ed25519").run {
                initSign(pair.private)
                update(canonicalPayload.toByteArray(Charsets.UTF_8))
                sign()
            }
            return json.encodeToString(
                SignedLicenseEnvelope(
                    payload = payload,
                    signatureAlgorithm = "Ed25519",
                    keyId = keyId,
                    signature = Base64.getEncoder().encodeToString(signature)
                )
            )
        }
    }
}
