package com.loomora.core.designsystem

import androidx.compose.ui.graphics.Color
import com.loomora.core.designsystem.theme.Indigo500
import com.loomora.core.designsystem.theme.LoomoraExtraColors
import com.loomora.core.designsystem.theme.Red500
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {

    @Test
    fun indigo500Color_valueIsCorrect() {
        assertEquals(Color(0xFF6366F1), Indigo500)
    }

    @Test
    fun loomoraExtraColors_defaultValuesAreValid() {
        val extraColors = LoomoraExtraColors()
        assertEquals(Red500, extraColors.recording)
    }
}
