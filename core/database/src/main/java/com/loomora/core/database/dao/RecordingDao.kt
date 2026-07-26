package com.loomora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.loomora.core.database.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun getRecordingById(id: String): Flow<RecordingEntity?>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingByIdSync(id: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getActiveRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE deletedAt IS NULL AND isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashedRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE deletedAt IS NULL AND (title LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchRecordings(query: String): Flow<List<RecordingEntity>>

    @Query("UPDATE recordings SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long)

    @Query("UPDATE recordings SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteRecording(id: String, deletedAt: Long, updatedAt: Long)

    @Query("UPDATE recordings SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreRecording(id: String, updatedAt: Long)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingPermanently(id: String)
}
