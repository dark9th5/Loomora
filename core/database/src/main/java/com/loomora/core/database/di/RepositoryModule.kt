package com.loomora.core.database.di

import com.loomora.core.database.repository.DefaultRecordingFileSystem
import com.loomora.core.database.repository.RecordingFileSystem
import com.loomora.core.database.repository.RecordingRepositoryImpl
import com.loomora.core.model.repository.RecordingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(
        impl: RecordingRepositoryImpl
    ): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindRecordingFileSystem(
        impl: DefaultRecordingFileSystem
    ): RecordingFileSystem
}
