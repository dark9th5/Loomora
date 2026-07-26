package com.loomora

import com.loomora.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class LoomoraApplicationTest {

    @Test
    fun screenRoutes_areDefinedCorrectly() {
        assertEquals("home", Screen.Home.route)
        assertEquals("recording_detail/123", Screen.RecordingDetail.createRoute("123"))
        assertEquals("editor/456", Screen.Editor.createRoute("456"))
    }
}
