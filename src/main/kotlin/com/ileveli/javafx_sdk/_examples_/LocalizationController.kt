package com.ileveli.javafx_sdk._examples_

import com.ileveli.javafx_sdk.UI.AbstractController
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import java.net.URL
import java.util.Locale
import java.util.ResourceBundle

class LocalizationController : AbstractController<LocalizedApplication>() {
    lateinit var localized_button: Button

    @FXML
    private lateinit var welcomeText: Label

    @FXML
    private lateinit var injected:AnchorPane
    @FXML
    private fun onHelloButtonClick() {
        welcomeText.text = "Welcome to JavaFX Application!"
        when (appContext.locale.toLanguageTag()) {
            "en" -> appContext.setLocale(Locale("ru"))
            "ru" -> appContext.setLocale(Locale("en"))
        }
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {

    }
}