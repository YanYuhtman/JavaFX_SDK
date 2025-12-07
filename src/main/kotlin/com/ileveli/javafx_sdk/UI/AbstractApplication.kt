package com.ileveli.javafx_sdk.UI

import com.ileveli.javafx_sdk.utils.CustomCoroutineScope
import com.ileveli.javafx_sdk.utils.Localization
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.application.Application
import javafx.application.Platform
import javafx.stage.Stage
import kotlinx.coroutines.cancel
import java.util.*
import kotlin.system.exitProcess

val Logger: KLogger
    get() = KotlinLogging.logger( Thread.currentThread().stackTrace[2].className )

/**
 * Basic Application abstraction
 */
abstract class AbstractApplication : Application() {
    val appScope = CustomCoroutineScope("AppScope")
    private var _primaryStage: Stage? = null
    protected val _localization: Localization = Localization(this)

    var locale: Locale
        get() = _localization.locale
        set(value) {
            _localization.locale = value
            _primaryStage?.let {
                it.close()
                //TODO: Find solution for reloading scene for locale change
                it.show()
            }?: Logger.warn { "Unable to relead primary stage! Make sure 'super.start(primaryStage)' been called" }
        }
    val resourceBundle: ResourceBundle
        get() = _localization.bundle

    fun getString(key:String) = _localization.getString(key)
    fun getString(key:String,default:String?) = _localization.getString(key,default)

    init {
         Thread.setDefaultUncaughtExceptionHandler {
                 thread, exception ->  Logger.error(exception){"$thread Unhandled exception!" }
             Logger.exit(1)
             exitProcess(1)
         }
    }

    override fun start(primaryStage: Stage?) {
        _primaryStage = primaryStage
    }

    override fun stop() {
        appScope.cancel()
    }

    fun restartWithSystemExit() {
        Platform.exit()
        Thread(Runnable {
            try {
                Thread.sleep(1000)
                Application.launch(this::class.java)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }).start()
    }
}


