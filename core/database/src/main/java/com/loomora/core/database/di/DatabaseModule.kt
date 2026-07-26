package com.loomora.core.database.di

import android.content.Context
import androidx.room.Room
import com.loomora.core.database.LoomoraDatabase
import com.loomora.core.database.dao.AudioSegmentDao
import com.loomora.core.database.dao.BackgroundJobDao
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.dao.RecordingDao
import com.loomora.core.database.dao.TagDao
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
        ).build()
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
}
