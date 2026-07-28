package com.loomora.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.loomora.core.database.dao.AudioSegmentDao
import com.loomora.core.database.dao.AnalysisJobDao
import com.loomora.core.database.dao.BackgroundJobDao
import com.loomora.core.database.dao.DiarizationDao
import com.loomora.core.database.dao.InsightDao
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.dao.OfflineModelDao
import com.loomora.core.database.dao.RecordingDao
import com.loomora.core.database.dao.TagDao
import com.loomora.core.database.dao.TranscriptDao
import com.loomora.core.database.dao.TrialOperationDao
import com.loomora.core.database.entity.AnalysisJobEntity
import com.loomora.core.database.entity.AudioSegmentEntity
import com.loomora.core.database.entity.BackgroundJobEntity
import com.loomora.core.database.entity.DiarizationRevisionEntity
import com.loomora.core.database.entity.InsightChunkCheckpointEntity
import com.loomora.core.database.entity.InsightRevisionEntity
import com.loomora.core.database.entity.EntitlementEntity
import com.loomora.core.database.entity.MarkerEntity
import com.loomora.core.database.entity.OfflineModelEntity
import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.database.entity.RecordingTagCrossRef
import com.loomora.core.database.entity.SpeakerAliasEntity
import com.loomora.core.database.entity.SpeakerTurnEntity
import com.loomora.core.database.entity.TagEntity
import com.loomora.core.database.entity.TrialUsageEntity
import com.loomora.core.database.entity.TrialOperationEntity
import com.loomora.core.database.entity.TranscriptRevisionEntity
import com.loomora.core.database.entity.TranscriptSegmentEntity

@Database(
    entities = [
        RecordingEntity::class,
        AudioSegmentEntity::class,
        MarkerEntity::class,
        TagEntity::class,
        RecordingTagCrossRef::class,
        BackgroundJobEntity::class,
        OfflineModelEntity::class,
        AnalysisJobEntity::class,
        TranscriptRevisionEntity::class,
        TranscriptSegmentEntity::class,
        DiarizationRevisionEntity::class,
        SpeakerTurnEntity::class,
        SpeakerAliasEntity::class,
        InsightRevisionEntity::class,
        InsightChunkCheckpointEntity::class,
        EntitlementEntity::class,
        TrialUsageEntity::class,
        TrialOperationEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class LoomoraDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun audioSegmentDao(): AudioSegmentDao
    abstract fun markerDao(): MarkerDao
    abstract fun tagDao(): TagDao
    abstract fun backgroundJobDao(): BackgroundJobDao
    abstract fun offlineModelDao(): OfflineModelDao
    abstract fun analysisJobDao(): AnalysisJobDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun diarizationDao(): DiarizationDao
    abstract fun insightDao(): InsightDao
    abstract fun trialOperationDao(): TrialOperationDao
}
