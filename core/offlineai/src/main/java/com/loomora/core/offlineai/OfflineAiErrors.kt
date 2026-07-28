package com.loomora.core.offlineai

sealed class OfflineAiException(message: String) : Exception(message) {
    data object ModelMissing : OfflineAiException("Required offline model is missing.")
    data object ModelChecksumMismatch : OfflineAiException("Imported model checksum does not match manifest.")
    data object DeviceIncompatible : OfflineAiException("Device is incompatible with the imported model.")
    data object ModelFileMissing : OfflineAiException("Installed model file is missing.")
    data object FileMissing : OfflineAiException("Audio source file is missing.")
    data object FileCorrupt : OfflineAiException("Audio source file is corrupt or unsupported.")
    data object ImportInterrupted : OfflineAiException("Model import was interrupted.")
    data object ProcessingCancelled : OfflineAiException("Offline processing was cancelled.")
    data object ModelInitializationFailed : OfflineAiException("Offline model could not be initialized.")
    data object InsightParseFailed : OfflineAiException("Local insight output could not be parsed or validated.")
    data object InsightSemanticInvalid : OfflineAiException("Local insight output is semantically invalid.")
    data object ProcessingUnavailable : OfflineAiException("Offline processing foundation is not fully installed yet.")
}
