package com.ileveli.javafx_sdk

import javafx.application.Platform
import org.junit.jupiter.api.Assertions
import java.util.Locale
import kotlin.test.Test

class LocaleTest {
    @Test
    fun test(){

        SimpleContextApplication.show({
            localization.locale = Locale("en")
            Assertions.assertEquals("Hello English", this.getString("hello"),"Default bundle is not loaded")
            localization.locale = Locale("foo")
            Assertions.assertEquals("Hello Default", this.getString("hello"),"Default bundle is not loaded")
            localization.locale = Locale("ru")
            Assertions.assertEquals("Здрасте", this.getString("hello"),"Default bundle is not loaded")

            Platform.exit()
        })


    }
}