package com.ileveli.javafx_sdk.UI

/**
 * Defines entity with ability to provide Scene reference
 * @param AppContext
 * @param Scene
 * @see AbstractApplication
 * @see AbstractScene
 */
interface ISceneProvider<AppContext, Scene> where AppContext : AbstractApplication, Scene : AbstractScene<AppContext>{
    val scene:Scene
}