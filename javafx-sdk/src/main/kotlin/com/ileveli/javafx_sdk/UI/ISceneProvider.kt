package com.ileveli.javafx_sdk.UI

/**
 * An interface for components that can provide a reference to their containing [AbstractScene].
 *
 * @param AppContext The type of the [AbstractApplication].
 * @param Scene The type of the [AbstractScene].
 * @see AbstractApplication
 * @see AbstractScene
 */
interface ISceneProvider<AppContext, Scene> where AppContext : AbstractApplication, Scene : AbstractScene<AppContext>{
    /**
     * The scene instance that this component is part of.
     */
    val scene:Scene
}