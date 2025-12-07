package com.ileveli.javafx_sdk

import com.ileveli.javafx_sdk.UI.Logger
import com.ileveli.javafx_sdk._examples_.LocalizedApplication
import javafx.application.Application


fun main() {
    try {
        Application.launch(LocalizedApplication::class.java)
    }catch (e: Exception){
        Logger.error(e) { "Finalization errors"}
    }


}
