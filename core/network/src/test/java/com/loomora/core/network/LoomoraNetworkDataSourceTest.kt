package com.loomora.core.network

import com.loomora.core.model.EntitlementPlan
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoomoraNetworkDataSourceTest {

    private val networkDataSource = DisabledLoomoraNetworkDataSource()

    @Test
    fun verifyEntitlement_anyTokenReturnsGuestFree() = runTest {
        val result = networkDataSource.verifyEntitlement("any-token")
        assertEquals(EntitlementPlan.GUEST_FREE, result.plan)
    }

    @Test
    fun verifyEntitlement_invalidTokenReturnsGuestFree() = runTest {
        val result = networkDataSource.verifyEntitlement("invalid-token")
        assertEquals(EntitlementPlan.GUEST_FREE, result.plan)
    }
}
