package com.ileveli.javafx_sdk.utils

import com.ileveli.javafx_sdk.UI.AbstractApplication
import com.ileveli.javafx_sdk.UI.Logger
import com.ileveli.javafx_sdk.UI.iLeveliException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        val resourceFileNamePrefix = "Messages"
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
            if(value == Locale.ROOT && value == Locale.getDefault()){
                _localSettings.locale = value
                return
            }
            resolveBundleOrThrow(value)
            _localSettings.locale = value
            saveSettings()
        }
    val bundle: ResourceBundle
        get() {
            //in case loading takes place
            while (loadingMutex.isLocked)
                Thread.sleep(50)

            return _bundle?.let { it } ?: run {
                    //loading settings in main thread, it should never happen
                    loadSettingsRaw()
                    _bundle?.let { it } ?: throw Exception("Unable to resolve resource bundle!")
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
        val bundle = locale?.let {
            ResourceBundle.getBundle(LocaleSettings.resourceFileNamePrefix,locale)
        }
        if(bundle?.locale != locale && _bundle?.locale != Locale.ROOT)
            throw iLeveliException("Missing resource for locale: ${locale?.toLanguageTag() ?: "NULL"}")
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

    private fun loadSettingsRaw(){

        try {
            _localSettings = FileInputStream(LocaleSettings.fileName).use {
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
    private fun saveSettings(){
        appContext.appScope.launch(Dispatchers.IO) {
            try {
                FileOutputStream(LocaleSettings.fileName).use {
                    json.encodeToStream(_localSettings, it)
                }
            }catch (e: Exception){
                Logger.error(e) {"Error while saving localization settings"}
            }
        }
    }

}