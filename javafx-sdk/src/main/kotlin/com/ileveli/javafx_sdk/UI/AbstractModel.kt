package com.ileveli.javafx_sdk.UI

import com.ileveli.javafx_sdk.utils.CustomCoroutineScope
import javafx.application.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive


/**
 * Represents the lifecycle status of a model as it gets attached to or detached from UI components.
 */
enum class ModelState{
    /** The model is not attached to any component. */
    DETACHED,
    /** The model is fully attached to all required components (e.g., scene and controller). */
    ATTACHED,
    /** The model has just been attached to a scene. */
    SCENE_ATTACHED,
    /** The scene the model is attached to has been shown on the stage. */
    SCENE_SHOWN,
    /** The scene has been detached from the model. */
    SCENE_DETACHED,
    /** The model has just been attached to a controller. */
    CONTROLLER_ATTACHED,
    /** The controller has been detached from the model. */
    CONTROLLER_DETACHED,
}

/**
 * A marker interface for all model classes in the framework.
 */
interface IModel{

}

/**
 * An abstract base class for models that are tied to the lifecycle of a scene, but not necessarily an FXML controller.
 * It provides context-awareness, coroutine scopes, and lifecycle callbacks.
 *
 * @param AppContext The type of the [AbstractApplication].
 * @param Scene The type of the [AbstractScene] this model can attach to.
 * @property isModelAttached A boolean flag indicating if the model is fully attached and ready.
 * @property modelState The current [ModelState] of the model.
 * @property appContext Provides access to the main application instance.
 * @property appScope Provides access to the application-level [CoroutineScope].
 * @property modelScope A [CoroutineScope] tied to this model's lifecycle, created when attached to a scene and cancelled when detached.
 * @property scene The [AbstractScene] this model is currently attached to.
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
     * @return The attached scene.
     * @throws InterfaceException if no scene is attached.
     * @see ModelState
     */
    override val scene: AbstractScene<AppContext>
        get() = _scene?.let { return it } ?: throw InterfaceException("The scene is not attached!")

    /**
     * Attaches the model to a scene, initializing the application context and model-specific coroutine scope.
     * @param scene The scene to attach to.
     */
    internal fun attachScene(scene: Scene){
        _scene = scene
        _appContext = scene.appContext
        if(_modelScope == null || !modelScope.isActive)
            _modelScope = CustomCoroutineScope(scene.sceneScope, "ModelScope ${this.javaClass.name}")

        modelState = ModelState.SCENE_ATTACHED
        Platform.runLater {
            modelState = ModelState.SCENE_SHOWN
        }

        Logger.debug { "Scene ${scene::class} attached to the the model\n${this@AbstractSceneModel::class}" }
    }

    /**
     * Detaches the model from the scene, cancelling the model's coroutine scope and clearing references.
     */
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
     * A general hook called whenever the model's state changes.
     * @param state The current [ModelState].
     */
    open fun OnModelStateChanged(state: ModelState){}

    /**
     * Called when the model is fully attached and all component references are available.
     * This is the primary entry point for a model's logic.
     */
    abstract fun OnAttached()

    /**
     * Called after the attached scene has been rendered and shown on the stage.
     */
    abstract fun OnSceneShown()
    /**
     * Called when the model is fully detached from all components. Use this for cleanup.
     */
    abstract fun OnDetached()

}
/**
 * An abstract model designed to be attached to both a scene and an FXML controller.
 * It extends [AbstractSceneModel] to include controller-specific lifecycle management.
 *
 * @param AppContext The type of the [AbstractApplication].
 * @param Scene The type of the [AbstractScene].
 * @param Controller The type of the [AbstractController].
 * @property controller The [AbstractController] this model is currently attached to.
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
     * @return The attached controller.
     * @throws InterfaceException if no controller is attached.
     * @see ModelState
     */
    val controller:Controller
        get() = _controller?.let { return it } ?: throw InterfaceException("The controller is not attached!")

    /**
     * Attaches the model to a controller, creating a coroutine scope for the controller.
     * @param controller The controller to attach to.
     */
    internal fun attachController(controller:Controller){
        _controller = controller
        if(controller._controllerScope == null || !controller.controllerScope.isActive)
            controller._controllerScope = CustomCoroutineScope(scene.sceneScope, "ControllerScope ${javaClass.name}")

        modelState = ModelState.CONTROLLER_ATTACHED
        Logger.debug { "Controller ${controller::class} attached to the the model ${this@AbstractControllerModel::class}" }
    }

    /**
     * Detaches the model from the controller, cancelling the controller's scope and clearing references.
     */
    internal fun detachController(){
        Logger.debug { "Controller ${controller::class} detaching from the the model ${this@AbstractControllerModel::class}" }
        _controller?.controllerScope?.cancel()
        _controller = null
        modelState = ModelState.CONTROLLER_DETACHED

    }
}
