package com.ileveli.javafx_sdk.UI

import java.lang.Exception

/**
 * A custom exception for errors related to the UI framework's internal logic and component lifecycle.
 * This exception is typically thrown when framework contracts are violated, such as accessing a component
 * that has not been initialized or is in an invalid state.
 */
class InterfaceException : Exception {
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