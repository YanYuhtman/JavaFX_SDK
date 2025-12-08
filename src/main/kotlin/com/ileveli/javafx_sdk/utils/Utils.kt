package com.ileveli.javafx_sdk.utils

val DEBUG: Boolean
    get() = System.getProperty("debug").toBoolean() || System.getenv("DEBUG").toBoolean()