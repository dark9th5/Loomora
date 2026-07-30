package com.loomora.core.offlineai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SingleEntryResourceCacheTest {
    @Test
    fun sameKey_reusesResource() {
        var creations = 0
        val cache = SingleEntryResourceCache<String, Any> { }
        val first = cache.getOrCreate("model-a/en/2") { creations++; Any() }
        val second = cache.getOrCreate("model-a/en/2") { creations++; Any() }

        assertSame(first, second)
        assertEquals(1, creations)
    }

    @Test
    fun changedKey_releasesOldResourceAndClearReleasesCurrent() {
        val released = mutableListOf<String>()
        val cache = SingleEntryResourceCache<String, String> { released += it }
        cache.getOrCreate("model-a") { "recognizer-a" }
        cache.getOrCreate("model-b") { "recognizer-b" }
        cache.clear()

        assertEquals(listOf("recognizer-a", "recognizer-b"), released)
    }
}
