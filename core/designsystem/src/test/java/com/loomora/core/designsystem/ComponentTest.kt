package com.loomora.core.designsystem

import com.loomora.core.designsystem.component.StatusPillType
import org.junit.Assert.assertEquals
import org.junit.Test

class ComponentTest {

    @Test
    fun statusPillType_valuesAreCorrect() {
        assertEquals("RECORDING", StatusPillType.RECORDING.name)
        assertEquals("PAUSED", StatusPillType.PAUSED.name)
        assertEquals("PREPARING", StatusPillType.PREPARING.name)
        assertEquals("FINALIZING", StatusPillType.FINALIZING.name)
    }
}
