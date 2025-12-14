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

@Serializable
data class LocaleSettings(@Serializable(with = LocalSerializable::class) var locale: Locale = Locale.getDefault()){
    companion object{
        val fileName = "locale.json"
        val resourceFileNamePrefix = "i18n.Messages"
    }
}
class Localization constructor(val appContext: AbstractApplication) {
    private var _localSettings: LocaleSettings = LocaleSettings()
    private var _bundle: ResourceBundle? = null
    private val json = Json {prettyPrint = true}

    private lateinit var loadingMutex: Mutex
    init {
        loadingMutex = Mutex()
        loadSettings()
    }
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
    fun getString(key: String) = getString(key,key)
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
    private fun loadSettings(){
        appContext.appScope.launch(Dispatchers.Default) {
           loadingMutex.withLock {
               loadSettingsRaw()
           }
        }
    }
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