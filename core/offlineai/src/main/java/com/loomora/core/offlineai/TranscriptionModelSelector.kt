package com.loomora.core.offlineai

/**
 * Selects a READY transcription model compatible with the requested language.
 * It prevents English jobs from silently using the Vietnamese-only Zipformer.
 */
object TranscriptionModelSelector {
    fun select(
        requestedLanguageTag: String?,
        records: List<OfflineModelRecord>,
        manuallySelectedId: String? = null
    ): OfflineModelRecord? {
        val requested = normalizeLanguage(requestedLanguageTag)
        val ready = records.filter {
            it.manifest.capability == ModelCapability.TRANSCRIPTION &&
                it.state == ModelInstallState.READY
        }

        if (!manuallySelectedId.isNullOrBlank()) {
            return ready.firstOrNull { record ->
                record.manifest.id == manuallySelectedId &&
                    supports(record, requested)
            }
        }

        if (requested == null) {
            return ready.firstOrNull { "multilingual" in normalizedLanguages(it) }
                ?: ready.firstOrNull()
        }

        val specialized = ready.firstOrNull { record ->
            val languages = normalizedLanguages(record)
            requested in languages && "multilingual" !in languages && languages.size == 1
        }
        if (specialized != null) return specialized

        return ready.firstOrNull { record ->
            val languages = normalizedLanguages(record)
            requested in languages && "multilingual" in languages
        } ?: ready.firstOrNull { supports(it, requested) }
    }

    fun supports(record: OfflineModelRecord, requestedLanguageTag: String?): Boolean {
        val requested = normalizeLanguage(requestedLanguageTag) ?: return true
        val languages = normalizedLanguages(record)
        return requested in languages || "multilingual" in languages
    }

    private fun normalizedLanguages(record: OfflineModelRecord): Set<String> =
        record.manifest.supportedLanguages.mapNotNull(::normalizeLanguageToken).toSet()

    private fun normalizeLanguage(value: String?): String? {
        val raw = value?.trim()?.lowercase()?.substringBefore('-')
        return when (raw) {
            null, "", "auto", "automatic", "detect", "multilingual" -> null
            "vi", "vie", "vietnamese", "tiếng việt", "tieng viet" -> "vi"
            "en", "eng", "english" -> "en"
            else -> raw
        }
    }

    private fun normalizeLanguageToken(value: String): String? =
        when (val raw = value.trim().lowercase().substringBefore('-')) {
            "", "auto", "automatic", "detect" -> null
            "vie", "vietnamese", "tiếng việt", "tieng viet" -> "vi"
            "eng", "english" -> "en"
            else -> raw
        }
}
