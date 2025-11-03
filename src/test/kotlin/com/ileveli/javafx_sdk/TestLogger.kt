package com.ileveli.javafx_sdk

import com.ileveli.javafx_sdk.UI.AbstractApplication
import com.ileveli.javafx_sdk.UI.Logger
import javafx.application.Platform
import javafx.stage.Stage
import org.junit.jupiter.api.Test
import org.junit.platform.commons.annotation.Testable

@Testable
class TestLogger : AbstractApplication(){

    override fun start(primaryStage: Stage?) {
        Logger.info { "Info message" }
        Logger.debug { "Debug message" }
        Logger.error(Exception("Test error")) { "Error message" }
        Platform.exit()
    }

    @Test
    fun testLogger(){
        launch(TestLogger::class.java)
    }

}