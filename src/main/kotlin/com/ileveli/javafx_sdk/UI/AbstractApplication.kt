package com.ileveli.javafx_sdk.UI

import com.ileveli.javafx_sdk.utils.CustomCoroutineScope
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

val Logger: KLogger
    get() = KotlinLogging.logger( Thread.currentThread().stackTrace[2].className )

/**
 * Basic Application abstraction
 */
abstract class AbstractApplication : Application() {
    val appScope = CustomCoroutineScope()

    init {
         Thread.setDefaultUncaughtExceptionHandler {
                 thread, exception ->  Logger.error(exception){"$thread Unhandled exception!" }
             Logger.exit(1)
         }
    }

    override fun stop() {
        appScope.cancel()
    }
}


