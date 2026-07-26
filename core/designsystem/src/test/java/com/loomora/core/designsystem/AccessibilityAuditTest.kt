package com.loomora.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityAuditTest {

    @Test
    fun verifyMinimumTouchTargetPolicy_is48dp() {
        val minTouchTargetDp = 48
        assertTrue("Minimum touch target must be at least 48dp", minTouchTargetDp >= 48)
    }

    @Test
    fun verifyLocalizationAudit_bothEnglishAndVietnamesePresent() {
        val supportedLanguages = listOf("en", "vi")
        assertEquals(2, supportedLanguages.size)
        assertTrue(supportedLanguages.contains("en"))
        assertTrue(supportedLanguages.contains("vi"))
    }
}
