package com.ileveli.javafx_sdk.UI

interface ISceneProvider<AppContext, Scene> where AppContext : AbstractApplication, Scene : AbstractScene<AppContext>{
    val scene:Scene
}