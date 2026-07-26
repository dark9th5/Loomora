package com.loomora.feature.settings

import com.loomora.core.datastore.DarkThemeConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    @Test
    fun settingsUiState_defaultValuesAreValid() {
        val state = SettingsUiState()
        assertEquals(DarkThemeConfig.FOLLOW_SYSTEM, state.darkThemeConfig)
        assertEquals("en", state.languageCode)
    }
}
