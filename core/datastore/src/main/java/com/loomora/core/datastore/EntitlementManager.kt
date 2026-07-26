package com.loomora.core.datastore

import com.loomora.core.model.Capability
import com.loomora.core.model.EntitlementStatus
import com.loomora.core.model.LicenseValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntitlementManager @Inject constructor() {

    private val _remainingTrialUses = MutableStateFlow(3)
    val remainingTrialUses: StateFlow<Int> = _remainingTrialUses.asStateFlow()

    private val _isProActive = MutableStateFlow(false)
    val isProActive: StateFlow<Boolean> = _isProActive.asStateFlow()

    fun getEntitlementStatus(): EntitlementStatus {
        return if (_isProActive.value) {
            EntitlementStatus.ProActive(expirationTimestamp = System.currentTimeMillis() + 365L * 24 * 3600 * 1000)
        } else {
            val uses = _remainingTrialUses.value
            if (uses > 0) EntitlementStatus.FreeTrial(uses) else EntitlementStatus.Expired
        }
    }

    fun isCapabilityGranted(capability: Capability): Boolean {
        if (capability == Capability.LOCAL_RECORDING) return true // Always free & unlimited!
        if (_isProActive.value) return true
        return _remainingTrialUses.value > 0
    }

    suspend fun consumeTrialUseOnSuccess() {
        if (!_isProActive.value && _remainingTrialUses.value > 0) {
            _remainingTrialUses.value -= 1
        }
    }

    suspend fun activateLicenseKey(key: String): LicenseValidationResult {
        val trimmed = key.trim()
        if (trimmed.length < 8) {
            return LicenseValidationResult.Invalid("License key must be at least 8 characters long.")
        }

        if (trimmed.contains("PRO", ignoreCase = true) || trimmed.startsWith("LM-")) {
            _isProActive.value = true
            return LicenseValidationResult.Valid(expiryTimestamp = System.currentTimeMillis() + 365L * 24 * 3600 * 1000)
        }

        return LicenseValidationResult.Invalid("Invalid license key format or key expired.")
    }
}
