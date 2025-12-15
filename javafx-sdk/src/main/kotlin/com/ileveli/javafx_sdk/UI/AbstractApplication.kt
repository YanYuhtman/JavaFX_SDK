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

/**
 * Provides access to the application-wide logger.
 */
val Logger: KLogger
    get() = com.ileveli.javafx_sdk.utils.Logger

/**
 * An abstract base class for JavaFX applications, providing common infrastructure for dependency injection,
 * coroutine management, localization, and scene management.
 *
 * This class sets up a coroutine scope (`appScope`) that is cancelled when the application stops.
 * It also initializes a default exception handler to log any uncaught exceptions and terminate the application.
 *
 * Subclasses must implement [packageName] and should override [mainSceneResolver] to define the primary scene.
 */
abstract class AbstractApplication : Application() {
    /**
     * A unique identifier for the application package, used for creating application-specific
     * directories and locating resources.
     */
    abstract val packageName:String

    /**
     * A [CustomCoroutineScope] tied to the application's lifecycle.
     * This scope is automatically cancelled in the [stop] method, ensuring all launched coroutines are cleaned up properly.
     */
    val appScope = CustomCoroutineScope("AppScope ${this.javaClass.name}")
    private var _primaryStage: Stage? = null
    /**
     * The localization manager for the application.
     */
    protected lateinit var  _localization: Localization

    /**
     * Gets or sets the current locale for the application.
     * Changing the locale will update the underlying [Localization] instance.
     * @see setLocale
     */
    var locale: Locale
        get() = _localization.locale
        private set(value) {_localization.locale = value}
    /**
     * Sets the application's locale and optionally restarts the user interface to apply the changes.
     *
     * @param locale The new [Locale] to set.
     * @param resetUI If `true`, the UI will be restarted by calling [restartUI]. Defaults to `true`.
     */
    fun setLocale(locale: Locale, resetUI: Boolean = true){
        this.locale = locale
        if(!resetUI)
            return
        restartUI()
    }

    /**
     * The [ResourceBundle] containing the localized strings for the current [locale].
     * Returns `null` if the bundle cannot be found.
     */
    val resourceBundle: ResourceBundle?
        get() = _localization.bundle

    /**
     * Retrieves a localized string for the given key.
     *
     * @param key The key for the desired string.
     * @return The localized string.
     * @throws MissingResourceException if the key is not found.
     */
    fun getString(key:String) = _localization.getString(key)
    /**
     * Retrieves a localized string for the given key, returning a default value if the key is not found.
     *
     * @param key The key for the desired string.
     * @param default The default value to return if the key is not found.
     * @return The localized string or the default value.
     */
    fun getString(key:String,default:String?) = _localization.getString(key,default)

    init {
        _localization = Localization(this)
         Thread.setDefaultUncaughtExceptionHandler {
                 thread, exception ->  Logger.error(exception){"$thread Unhandled exception!" }
             Logger.exit(1)
             exitProcess(1)
         }
    }
    /**
     * Resolves and creates the main scene for the application.
     * Subclasses should override this method to provide the primary [Scene].
     *
     * @param stage The primary [Stage] of the application.
     * @return The main [Scene] to be displayed, or `null` if no scene is created.
     */
    protected open fun mainSceneResolver(stage: Stage): Scene? = null
    /**
     * The main entry point for the JavaFX application.
     * It sets the primary stage and displays the initial scene resolved by [mainSceneResolver].
     *
     * @param primaryStage The primary [Stage] for this application.
     */
    override fun start(primaryStage: Stage) {
        _primaryStage = primaryStage
        mainSceneResolver(primaryStage)?.let {
            primaryStage.scene = it
        }

    }

    /**
     * Called when the application is stopped.
     * This method cancels the [appScope], terminating all related coroutines.
     */
    override fun stop() {
        appScope.cancel()
    }
    /**
     * Restarts the user interface by re-resolving and setting the main scene.
     * This is useful for applying theme or locale changes that require a full UI refresh.
     *
     * @throws iLeveliException if this method is called before the primary stage is available or
     * if [mainSceneResolver] is not overridden to provide a scene.
     */
    fun restartUI() {
        _primaryStage?.let {pStage ->
            this.mainSceneResolver(pStage)?.let {
                pStage.scene = it
            }?: throw iLeveliException("In order to use restartUI one must override mainSceneResolver() and provide a Scene")
        }?: throw iLeveliException("Primary stage is not attached, make sure you've called super.start()")
    }

    /**
     * Returns the platform-specific directory for storing application data.
     * The location varies depending on the operating system:
     * - **Windows:** `%APPDATA%\<packageName>`
     * - **macOS:** `~/Library/Application Support/<packageName>`
     * - **Linux/Unix:** `~/.<packageName>`
     *
     * The directory (and any necessary parent directories) will be created if it does not exist.
     *
     * @param packageName The name of the application, used to create the directory.
     * @return A [File] object representing the application data directory.
     */
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


