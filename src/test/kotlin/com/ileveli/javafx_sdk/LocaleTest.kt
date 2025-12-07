package com.ileveli.javafx_sdk

import com.ileveli.javafx_sdk.UI.AbstractApplication
import com.ileveli.javafx_sdk.UI.AbstractScene
import com.ileveli.javafx_sdk._examples_.LocalizedApplication
import com.ileveli.javafx_sdk._examples_.SimpleContextApplication
import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.control.Label
import org.junit.jupiter.api.Assertions
import java.util.Locale
import kotlin.test.Test

class LocaleTest {
    @Test
    fun test(){

        SimpleContextApplication.show({
            locale = Locale("en")
            Assertions.assertEquals("Hello English", this.getString("locale_hello"),"Default bundle is not loaded")
            locale = Locale("foo")
            Assertions.assertEquals("Hello Default", this.getString("locale_hello"),"Default bundle is not loaded")
            locale = Locale("ru")
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
    fun getBundleWithBlocking(){
        LocalizedApplication.show {
            assertLocale(this)
            Thread.sleep(500)
            when (this.locale.toLanguageTag()) {
                "en" -> locale = Locale("ru")
                "ru" -> locale = Locale("en")
            }

        }

//        assertLocale(LocalizedApplication.refToSelf)

    }
}