package com.ileveli.javafx_sdk

import com.ileveli.javafx_sdk._examples_.LocalizedApplication
import com.ileveli.javafx_sdk.utils.Logger
import javafx.application.Application


fun main() {
    try {
        Application.launch(LocalizedApplication::class.java)
    }catch (e: Exception){
        Logger.error(e) { "Finalization errors"}
    }


}
