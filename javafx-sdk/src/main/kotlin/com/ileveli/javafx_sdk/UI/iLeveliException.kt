package com.ileveli.javafx_sdk.UI

import java.lang.Exception

/**
 * A general-purpose custom exception for the iLeveli framework.
 * Used for framework-specific errors that don't fall into the more specific [InterfaceException] category.
 */
class iLeveliException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(
        message,
        cause,
        enableSuppression,
        writableStackTrace
    )


}