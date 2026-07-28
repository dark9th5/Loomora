package com.loomora.core.network

import com.loomora.core.model.Entitlement
import com.loomora.core.model.EntitlementPlan

interface LoomoraNetworkDataSource {
    suspend fun verifyEntitlement(token: String): Entitlement
}

class DisabledLoomoraNetworkDataSource : LoomoraNetworkDataSource {
    override suspend fun verifyEntitlement(token: String): Entitlement {
        return Entitlement(plan = EntitlementPlan.GUEST_FREE, signedToken = null, isOfflineGraceValid = false)
    }
}
