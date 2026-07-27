package com.loomora.core.offlineai.di

import android.content.Context
import com.loomora.core.offlineai.LocalTranscriptionEngine
import com.loomora.core.offlineai.SherpaOnnxTranscriptionEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OfflineAiModule {
    @Provides
    @Singleton
    fun provideOfflineAiJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        }
    }

    @Provides
    @Singleton
    fun provideLocalTranscriptionEngine(
        @ApplicationContext context: Context
    ): LocalTranscriptionEngine {
        return SherpaOnnxTranscriptionEngine(context)
    }
}
