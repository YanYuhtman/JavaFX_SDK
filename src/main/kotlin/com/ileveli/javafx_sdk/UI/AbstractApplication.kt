package com.ileveli.javafx_sdk.UI

import com.ileveli.javafx_sdk.utils.CustomCoroutineScope
import com.ileveli.javafx_sdk.utils.Localization
import io.github.oshai.kotlinlogging.KLogger
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.cancel
import java.io.File
import java.util.*
import kotlin.system.exitProcess

val Logger: KLogger
    get() = com.ileveli.javafx_sdk.utils.Logger

/**
 * Basic Application abstraction
 */
abstract class AbstractApplication : Application() {
    abstract val packageName:String

    val appScope = CustomCoroutineScope("AppScope ${this.javaClass.name}")
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

    val resourceBundle: ResourceBundle?
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

    fun getAppDataDirectory(packageName: String): File {
        val os = System.getProperty("os.name").lowercase()
        val dir = when {
            os.contains("win") -> File(System.getenv("APPDATA") ?: System.getProperty("user.home"), packageName)
            os.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support/$packageName")
            else -> File(System.getProperty("user.home"), ".$packageName") // Linux/Unix
        }
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

}


