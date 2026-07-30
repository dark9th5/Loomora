package com.loomora.core.database.di

import android.content.Context
import androidx.room.Room
import com.loomora.core.database.LoomoraMigrations
import com.loomora.core.database.LoomoraDatabase
import com.loomora.core.database.dao.AnalysisJobDao
import com.loomora.core.database.dao.AudioSegmentDao
import com.loomora.core.database.dao.BackgroundJobDao
import com.loomora.core.database.dao.DiarizationDao
import com.loomora.core.database.dao.InsightDao
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.dao.OfflineModelDao
import com.loomora.core.database.dao.RecordingDao
import com.loomora.core.database.dao.TagDao
import com.loomora.core.database.dao.TranscriptDao
import com.loomora.core.database.dao.TrialOperationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLoomoraDatabase(
        @ApplicationContext context: Context
    ): LoomoraDatabase {
        return Room.databaseBuilder(
            context,
            LoomoraDatabase::class.java,
            "loomora.db"
        )
            .addMigrations(
                LoomoraMigrations.MIGRATION_1_2,
                LoomoraMigrations.MIGRATION_2_3,
                LoomoraMigrations.MIGRATION_3_4,
                LoomoraMigrations.MIGRATION_4_5,
                LoomoraMigrations.MIGRATION_5_6,
                LoomoraMigrations.MIGRATION_6_7,
                LoomoraMigrations.MIGRATION_7_8
            )
            .build()
    }

    @Provides
    fun provideRecordingDao(database: LoomoraDatabase): RecordingDao = database.recordingDao()

    @Provides
    fun provideAudioSegmentDao(database: LoomoraDatabase): AudioSegmentDao = database.audioSegmentDao()

    @Provides
    fun provideMarkerDao(database: LoomoraDatabase): MarkerDao = database.markerDao()

    @Provides
    fun provideTagDao(database: LoomoraDatabase): TagDao = database.tagDao()

    @Provides
    fun provideBackgroundJobDao(database: LoomoraDatabase): BackgroundJobDao = database.backgroundJobDao()

    @Provides
    fun provideOfflineModelDao(database: LoomoraDatabase): OfflineModelDao = database.offlineModelDao()

    @Provides
    fun provideAnalysisJobDao(database: LoomoraDatabase): AnalysisJobDao = database.analysisJobDao()

    @Provides
    fun provideTranscriptDao(database: LoomoraDatabase): TranscriptDao = database.transcriptDao()

    @Provides
    fun provideDiarizationDao(database: LoomoraDatabase): DiarizationDao = database.diarizationDao()

    @Provides
    fun provideInsightDao(database: LoomoraDatabase): InsightDao = database.insightDao()

    @Provides
    fun provideTrialOperationDao(database: LoomoraDatabase): TrialOperationDao = database.trialOperationDao()
}
