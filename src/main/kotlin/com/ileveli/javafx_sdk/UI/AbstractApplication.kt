package com.ileveli.javafx_sdk.UI

import com.ileveli.javafx_sdk.utils.CustomCoroutineScope
import com.ileveli.javafx_sdk.utils.Localization
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.application.Application
import javafx.scene.Scene
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
    protected lateinit var  _localization: Localization

    var locale: Locale
        get() = _localization.locale
        private set(value) {_localization.locale = value}
    fun setLocale(locale: Locale, resetUI: Boolean = true){
        this.locale = locale
        if(!resetUI)
            return
        restartUI()
    }

    val resourceBundle: ResourceBundle
        get() = _localization.bundle

    fun getString(key:String) = _localization.getString(key)
    fun getString(key:String,default:String?) = _localization.getString(key,default)

    init {
        _localization = Localization(this)
         Thread.setDefaultUncaughtExceptionHandler {
                 thread, exception ->  Logger.error(exception){"$thread Unhandled exception!" }
             Logger.exit(1)
             exitProcess(1)
         }
    }
    protected open fun mainSceneResolver(stage: Stage): Scene? = null
    override fun start(primaryStage: Stage) {
        _primaryStage = primaryStage
        mainSceneResolver(primaryStage)?.let {
            primaryStage.scene = it
        }

    }

    override fun stop() {
        appScope.cancel()
    }
    fun restartUI() {
        _primaryStage?.let {pStage ->
            this.mainSceneResolver(pStage)?.let {
                pStage.scene = it
            }?: throw iLeveliException("In order to use restartUI one must override mainSceneResolver() and provide a Scene")
        }?: throw iLeveliException("Primary stage is not attached, make sure you've called super.start()")
    }
}


