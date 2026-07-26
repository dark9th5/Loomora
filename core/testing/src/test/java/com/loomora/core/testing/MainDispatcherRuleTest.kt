package com.loomora.core.testing

import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class MainDispatcherRuleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun testDispatcher_isInitialized() {
        assertNotNull(mainDispatcherRule.testDispatcher)
    }
}
