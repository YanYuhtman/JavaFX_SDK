package com.ileveli.javafx_sdk.UI

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.application.Application

val Logger: KLogger
    get() = KotlinLogging.logger( Thread.currentThread().stackTrace[2].className )

/**
 * Basic Application abstraction
 */
abstract class AbstractApplication : Application() {
    init {
         Thread.setDefaultUncaughtExceptionHandler {
                 thread, exception ->  Logger.error(exception){"$thread Unhandled exception!" }
             Logger.exit(1)
         }
    }
}


