package com.ileveli.javafx_sdk.utils

import com.ileveli.javafx_sdk.UI.AbstractApplication
import com.ileveli.javafx_sdk.UI.Logger
import com.ileveli.javafx_sdk.UI.iLeveliException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

/**
 * Custom [KSerializer] for `java.util.Locale` to enable its serialization and deserialization
 * using `kotlinx.serialization`. It serializes a `Locale` object to its language tag string.
 */
class LocalSerializable: KSerializer<Locale>{
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("locale", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Locale {
       return Locale(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Locale) {
        encoder.encodeString(value.toLanguageTag())
    }
}

/**
 * Data class to hold the application's locale settings.
 *
 * @property locale The currently selected [Locale] for the application. Defaults to [Locale.getDefault()].
 */
@Serializable
data class LocaleSettings(@Serializable(with = LocalSerializable::class) var locale: Locale = Locale.getDefault()){
    companion object{
        /** The file name used to store locale settings. */
        val fileName = "locale.json"
        /** The prefix for resource bundle file names (e.g., "i18n.Messages"). */
        val resourceFileNamePrefix = "i18n.Messages"
    }
}

/**
 * Manages localization for the application.
 *
 * This class handles loading and saving locale settings, resolving resource bundles,
 * and providing localized strings. It uses the application's coroutine scope for
 * asynchronous loading and saving of settings.
 *
 * @param appContext The [AbstractApplication] context, providing access to application-wide resources and scopes.
 */
class Localization constructor(val appContext: AbstractApplication) {
    private var _localSettings: LocaleSettings = LocaleSettings()
    private var _bundle: ResourceBundle? = null
    private val json = Json {prettyPrint = true}

    private lateinit var loadingMutex: Mutex
    init {
        loadingMutex = Mutex()
        loadSettings()
    }

    /**
     * Gets or sets the current [Locale] for the application.
     * When setting a new locale, it attempts to resolve the corresponding resource bundle
     * and saves the updated settings asynchronously.
     *
     * @throws iLeveliException if the resource bundle for the new locale cannot be found.
     */
    var locale: Locale
        get() = _localSettings.locale
        set(value) {
            var _value = value.toLanguageTag().let {
                if (it == Locale.ROOT.toLanguageTag()) {
                    Locale.getDefault()
                }
                value
            }
            resolveBundleOrThrow(_value)
            _localSettings.locale = _value
            saveSettings()
        }

    /**
     * The [ResourceBundle] currently in use for localization.
     * If the bundle is not yet loaded, it will attempt to load settings synchronously
     * (which should generally not happen if `loadSettings` is called on init).
     *
     * Returns `null` if no bundle can be resolved.
     */
    val bundle: ResourceBundle?
        get() {
            //in case loading takes place
            while (loadingMutex.isLocked)
                Thread.sleep(50)

            return _bundle ?: run {
                    //loading settings in main thread, it should never happen
                    loadSettingsRaw()
//                    _bundle?.let { it } ?: throw Exception("Unable to resolve resource bundle!")
                    _bundle
               }

        }

    /**
     * Retrieves a localized string for the given key. If the key is not found, it returns the key itself.
     *
     * @param key The key to look up in the resource bundle.
     * @return The localized string, or the key if not found and no default is provided.
     * @throws MissingResourceException if the key is not found and no default value is specified.
     */
    fun getString(key: String) = getString(key,key)

    /**
     * Retrieves a localized string for the given key.
     *
     * @param key The key to look up in the resource bundle.
     * @param default The default string to return if the key is not found. If `null`, a [MissingResourceException] is thrown.
     * @return The localized string, or the default value if the key is not found.
     * @throws MissingResourceException if the key is not found and `default` is `null`.
     */
    fun getString(key:String, default:String? = null) = _bundle?.let {
        return try {
            it.getString(key)
        }catch (em: MissingResourceException){
            default?: throw em
        }

    }?: run {
        Logger.warn { "The bundle isn't initialized, returning the key: ${key}" }
        return@run key
    }

    /**
     * Resolves the [ResourceBundle] for the given locale. If the bundle or its keyset is empty,
     * it throws an [iLeveliException].
     *
     * @param locale The [Locale] for which to resolve the bundle.
     * @throws iLeveliException if the resource bundle cannot be found or is empty for the given locale.
     */
    private fun resolveBundleOrThrow(locale: Locale?){
        var _locale = locale
        val bundle = locale?.let {
           var b = ResourceBundle.getBundle(LocaleSettings.resourceFileNamePrefix,_locale)
           if(b.locale.toLanguageTag() != _locale.toLanguageTag()){
                _locale =  Locale(_locale.toLanguageTag().split("_").first())
                b = ResourceBundle.getBundle(LocaleSettings.resourceFileNamePrefix,_locale)
            }
            return@let if(b.keySet().count() > 0) b else null
        }?: throw iLeveliException("Missing resource or keyset is empty for locale: ${_locale?.toLanguageTag() ?: "NULL"}")
        _bundle = bundle
    }

    /**
     * Attempts to resolve the [ResourceBundle] for the given locale. If an error occurs,
     * it falls back to the default [Locale.ROOT] bundle.
     *
     * @param locale The [Locale] for which to resolve the bundle.
     */
    private fun resolveBundle(locale: Locale?){
        try {
           resolveBundleOrThrow(locale)
        }catch (e: Exception){
            Logger.warn(e) { "Unable to load localization resource file:" +
                    "${LocaleSettings.resourceFileNamePrefix}_${_localSettings.locale}" +
                    " for locale: ${_localSettings.locale}." +
                    " Default will be loaded instead" }
            try {
                _bundle = ResourceBundle.getBundle(LocaleSettings.resourceFileNamePrefix, Locale.ROOT)
            }catch (e2: Exception){
                Logger.warn(e2) { "Unable to load default localization file: ${LocaleSettings.resourceFileNamePrefix} resource: ${_localSettings.locale}" }
            }
        }
    }

    /**
     * Loads the locale settings synchronously from the application data directory.
     * If loading fails, it uses default settings.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private fun loadSettingsRaw(){

        try {
            val file = File(appContext.getAppDataDirectory(appContext.packageName), LocaleSettings.fileName)
            Logger.info { "Loading language settings from: $file" }
            _localSettings = FileInputStream(file).use {
                return@use json.decodeFromStream<LocaleSettings>(it)
            }
        }catch (e: Exception){
            Logger.warn(e) { "Unable to load locale setting:  ${_localSettings}. Defaults will be used:"}
            resolveBundle(null)
        }
        resolveBundle(_localSettings.locale)

    }

    /**
     * Initiates asynchronous loading of locale settings in the application's coroutine scope.
     * It uses a mutex to prevent concurrent loading.
     */
    private fun loadSettings(){
        appContext.appScope.launch(Dispatchers.Default) {
           loadingMutex.withLock {
               loadSettingsRaw()
           }
        }
    }

    /**
     * Asynchronously saves the current locale settings to the application data directory.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private fun saveSettings(){
        appContext.appScope.launch(Dispatchers.IO) {
            try {
                val file = File(appContext.getAppDataDirectory(appContext.packageName), LocaleSettings.fileName)
                Logger.info { "Saving language settings to: $file" }
                FileOutputStream(file).use {
                    json.encodeToStream(_localSettings, it)
                }
            }catch (e: Exception){
                Logger.error(e) {"Error while saving localization settings"}
            }
        }
    }

}