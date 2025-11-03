package com.ileveli.javafx_sdk.UI

enum class ModelState{
    DETACHED,
    INITIALIZED,
    SCENE_ATTACHED,
    SCENE_DETACHED,
    CONTROLLER_ATTACHED,
    CONTROLLER_DETACHED,
}

interface IModel{

}
abstract class AbstractSceneModel<AppContext, Scene> : IModel, IAppContextProvider<AppContext>, ISceneProvider<AppContext, AbstractScene<AppContext>>
    where AppContext : AbstractApplication, Scene : AbstractScene<AppContext> {

    protected var _modelState = ModelState.DETACHED
    open var modelState: ModelState
        get() = _modelState
        protected set(value) {
            _modelState = value
            onModelStateChanged()
            if(value == ModelState.SCENE_ATTACHED){
                _modelState = ModelState.INITIALIZED
                onModelStateChanged()
            }
            else if(value == ModelState.SCENE_DETACHED) {
                modelState = ModelState.DETACHED
                onModelStateChanged()

            }
        }

    protected var _appContext:AppContext? = null
    override val appContext: AppContext
        get() = _appContext?.let { return it } ?: throw UI_Excpetion("The application context is not attached!")

    protected var _scene:Scene? = null
    override val scene: AbstractScene<AppContext>
        get() = _scene?.let { return it } ?: throw UI_Excpetion("The scene is not attached!")

    internal fun attachScene(scene: Scene){
        _scene = scene
        _appContext = scene.appContext
        modelState = ModelState.SCENE_ATTACHED

        appContext.Logger.info { "Scene ${scene::class} attached to the the model\n${this@AbstractSceneModel::class}" }
    }
    abstract fun OnSceneAttached(scene: Scene)

    internal fun detachScene(){
        _scene = null
        modelState = ModelState.SCENE_DETACHED
        appContext.Logger.info { "Scene ${scene::class} detached to the the model\n${this@AbstractSceneModel::class}" }
        _appContext = null
    }

    protected fun onModelStateChanged(){
        appContext.Logger.info { "Model state changed to $modelState" }
        OnModelStateChanged(modelState)
    }

    abstract fun OnModelStateChanged(state: ModelState)

}

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
                _modelState = ModelState.INITIALIZED
                onModelStateChanged()
            }
            else if(oldState == ModelState.INITIALIZED) {
                _modelState = ModelState.DETACHED
                onModelStateChanged()
            }
        }


    private var _controller:Controller? = null
    val controller:Controller
        get() = _controller?.let { return it } ?: throw UI_Excpetion("The controller is not attached!")

    internal fun attachController(controller:Controller){
        _controller = controller;
        modelState = ModelState.CONTROLLER_ATTACHED
        appContext.Logger.info { "Controller ${controller::class} attached to the the model ${this@AbstractControllerModel::class}" }
    }

    internal fun detachController(){
        _controller = null
        modelState = ModelState.CONTROLLER_DETACHED
        appContext.Logger.info { "Controller ${controller::class} detached from the the model ${this@AbstractControllerModel::class}" }
    }
}
