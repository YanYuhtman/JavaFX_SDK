package com.ileveli.javafx_sdk

import com.ileveli.javafx_sdk.UI.AbstractScene
import com.ileveli.javafx_sdk.UI.iLeveliException
import com.ileveli.javafx_sdk._examples_.LocalizedApplication
import com.ileveli.javafx_sdk._examples_.SimpleContextApplication
import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.control.Label
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows
import java.util.Locale
import java.util.MissingResourceException
import kotlin.test.Test

class LocaleTest {
    @Test
    fun test(){

        SimpleContextApplication.show({
            setLocale(Locale("en"),false)
            Assertions.assertEquals("Hello English", this.getString("locale_hello"),"Default bundle is not loaded")
            setLocale(Locale("ru"),false)
            Assertions.assertEquals("Здрасте", this.getString("locale_hello"),"Default bundle is not loaded")
            assertThrows<iLeveliException>("setLocale with invalid locale should throw exception", {
                setLocale(Locale("foo"), false)
            })
            Assertions.assertEquals("Здрасте", this.getString("locale_hello"),"Default bundle is not loaded")

            Platform.exit()
        })


    }
    @Test
    fun fxmlReplacement(){
        LocalizedApplication.show()
    }
    fun <T> assertLocale(_this:T) where T : LocalizedApplication{
        fun verifyLocale(text: String, assertionMessage: String) {
            when (_this.locale.toLanguageTag()) {
                "en" -> assert(text == "Hello English") { assertionMessage }
                "ru" -> assert(text == "Здрасте") { assertionMessage }
            }
        }

        val buttonText = (_this.stage.scene.lookup("#localized_button") as Button).text
        verifyLocale(buttonText, "Incorrect first order localization")

        val labelText = (_this.stage.scene.lookup("#localized_label") as Label).text
        verifyLocale(labelText, "Incorrect localized inject")

        val menu = (_this.stage.scene as AbstractScene<LocalizedApplication>).menuBar.menus[0]
        val menuText = menu.text
        verifyLocale(menuText, "Incorrect menu localization")

        val menuItemText = menu.items[0].text
        verifyLocale(menuItemText, "Incorrect menu localization")
    }
    @Test
    fun verifyBandleLocale(){
         LocalizedApplication.show {
            assertLocale(this)
        }
    }
    @Test
    fun verifyLocaleException(){
        SimpleContextApplication.show {
            assertThrows<iLeveliException>("Exception must be thrown",{
                setLocale(Locale("he"))
            })
            assertThrows<iLeveliException>("Exception must be thrown",{
                setLocale(Locale("en"))
            })
            Platform.exit()
        }
    }
}