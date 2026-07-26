package com.loomora.core.network

import com.loomora.core.model.AiJobStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPipelineEngineTest {

    @Test
    fun processAudio_withoutConsent_requiresConsent() = runTest {
        val provider = DefaultAiProvider()
        val engine = AiPipelineEngine(provider, provider)

        engine.processAudio("file:///test.m4a", hasUserConsented = false)
        assertEquals(AiJobStatus.ConsentRequired, engine.jobStatus.value)
    }

    @Test
    fun processAudio_withConsentNoConfig_failsGracefullyWithRetryableFlag() = runTest {
        val provider = DefaultAiProvider()
        val engine = AiPipelineEngine(provider, provider)

        engine.processAudio("file:///test.m4a", hasUserConsented = true)
        val status = engine.jobStatus.value
        assertTrue(status is AiJobStatus.Failed)
        val failedState = status as AiJobStatus.Failed
        assertEquals(true, failedState.isRetryable)
    }
}
