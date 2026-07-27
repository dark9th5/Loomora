package com.loomora.core.audio.di

import com.loomora.core.audio.recovery.AndroidRecordingFileValidator
import com.loomora.core.audio.recovery.RecordingFileValidator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioRecoveryModule {
    @Binds
    @Singleton
    abstract fun bindRecordingFileValidator(
        validator: AndroidRecordingFileValidator
    ): RecordingFileValidator
}
