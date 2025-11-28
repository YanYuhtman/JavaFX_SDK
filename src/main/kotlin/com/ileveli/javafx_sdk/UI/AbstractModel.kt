package com.ileveli.javafx_sdk.UI

import com.ileveli.javafx_sdk.utils.CustomCoroutineScope
import javafx.application.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive


/**
 * Model state attach detach component status
 */
enum class ModelState{
    DETACHED,
    ATTACHED,
    SCENE_ATTACHED,
    SCENE_SHOWN,
    SCENE_DETACHED,
    CONTROLLER_ATTACHED,
    CONTROLLER_DETACHED,
}

/**
 * Model interface
 */
interface IModel{

}

/**
 * Basic Scene model, without FXML support
 */
abstract class AbstractSceneModel<AppContext, Scene> : IModel, IAppContextProvider<AppContext>, ISceneProvider<AppContext, AbstractScene<AppContext>>
    where AppContext : AbstractApplication, Scene : AbstractScene<AppContext> {

    private var _isModelAttached = false
    var isModelAttached: Boolean
        get() = _isModelAttached
        protected set(value) { _isModelAttached = value }

    internal var _modelState = ModelState.DETACHED
    open var modelState: ModelState
        get() = _modelState
        protected set(value) {
            _modelState = value
            onModelStateChanged()
            if(value == ModelState.SCENE_ATTACHED){
                _modelState = ModelState.ATTACHED
                isModelAttached = true
                onModelStateChanged()
            }
            else if(value == ModelState.SCENE_DETACHED) {
                modelState = ModelState.DETACHED
                isModelAttached = false
                onModelStateChanged()
            }
        }

    internal var _appContext:AppContext? = null
    override val appContext: AppContext
        get() = _appContext?.let { return it } ?: throw InterfaceException("The application context is not attached!")

    override val appScope: CoroutineScope
        get() = _appContext?.let { return it.appScope } ?: throw InterfaceException("The application context is not attached!")

    private var _modelScope: CoroutineScope? = null
    val modelScope: CoroutineScope
        get() = _modelScope?.let { it } ?: throw InterfaceException("The model scope not yet initialized")

    internal var _scene:Scene? = null
    /**
     * @return The attached scene
     * @throws InterfaceException in case attached component not available
     * @see ModelState
     */
    override val scene: AbstractScene<AppContext>
        get() = _scene?.let { return it } ?: throw InterfaceException("The scene is not attached!")

    internal fun attachScene(scene: Scene){
        _scene = scene
        _appContext = scene.appContext
        if(_modelScope == null || !modelScope.isActive)
            _modelScope = CustomCoroutineScope(scene.sceneScope, "ModelScope")

        modelState = ModelState.SCENE_ATTACHED
        Platform.runLater {
            modelState = ModelState.SCENE_SHOWN
        }

        Logger.debug { "Scene ${scene::class} attached to the the model\n${this@AbstractSceneModel::class}" }
    }

    internal fun detachScene(){
        Logger.debug { "Scene ${scene::class} detaching from the the model\n${this@AbstractSceneModel::class}" }
        modelScope.cancel()
        _scene = null
        modelState = ModelState.SCENE_DETACHED
        _appContext = null
    }

    internal fun onModelStateChanged(){
        Logger.debug { "Model state changed to $modelState" }
        OnModelStateChanged(modelState)
        when(modelState){
            ModelState.ATTACHED -> OnAttached()
            ModelState.SCENE_SHOWN -> OnSceneShown()
            ModelState.DETACHED -> OnDetached()
            else -> Unit
        }
    }

    /**
     * Called on model state changed
     * @param state current model state
     * @see ModelState
     */
    open fun OnModelStateChanged(state: ModelState){}

    /**
     * Called when model finally attached. In this state all component references are available
     */
    abstract fun OnAttached()

    /**
     * Called later when scene is in show state
     */
    abstract fun OnSceneShown()
    /**
     * Called when model detached from all components
     */
    abstract fun OnDetached()

}
/**
 * Model attached to FXML controller
 * @param AppContext Application context
 * @see AbstractApplication
 *
 * @param Scene Attached scene
 * @see AbstractScene
 *
 * @param Controller Attached controller
 * @see AbstractController
 */
abstract class AbstractControllerModel<AppContext, Scene, Controller> : AbstractSceneModel<AppContext, Scene>()
        where AppContext : AbstractApplication,
              Scene : AbstractScene<AppContext>,
              Controller : AbstractController<AppContext>
{

    override var modelState: ModelState
        get() = _modelState
        protected set(value) {

            val oldState = _modelState
            _modelState = value
            onModelStateChanged()
            if(!isModelAttached && _scene != null && _controller != null) {
                _modelState = ModelState.ATTACHED
                isModelAttached = true
                onModelStateChanged()
            }
            else if(isModelAttached && _scene == null && _controller == null) {
                _modelState = ModelState.DETACHED
                isModelAttached = false
                onModelStateChanged()
            }
        }


    internal var _controller:Controller? = null

    /**
     * @return The attached controller
     * @throws InterfaceException in case attached component not available
     * @see ModelState
     */
    val controller:Controller
        get() = _controller?.let { return it } ?: throw InterfaceException("The controller is not attached!")

    internal fun attachController(controller:Controller){
        _controller = controller
        if(controller._controllerScope == null || !controller.controllerScope.isActive)
            controller._controllerScope = CustomCoroutineScope(scene.sceneScope, "ControllerScope")

        modelState = ModelState.CONTROLLER_ATTACHED
        Logger.debug { "Controller ${controller::class} attached to the the model ${this@AbstractControllerModel::class}" }
    }

    internal fun detachController(){
        Logger.debug { "Controller ${controller::class} detaching from the the model ${this@AbstractControllerModel::class}" }
        _controller?.controllerScope?.cancel()
        _controller = null
        modelState = ModelState.CONTROLLER_DETACHED

    }
}
