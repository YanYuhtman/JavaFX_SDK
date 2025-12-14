package com.ileveli.javafx_sdk.utils

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

val Logger: KLogger
    get() = KotlinLogging.logger( Thread.currentThread().stackTrace[2].className )

val DEBUG: Boolean
    get() = System.getProperty("debug").toBoolean() || System.getenv("DEBUG").toBoolean()