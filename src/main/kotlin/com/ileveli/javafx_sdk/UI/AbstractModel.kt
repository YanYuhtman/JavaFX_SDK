package com.ileveli.javafx_sdk.UI


/**
 * Model state attach detach component status
 */
enum class ModelState{
    DETACHED,
    ATTACHED,
    SCENE_ATTACHED,
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

    internal var _modelState = ModelState.DETACHED
    open var modelState: ModelState
        get() = _modelState
        protected set(value) {
            _modelState = value
            onModelStateChanged()
            if(value == ModelState.SCENE_ATTACHED){
                _modelState = ModelState.ATTACHED
                onModelStateChanged()
            }
            else if(value == ModelState.SCENE_DETACHED) {
                modelState = ModelState.DETACHED
                onModelStateChanged()
            }
        }

    internal var _appContext:AppContext? = null
    override val appContext: AppContext
        get() = _appContext?.let { return it } ?: throw InterfaceException("The application context is not attached!")

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
        modelState = ModelState.SCENE_ATTACHED

        Logger.debug { "Scene ${scene::class} attached to the the model\n${this@AbstractSceneModel::class}" }
    }

    internal fun detachScene(){
        Logger.debug { "Scene ${scene::class} detaching from the the model\n${this@AbstractSceneModel::class}" }
        _scene = null
        modelState = ModelState.SCENE_DETACHED
        _appContext = null
    }

    internal fun onModelStateChanged(){
        Logger.debug { "Model state changed to $modelState" }
        OnModelStateChanged(modelState)
        when(modelState){
            ModelState.ATTACHED -> OnAttached()
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
            if(_scene != null && _controller != null) {
                _modelState = ModelState.ATTACHED
                onModelStateChanged()
            }
            else if(_scene == null && _controller == null) {
                _modelState = ModelState.DETACHED
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
        _controller = controller;
        modelState = ModelState.CONTROLLER_ATTACHED
        Logger.debug { "Controller ${controller::class} attached to the the model ${this@AbstractControllerModel::class}" }
    }

    internal fun detachController(){
        Logger.debug { "Controller ${controller::class} detaching from the the model ${this@AbstractControllerModel::class}" }
        _controller = null
        modelState = ModelState.CONTROLLER_DETACHED

    }
}
