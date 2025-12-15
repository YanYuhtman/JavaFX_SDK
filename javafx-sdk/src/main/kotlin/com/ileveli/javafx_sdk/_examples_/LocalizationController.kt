package com.ileveli.javafx_sdk._examples_

import com.ileveli.javafx_sdk.UI.AbstractController
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import java.util.Locale

/**
 * An example controller demonstrating localization functionality within a JavaFX application.
 * This controller manages a simple UI with a button that changes the application's locale
 * and updates a welcome text label accordingly.
 *
 * It extends [AbstractController] to gain access to the application context and its localization features.
 */
class LocalizationController : AbstractController<LocalizedApplication>() {
    /**
     * A button that triggers a locale change when clicked. This button is likely linked
     * via FXML or programmatically by ID.
     */
    lateinit var localized_button: Button

    /**
     * The [Label] displaying a welcome message, whose text is updated based on the current locale.
     * This field is injected via FXML.
     */
    @FXML
    private lateinit var welcomeText: Label

    /**
     * An [AnchorPane] that is part of the FXML layout, potentially used for injecting other UI elements.
     * This field is injected via FXML.
     */
    @FXML
    private lateinit var injected:AnchorPane

    /**
     * Handles the action when the "Hello" button is clicked.
     * This method toggles the application's locale between English ("en") and Russian ("ru")
     * and updates the `welcomeText` label to reflect the change. The application's UI is
     * then restarted to apply the new locale.
     */
    @FXML
    private fun onHelloButtonClick() {
        welcomeText.text = "Welcome to JavaFX Application!"
        when (appContext.locale.toLanguageTag()) {
            "en" -> appContext.setLocale(Locale("ru"))
            "ru" -> appContext.setLocale(Locale("en"))
        }
    }

    /**
     * Lifecycle method called after the controller has been initialized with the application context.
     * In this example, no specific initialization is required beyond what the framework provides.
     *
     * @param appContext The initialized [LocalizedApplication] context.
     */
    override fun onContextInitialized(appContext: LocalizedApplication) {
    }
}