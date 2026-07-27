package com.loomora.core.offlineai

import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineEngineLifecycleManager @Inject constructor() : Closeable {
    private val activeEngine = AtomicReference<Closeable?>(null)

    fun register(engine: Closeable) {
        activeEngine.getAndSet(engine)?.close()
    }

    override fun close() {
        activeEngine.getAndSet(null)?.close()
    }
}
