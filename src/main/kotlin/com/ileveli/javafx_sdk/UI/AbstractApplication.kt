package com.ileveli.javafx_sdk.UI

import com.ileveli.javafx_sdk.utils.CustomCoroutineScope
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.application.Application
import kotlinx.coroutines.cancel
import kotlin.system.exitProcess

val Logger: KLogger
    get() = KotlinLogging.logger( Thread.currentThread().stackTrace[2].className )

/**
 * Basic Application abstraction
 */
abstract class AbstractApplication : Application() {
    val appScope = CustomCoroutineScope("AppScope")
    val localization: Localization = Localization(this)

    fun getString(key:String) = localization.getString(key)
    fun getString(key:String,default:String?) = localization.getString(key,default)
    init {
         Thread.setDefaultUncaughtExceptionHandler {
                 thread, exception ->  Logger.error(exception){"$thread Unhandled exception!" }
             Logger.exit(1)
             exitProcess(1)
         }
    }

    override fun stop() {
        appScope.cancel()
    }
}


