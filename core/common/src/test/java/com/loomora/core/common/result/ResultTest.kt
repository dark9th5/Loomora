package com.loomora.core.common.result

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun flowAsResult_emitsLoadingThenSuccess() = runTest {
        val flow = flowOf("Test Data")
        val results = flow.asResult().toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertEquals(Result.Success("Test Data"), results[1])
    }

    @Test
    fun flowAsResult_emitsLoadingThenError() = runTest {
        val exception = RuntimeException("Test Exception")
        val flow = flow<String> { throw exception }
        val results = flow.asResult().toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertEquals(Result.Error(exception), results[1])
    }
}
