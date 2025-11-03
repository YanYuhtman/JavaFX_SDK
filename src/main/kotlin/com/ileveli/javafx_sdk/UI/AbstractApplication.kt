package com.ileveli.javafx_sdk.UI

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.application.Application


abstract class AbstractApplication : Application() {
    companion object{
        val Logger: KLogger by lazy{
            KotlinLogging.logger{}
        }
    }
    init {
         Thread.setDefaultUncaughtExceptionHandler {
                 thread, exception ->  Logger.error(exception){"$thread Unhandled exception!" }
             Logger.exit(1)
         }
    }
}


