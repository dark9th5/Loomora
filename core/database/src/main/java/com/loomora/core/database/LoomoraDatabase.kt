package com.loomora.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.loomora.core.database.dao.AudioSegmentDao
import com.loomora.core.database.dao.BackgroundJobDao
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.dao.RecordingDao
import com.loomora.core.database.dao.TagDao
import com.loomora.core.database.entity.AudioSegmentEntity
import com.loomora.core.database.entity.BackgroundJobEntity
import com.loomora.core.database.entity.EntitlementEntity
import com.loomora.core.database.entity.MarkerEntity
import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.database.entity.RecordingTagCrossRef
import com.loomora.core.database.entity.TagEntity
import com.loomora.core.database.entity.TrialUsageEntity

@Database(
    entities = [
        RecordingEntity::class,
        AudioSegmentEntity::class,
        MarkerEntity::class,
        TagEntity::class,
        RecordingTagCrossRef::class,
        BackgroundJobEntity::class,
        EntitlementEntity::class,
        TrialUsageEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class LoomoraDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun audioSegmentDao(): AudioSegmentDao
    abstract fun markerDao(): MarkerDao
    abstract fun tagDao(): TagDao
    abstract fun backgroundJobDao(): BackgroundJobDao
}
