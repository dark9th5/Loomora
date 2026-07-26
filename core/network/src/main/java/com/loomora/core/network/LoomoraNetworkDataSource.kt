package com.loomora.core.network

import com.loomora.core.model.Entitlement
import com.loomora.core.model.EntitlementPlan

interface LoomoraNetworkDataSource {
    suspend fun verifyEntitlement(token: String): Entitlement
}

class FakeLoomoraNetworkDataSource : LoomoraNetworkDataSource {
    override suspend fun verifyEntitlement(token: String): Entitlement {
        return if (token == "valid-pro-token") {
            Entitlement(plan = EntitlementPlan.PRO, signedToken = token, isOfflineGraceValid = true)
        } else {
            Entitlement(plan = EntitlementPlan.GUEST_FREE, signedToken = null, isOfflineGraceValid = false)
        }
    }
}
