package com.ileveli.javafx_sdk.UI

import kotlinx.coroutines.CoroutineScope

/**
 * An interface for components that are aware of the application context.
 * It provides access to the main application instance and its primary coroutine scope.
 *
 * @param AppContext The type of the [AbstractApplication] instance.
 * @see AbstractApplication
 */
interface IAppContextProvider<AppContext> where AppContext : AbstractApplication {
    /**
     * The application instance, providing access to global state and services.
     */
    val appContext:AppContext
    /**
     * The primary [CoroutineScope] of the application, tied to the application's lifecycle.
     */
    val appScope : CoroutineScope

}