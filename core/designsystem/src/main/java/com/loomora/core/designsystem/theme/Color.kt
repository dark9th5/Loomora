package com.loomora.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Brand Palette
val Indigo50 = Color(0xFFEEF2FF)
val Indigo100 = Color(0xFFE0E7FF)
val Indigo500 = Color(0xFF6366F1)
val Indigo600 = Color(0xFF4F46E5)
val Indigo900 = Color(0xFF1E1B4B)

val Teal50 = Color(0xFFF0FDFA)
val Teal100 = Color(0xFFCCFBF1)
val Teal500 = Color(0xFF0EA5E9)
val Teal700 = Color(0xFF0369A1)

val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate600 = Color(0xFF475569)
val Slate700 = Color(0xFF334155)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)

// Semantic Accents
val Red500 = Color(0xFFEF4444)
val Red100 = Color(0xFFFEE2E2)
val Green500 = Color(0xFF10B981)
val Green100 = Color(0xFFD1FAE5)
val Amber500 = Color(0xFFF59E0B)
val Amber100 = Color(0xFFFEF3C7)

@Immutable
data class LoomoraExtraColors(
    val recording: Color = Red500,
    val onRecording: Color = Color.White,
    val recordingContainer: Color = Red100,
    val success: Color = Green500,
    val warning: Color = Amber500,
    val waveformActive: Color = Indigo500,
    val waveformInactive: Color = Slate300
)

val LocalLoomoraExtraColors = staticCompositionLocalOf { LoomoraExtraColors() }
