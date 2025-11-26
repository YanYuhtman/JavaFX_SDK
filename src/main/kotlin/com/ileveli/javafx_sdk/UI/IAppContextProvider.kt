package com.ileveli.javafx_sdk.UI

import kotlinx.coroutines.CoroutineScope

/**
 * Defines context ware entity
 * @param AppContext Application instance
 * @see AbstractApplication
 */
interface IAppContextProvider<AppContext> where AppContext : AbstractApplication {
    val appContext:AppContext
    val appScope : CoroutineScope

}