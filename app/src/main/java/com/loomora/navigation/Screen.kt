package com.loomora.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Tutorial : Screen("tutorial")
    data object Home : Screen("home")
    data object Recorder : Screen("recorder?mode={mode}") {
        fun createRoute(mode: String? = null) = if (mode == null) "recorder" else "recorder?mode=$mode"
    }
    data object Library : Screen("library")
    data object RecordingDetail : Screen("recording_detail/{recordingId}") {
        fun createRoute(recordingId: String) = "recording_detail/$recordingId"
    }
    data object Editor : Screen("editor/{recordingId}") {
        fun createRoute(recordingId: String) = "editor/$recordingId"
    }
    data object Settings : Screen("settings")
    data object Subscription : Screen("subscription")
}
