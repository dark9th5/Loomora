package com.loomora.core.audio.di

import com.loomora.core.audio.editor.AndroidAudioOutputMetadataReader
import com.loomora.core.audio.editor.AudioEditEngine
import com.loomora.core.audio.editor.AudioOutputMetadataReader
import com.loomora.core.audio.editor.Media3AudioEditEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioEditModule {

    @Binds
    @Singleton
    abstract fun bindAudioEditEngine(
        impl: Media3AudioEditEngine
    ): AudioEditEngine

    @Binds
    @Singleton
    abstract fun bindAudioOutputMetadataReader(
        impl: AndroidAudioOutputMetadataReader
    ): AudioOutputMetadataReader
}
