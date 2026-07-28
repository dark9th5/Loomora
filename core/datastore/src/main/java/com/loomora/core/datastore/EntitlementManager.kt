package com.loomora.core.datastore

import com.loomora.core.model.Capability
import com.loomora.core.model.EntitlementDecisionCode
import com.loomora.core.model.EntitlementStatus
import com.loomora.core.model.LicenseValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntitlementManager @Inject constructor(
    private val entitlementRepository: EntitlementRepository
) {
    val remainingTrialUses: Flow<Int> = entitlementRepository.observeEntitlements().map { snapshot ->
        if (snapshot.licensedCapabilities.isEmpty()) 0 else Int.MAX_VALUE
    }

    val isProActive: Flow<Boolean> = entitlementRepository.observeEntitlements().map { snapshot ->
        snapshot.licensedCapabilities.isNotEmpty() && !snapshot.suspiciousClock
    }

    suspend fun getEntitlementStatus(): EntitlementStatus {
        val decision = entitlementRepository.canUse(Capability.SMART_INSIGHTS)
        return when (decision.code) {
            EntitlementDecisionCode.GRANTED_LICENSED -> {
                val snapshot = entitlementRepository.observeEntitlements().first()
                EntitlementStatus.ProActive(snapshot.licenseExpiresAtEpochMs ?: Long.MAX_VALUE)
            }
            EntitlementDecisionCode.GRANTED_FREE,
            EntitlementDecisionCode.GRANTED_TRIAL -> EntitlementStatus.FreeTrial(0)
            else -> EntitlementStatus.Expired
        }
    }

    suspend fun isCapabilityGranted(capability: Capability): Boolean {
        return entitlementRepository.canUse(capability).isGranted
    }

    suspend fun consumeTrialUseOnSuccess() = Unit

    suspend fun activateLicenseKey(envelopeJson: String): LicenseValidationResult {
        return entitlementRepository.importLicense(envelopeJson)
    }

    suspend fun removeLicense() {
        entitlementRepository.removeLicense()
    }
}
