package com.ileveli.javafx_sdk.UI

interface IAppContextProvider<AppContext> where AppContext : AbstractApplication {
    val appContext:AppContext

}