package com.loomora.core.offlineai.di

import android.content.Context
import com.loomora.core.database.dao.TrialOperationDao
import com.loomora.core.offlineai.FallbackMeetingInsightEngine
import com.loomora.core.offlineai.HeuristicMeetingInsightEngine
import com.loomora.core.offlineai.LocalDiarizationEngine
import com.loomora.core.offlineai.LocalMeetingInsightEngine
import com.loomora.core.offlineai.LocalTranscriptionEngine
import com.loomora.core.offlineai.LlamaCppMeetingInsightEngine
import com.loomora.core.offlineai.LiteRtLmMeetingInsightEngine
import com.loomora.core.offlineai.LlamaCppRuntime
import com.loomora.core.offlineai.DurableTrialReservationPort
import com.loomora.core.offlineai.SherpaOnnxDiarizationEngine
import com.loomora.core.offlineai.SherpaOnnxTranscriptionEngine
import com.loomora.core.offlineai.TrialReservationPort
import com.loomora.core.offlineai.UnavailableLlamaCppRuntime
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

    @Provides
    @Singleton
    fun provideLocalDiarizationEngine(
        @ApplicationContext context: Context
    ): LocalDiarizationEngine {
        return SherpaOnnxDiarizationEngine(context)
    }

    @Provides
    @Singleton
    fun provideLocalMeetingInsightEngine(
        heuristic: HeuristicMeetingInsightEngine,
        liteRtLm: LiteRtLmMeetingInsightEngine,
        llamaCpp: LlamaCppMeetingInsightEngine
    ): LocalMeetingInsightEngine {
        return FallbackMeetingInsightEngine(heuristic, liteRtLm, llamaCpp)
    }

    @Provides
    @Singleton
    fun provideLlamaCppRuntime(): LlamaCppRuntime = UnavailableLlamaCppRuntime()

    @Provides
    @Singleton
    fun provideTrialReservationPort(
        trialOperationDao: TrialOperationDao
    ): TrialReservationPort = DurableTrialReservationPort(trialOperationDao)
}
