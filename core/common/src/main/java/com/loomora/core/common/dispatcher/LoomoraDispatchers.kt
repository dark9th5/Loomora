package com.loomora.core.common.dispatcher

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val loomoraDispatcher: LoomoraDispatchers)

enum class LoomoraDispatchers {
    Default,
    IO,
    Main
}
