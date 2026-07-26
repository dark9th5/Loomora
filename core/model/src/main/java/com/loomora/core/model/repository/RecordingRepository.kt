package com.loomora.core.model.repository

import com.loomora.core.model.Recording
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    fun getActiveRecordings(): Flow<List<Recording>>
    fun getFavoriteRecordings(): Flow<List<Recording>>
    fun getTrashedRecordings(): Flow<List<Recording>>
    fun getRecordingById(id: String): Flow<Recording?>
    fun searchRecordings(query: String): Flow<List<Recording>>
    suspend fun insertRecording(recording: Recording)
    suspend fun toggleFavorite(id: String, isFavorite: Boolean)
    suspend fun softDeleteRecording(id: String)
    suspend fun restoreRecording(id: String)
    suspend fun deleteRecordingPermanently(id: String)
}
