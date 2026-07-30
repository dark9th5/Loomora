package com.loomora.core.offlineai.translation

import java.io.Closeable

sealed interface TranslationReadiness {
    data object Ready : TranslationReadiness
    data class ModelDownloadRequired(
        val sourceLanguageTag: String,
        val targetLanguageTag: String
    ) : TranslationReadiness
    data class Unsupported(val reason: String) : TranslationReadiness
}

interface LocalTranslationEngine : Closeable {
    suspend fun prepare(
        sourceLanguageTag: String,
        targetLanguageTag: String
    ): TranslationReadiness

    suspend fun translate(
        text: String,
        sourceLanguageTag: String,
        targetLanguageTag: String
    ): String
}

data class TranslationSelection(
    val enabled: Boolean = false,
    val sourceLanguageTag: String? = null,
    val targetLanguageTag: String? = null,
    val translateFinalOnly: Boolean = true
) {
    fun validated(): TranslationSelection {
        val source = normalize(sourceLanguageTag)
        val target = normalize(targetLanguageTag)
        require(!enabled || target != null) {
            "Target language is required when translation is enabled"
        }
        require(source == null || source != target) {
            "Source and target languages must differ"
        }
        return copy(sourceLanguageTag = source, targetLanguageTag = target)
    }

    private fun normalize(value: String?): String? =
        value?.trim()?.lowercase()?.substringBefore('-')?.takeIf(String::isNotBlank)
}
