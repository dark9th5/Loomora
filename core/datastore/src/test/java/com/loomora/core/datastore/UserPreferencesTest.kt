package com.loomora.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UserPreferencesTest {

    @Test
    fun userPreferences_defaultValuesAreValid() {
        val prefs = UserPreferences()
        assertEquals(DarkThemeConfig.FOLLOW_SYSTEM, prefs.darkThemeConfig)
        assertEquals("en", prefs.languageCode)
        assertFalse(prefs.hasCompletedOnboarding)
    }
}
