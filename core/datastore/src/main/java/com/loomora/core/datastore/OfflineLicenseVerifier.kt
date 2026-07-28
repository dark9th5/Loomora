package com.loomora.core.datastore

import com.loomora.core.model.Capability
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineLicenseVerifier(
    private val json: Json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
        prettyPrint = false
    },
    private val productionPublicKeys: Map<String, String> = emptyMap()
) {
    @Inject
    constructor() : this(
        Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
            prettyPrint = false
        },
        mapOf(
            "loomora-prod-2026-01" to "MCowBQYDK2VwAyEA/4qV7ycbmTg0luoDwJQPb87dnVQmHUsAfiDT+PIrzP8="
        )
    )

    fun verify(
        envelopeJson: String,
        nowEpochMs: Long = System.currentTimeMillis(),
        publicKeysById: Map<String, String> = productionPublicKeys,
        expectedDeviceBinding: String? = null
    ): OfflineLicenseVerification {
        val envelope = try {
            json.decodeFromString<SignedLicenseEnvelope>(envelopeJson)
        } catch (_: SerializationException) {
            return OfflineLicenseVerification.Invalid("Malformed license envelope.")
        } catch (_: IllegalArgumentException) {
            return OfflineLicenseVerification.Invalid("Malformed license envelope.")
        }

        if (envelope.signatureAlgorithm != SIGNATURE_ALGORITHM) {
            return OfflineLicenseVerification.Invalid("Unsupported signature algorithm.")
        }
        val publicKeyBase64 = publicKeysById[envelope.keyId]
            ?: return OfflineLicenseVerification.Invalid("Unknown license signing key.")
        val payload = envelope.payload
        if (payload.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            return OfflineLicenseVerification.Invalid("Unsupported license schema.")
        }
        if (payload.product != PRODUCT_ID) {
            return OfflineLicenseVerification.Invalid("License is for a different product.")
        }
        if (payload.licenseId.isBlank() || payload.licenseVersion < 1) {
            return OfflineLicenseVerification.Invalid("License payload is incomplete.")
        }
        if (payload.deviceBinding != null && payload.deviceBinding != expectedDeviceBinding) {
            return OfflineLicenseVerification.Invalid("License is bound to another installation.")
        }

        val notBeforeMs = payload.notBefore.toEpochMsOrNull()
            ?: return OfflineLicenseVerification.Invalid("License notBefore is invalid.")
        val expiresAtMs = payload.expiresAt?.toEpochMsOrNull()
            ?: payload.expiresAt?.let { return OfflineLicenseVerification.Invalid("License expiry is invalid.") }
        if (nowEpochMs < notBeforeMs) {
            return OfflineLicenseVerification.Invalid("License is not active yet.")
        }
        if (expiresAtMs != null && nowEpochMs > expiresAtMs) {
            return OfflineLicenseVerification.Invalid("License has expired.")
        }

        val capabilities = payload.capabilities.mapNotNull { raw ->
            runCatching { Capability.valueOf(raw) }.getOrNull()
        }.toSet()
        if (capabilities.size != payload.capabilities.distinct().size) {
            return OfflineLicenseVerification.Invalid("License includes an unknown capability.")
        }

        val canonicalPayload = canonicalPayload(payload)
        val verified = runCatching {
            val keyBytes = Base64.getDecoder().decode(publicKeyBase64)
            val publicKey = KeyFactory.getInstance(SIGNATURE_ALGORITHM)
                .generatePublic(X509EncodedKeySpec(keyBytes))
            val signatureBytes = Base64.getDecoder().decode(envelope.signature)
            Signature.getInstance(SIGNATURE_ALGORITHM).run {
                initVerify(publicKey)
                update(canonicalPayload.toByteArray(Charsets.UTF_8))
                verify(signatureBytes)
            }
        }.getOrDefault(false)
        if (!verified) {
            return OfflineLicenseVerification.Invalid("License signature is invalid.")
        }

        return OfflineLicenseVerification.Valid(
            envelopeJson = envelopeJson,
            payload = payload,
            capabilities = capabilities,
            expiresAtEpochMs = expiresAtMs
        )
    }

    fun canonicalPayload(payload: LicensePayload): String {
        val normalized = payload.copy(capabilities = payload.capabilities.distinct().sorted())
        return json.encodeToString(normalized)
    }

    private fun String.toEpochMsOrNull(): Long? {
        return runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()
    }

    private companion object {
        const val PRODUCT_ID = "loomora"
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val SIGNATURE_ALGORITHM = "Ed25519"
    }
}
