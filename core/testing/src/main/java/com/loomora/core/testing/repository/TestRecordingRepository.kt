package com.loomora.core.testing.repository

import com.loomora.core.model.Recording
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class TestRecordingRepository {

    private val recordingsFlow = MutableStateFlow<List<Recording>>(emptyList())

    fun getRecordings(): Flow<List<Recording>> = recordingsFlow

    fun getRecordingById(id: String): Flow<Recording?> =
        recordingsFlow.map { list -> list.find { it.id == id } }

    fun addRecording(recording: Recording) {
        recordingsFlow.value = recordingsFlow.value + recording
    }

    fun deleteRecording(id: String) {
        recordingsFlow.value = recordingsFlow.value.filterNot { it.id == id }
    }
}
