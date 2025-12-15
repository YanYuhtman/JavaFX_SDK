package com.ileveli.javafx_sdk.UI

import com.ileveli.javafx_sdk.utils.CustomCoroutineScope
import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.SceneAntialiasing
import javafx.scene.control.MenuBar
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import javafx.stage.WindowEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.InvalidParameterException
import java.util.ResourceBundle
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.pathString
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

/**
 * The ID assigned to the root BorderPane created by [SceneUtils.MenuBarWrapper].
 */
const val id_root_pan = "ileveli_root_pane"
/**
 * The ID assigned to the MenuBar managed by the scene.
 */
const val id_menuBar = "ileveli_menuBar"

/**
 * Internal utility object for handling FXML loading and scene construction.
 */
internal object SceneUtils{
    private val loaderDic = mutableMapOf<String, FXMLLoader>()

    /**
     * Retrieves a cached [FXMLLoader] for a given FXML path and removes it from the cache.
     * This is used to pass a pre-loaded loader to the scene's controller.
     *
     * @param fxmlResourcePath The resource path of the FXML file.
     * @return The cached [FXMLLoader].
     * @throws iLeveliException if no loader is found for the given path.
     */
    internal fun DemandLoader(fxmlResourcePath: String): FXMLLoader {
        if(!loaderDic.containsKey(fxmlResourcePath))
            throw iLeveliException("Critical exception! Missing FXMLLoader for key: $fxmlResourcePath")
        val result = loaderDic[fxmlResourcePath]!!
        loaderDic.remove(fxmlResourcePath)
        return result
    }

    /**
     * Wraps a given [Parent] node in a [BorderPane] with a [MenuBar] at the top.
     *
     * @param parent The main content to be placed in the center of the pane.
     * @param menuBar An optional [MenuBar]. If not provided, a new one with `useSystemMenuBar` set to true is created.
     * @return The new [BorderPane] containing the menu and content.
     */
    internal fun MenuBarWrapper(parent: Parent, menuBar: MenuBar? = null) : Parent{
        val pane = BorderPane().also { pane ->
            pane.id = id_root_pan
        }
        val _menuBar = menuBar ?: MenuBar().also {
            it.isUseSystemMenuBar = true
        }
        _menuBar.id = id_menuBar

        pane.top = _menuBar
        pane.center = parent

        return pane
    }

    /**
     * Loads an FXML file and returns the root [Parent] node.
     *
     * @param appContext The application context used to resolve resource paths.
     * @param fxmlResourcePath The path to the FXML file.
     * @param bundle The [ResourceBundle] for localization.
     * @param cached If `true`, the [FXMLLoader] instance is cached for later retrieval via [DemandLoader].
     * @return The loaded [Parent] node from the FXML file.
     */
    internal fun <AppContext>LoadFXMl(appContext:AppContext ,fxmlResourcePath:String, bundle: ResourceBundle? = null, cached: Boolean = true): Parent
            where AppContext : Application{
        val fxmlLoader = FXMLLoader(appContext.javaClass.getResource(verifyFXMLPath(fxmlResourcePath)))
        fxmlLoader.resources = bundle
        if(cached)
            loaderDic[fxmlResourcePath] = fxmlLoader
        return fxmlLoader.load()
    }

    /**
     * Loads FXML for the main content and an optional menu, then wraps them in a [BorderPane].
     *
     * @param appContext The application context.
     * @param fxmlResourcePath The path to the main content FXML.
     * @param fxmlMenuResourcePath Optional path to an FXML file containing a [MenuBar].
     * @param bundle The [ResourceBundle] for localization.
     * @return A [Parent] node with the menu and content.
     * @throws InterfaceException if `fxmlMenuResourcePath` is provided but does not contain a `MenuBar`.
     */
    internal fun <AppContext>LoadWrappedFXMl(appContext:AppContext ,fxmlResourcePath:String,fxmlMenuResourcePath: String = "", bundle: ResourceBundle? = null): Parent
        where AppContext : Application{
            if(fxmlMenuResourcePath.isEmpty())
                return MenuBarWrapper(LoadFXMl(appContext,fxmlResourcePath,bundle))
            else {
                val content = LoadFXMl(appContext, fxmlResourcePath,bundle)
                val menuContainer = LoadFXMl(appContext, fxmlMenuResourcePath,bundle, true)
                val menu: Node = menuContainer.lookup("MenuBar")
                    ?: throw InterfaceException("Referenced 'fxmlMenuResourcePath: $fxmlMenuResourcePath' must contain item of type ${typeOf<MenuBar>()}")

                return MenuBarWrapper(content,menu as MenuBar)

            }

    }

    /**
     * Verifies and standardizes an FXML file path. Ensures it has a `.fxml` extension and uses forward slashes.
     *
     * @param pathString The raw path string.
     * @return A standardized resource path.
     * @throws InvalidParameterException if the path is blank or has an invalid extension.
     */
    internal fun verifyFXMLPath(pathString: String):String{
        if(pathString.isBlank())
            throw InvalidParameterException("The FXML path must not be empty")
        var path = Path(pathString)

        if(path.extension.isBlank())
            path = Path(path.pathString + ".fxml")
        else if(!path.extension.equals("fxml", true))
            throw InvalidParameterException("Path extension should be 'fxml',the ${path.extension} extension is invalid ")

        //Fixes path for resources
        return path.pathString.replace("\\","/")
    }

}

/**
 * An abstract base class for a JavaFX [Scene] that provides context-awareness, lifecycle management,
 * and a model-caching mechanism. It is designed for scenes created programmatically.
 *
 * It automatically creates a `sceneScope` [CoroutineScope] that is cancelled when the scene is detached
 * from the stage. It also handles model attachment and detachment, linking them to the scene's lifecycle.
 *
 * @param AppContext The type of the [AbstractApplication] providing the application-level context.
 * @property appContext Provides access to the main application instance.
 * @property appScope Provides access to the application-level [CoroutineScope].
 * @property sceneScope A [CoroutineScope] tied to this scene's lifecycle. It is created when the scene is initialized and cancelled when detached.
 * @property menuBar The [MenuBar] associated with this scene, automatically created and placed in a [BorderPane].
 * @property stage The [Stage] this scene is currently attached to.
 */
abstract class AbstractScene<AppContext> : IAppContextProvider<AppContext>,Scene
        where AppContext : AbstractApplication {
    internal lateinit var _appContext: AppContext
    override val appContext: AppContext
        get() = _appContext

    override val appScope : CoroutineScope
        get() = _appContext.appScope

    private var _sceneScope: CoroutineScope? = null
    val sceneScope: CoroutineScope
        get() = _sceneScope?.let { it } ?: throw InterfaceException("The scene scope not yet initialized")

    internal lateinit var _menuBar: MenuBar
    val menuBar: MenuBar
        get() =  _menuBar

    val stage: Stage
        get() = this.window as Stage

    private fun _initialize(appContext: AppContext){
        _appContext = appContext
        _sceneScope = CustomCoroutineScope(appContext.appScope, "SceneScope ${this.javaClass.name}")
        _menuBar = this.lookup("#$id_menuBar") as MenuBar
    }
    /**
     * Constructs a scene with a programmatically created root and a ready-to-use [MenuBar].
     * The root is automatically wrapped in a [BorderPane].
     *
     * @param appContext Reference to the application class.
     * @param root The root [Parent] node for the scene content.
     * @param depthBuffer Specifies whether a depth buffer is enabled.
     * @param antialising Specifies the anti-aliasing mode.
     */
    constructor(appContext: AppContext, root: Parent, depthBuffer: Boolean = false, antialising: SceneAntialiasing? = null)
            : this (0,appContext, SceneUtils.MenuBarWrapper(root),depthBuffer,antialising)
    protected constructor(internalConstructor:Int = 0,appContext: AppContext, root: Parent, depthBuffer: Boolean = false, antialising: SceneAntialiasing? = null, )
            : super ( root,-1.0,-1.0,depthBuffer,antialising){
        _initialize(appContext)


        //Receiving close event workaround
        appScope.launch {
            var attachedToStage: Stage? = null
            while (attachedToStage == null){
                attachedToStage = (this@AbstractScene.window as Stage?)
                delay(100)

            }
            Logger.info {"Scene ${this@AbstractScene::class} attached to stage ${attachedToStage.title}\n"}
            attachedToStage.onCloseRequest = EventHandler<WindowEvent> {
                detachScene()
            }
        }
    }

    /**
     * Constructs a scene with a specified size, a programmatically created root, and a ready-to-use [MenuBar].
     *
     * @param appContext Reference to the application class.
     * @param root The root [Parent] node for the scene content.
     * @param width The width of the scene.
     * @param height The height of the scene.
     * @param depthBuffer Specifies whether a depth buffer is enabled.
     * @param antialising Specifies the anti-aliasing mode.
     */
    constructor(appContext: AppContext, root: Parent, width: Double, height:Double, depthBuffer: Boolean = false, antialising: SceneAntialiasing?)
            :this(1,appContext, SceneUtils.MenuBarWrapper(root),width,height,depthBuffer,antialising)
    protected constructor(internalConstructor: Int = 0, appContext: AppContext, root: Parent, width: Double, height:Double, depthBuffer: Boolean = false, antialising: SceneAntialiasing?)
            : super (root,width,height,depthBuffer,antialising){
        this._appContext = appContext
    }

    private fun detachScene(){
        Logger.info {"${this@AbstractScene::class}: Detaching the scene from stage"}
        val mKeys = modelCache.keys.toList()
        sceneScope.cancel()
        for(key in mKeys){
            detachModel(key)
        }
    }

    private val modelCache = mutableMapOf<KClass<out IModel>, IModel>()

    /**
     * Attaches a model instance to the scene's lifecycle.
     *
     * @param model The model instance to attach.
     * @param forceDetachOld If `true`, any existing model of the same type will be detached first.
     * @throws InterfaceException if a model of the same type is already attached and `forceDetachOld` is `false`.
     */
    fun attachModel(model: IModel, forceDetachOld: Boolean = false){
        if(modelCache.containsKey(model::class)) {
            if(forceDetachOld)
                detachModel(model::class)
            else
                throw InterfaceException("The model type ${model::class} already attached to the Scene ${this@AbstractScene::class}")
        }
        modelCache[model::class] = model
        OnModelCached(model)
    }

    /**
     * A hook that is called after a model has been cached. Subclasses can override this to perform
     * additional setup, such as attaching the scene to the model.
     *
     * @param model The model that was just cached.
     */
    @Suppress("UNCHECKED_CAST")
    protected open fun OnModelCached(model: IModel){
        (model as AbstractSceneModel<AppContext, AbstractScene<AppContext>>).attachScene(this)
    }

    /**
     * Detaches a model from the scene based on its class.
     *
     * @param clazz The [KClass] of the model to detach.
     * @return `true` if a model was found and detached, `false` otherwise.
     */
    fun detachModel(clazz: KClass<out IModel>) : Boolean{
        return modelCache[clazz]?.let {
            OnModelUncached(it)
            modelCache.remove(clazz)
            true
        }?: false
    }

    /**
     * A hook that is called before a model is uncached and removed. Subclasses can override this to
     * perform cleanup, such as detaching the scene from the model.
     *
     * @param model The model that is about to be uncached.
     */
    @Suppress("UNCHECKED_CAST")
    protected open fun OnModelUncached(model: IModel){
        (model as AbstractSceneModel<AppContext, AbstractScene<AppContext>>).detachScene()
    }

    /**
     * Retrieves a previously attached model by its class.
     *
     * @param clazz The [KClass] of the model to retrieve.
     * @return The model instance, or `null` if no model of that type is attached.
     */
    fun getModel(clazz: KClass<out IModel>) : IModel?{
        return modelCache[clazz]
    }
 }


/**
 * An abstract base class for a JavaFX [Scene] loaded from an FXML file. It extends [AbstractScene]
 * to provide FXML-specific functionality, including controller access and automatic initialization.
 *
 * @param AppContext The type of the [AbstractApplication] providing the application-level context.
 * @param Controller The type of the FXML controller associated with this scene.
 * @property loader The [FXMLLoader] used to load the main FXML file.
 * @property menuLoader The optional [FXMLLoader] for the menu FXML file.
 * @property controller The main FXML controller instance.
 * @property menuController The optional controller for the menu FXML.
 */
abstract class AbstractFXMLScene<AppContext,Controller> : AbstractScene<AppContext>
        where AppContext : AbstractApplication, Controller : AbstractController<AppContext>{
    protected lateinit var loader: FXMLLoader
    protected var menuLoader: FXMLLoader? = null

    val controller:Controller
        get() = loader.getController<Controller>()
            ?: throw iLeveliException("View fxml: ${loader.location} must provide controller")

    val menuController: AbstractController<AppContext>?
        get() {
            menuLoader?.getController<AbstractController<AppContext>>()?.let {
                return it
            }?: Logger.warn { "Default menu controller is not defined for scene ${this.javaClass.name}" }
            return null
        }

    private fun _initialize(fxmlResourcePath: String, fxmlMenuResourcePath: String){
        this.loader = SceneUtils.DemandLoader(fxmlResourcePath)
        if(!fxmlMenuResourcePath.isEmpty())
            this.menuLoader = SceneUtils.DemandLoader(fxmlMenuResourcePath)
        val rootPane = this.lookup("#$id_root_pan") as Parent
        val menuBar = this.lookup("#$id_menuBar") as MenuBar
        controller.init(appContext,rootPane,menuBar)
        menuController?.init(appContext,rootPane,menuBar)

    }
    /**
     * Constructs a context-aware scene from an FXML file, with a ready-to-use [MenuBar].
     *
     * @param appContext Application class reference.
     * @param fxmlResourcePath Path to the FXML file, relative to the classpath root.
     * @param fxmlMenuResourcePath Optional path to an FXML file with a menu declaration.
     * @param resourceBundle The [ResourceBundle] for localization. If `null`, the app's default is used.
     * @param depthBuffer Specifies whether a depth buffer is enabled.
     * @param antialising Specifies the anti-aliasing mode.
     */
        constructor(appContext: AppContext, fxmlResourcePath: String, fxmlMenuResourcePath:String = "", resourceBundle: ResourceBundle? = null, depthBuffer: Boolean = false, antialising: SceneAntialiasing? = null)
            :super(1, appContext
        , SceneUtils.LoadWrappedFXMl(appContext,fxmlResourcePath,fxmlMenuResourcePath, resourceBundle?.let { it }?:appContext.resourceBundle)
        ,depthBuffer,antialising)
    {
        _initialize(fxmlResourcePath,fxmlMenuResourcePath)
    }

    /**
     * Constructs a context-aware scene with a specific size from an FXML file.
     *
     * @param appContext Application class reference.
     * @param fxmlResourcePath Path to the FXML file, relative to the classpath root.
     * @param fxmlMenuResourcePath Optional path to an FXML file with a menu declaration.
     * @param bundle The [ResourceBundle] for localization.
     * @param width The width of the scene.
     * @param height The height of the scene.
     * @param depthBuffer Specifies whether a depth buffer is enabled.
     * @param antialising Specifies the anti-aliasing mode.
     */
    constructor(appContext: AppContext, fxmlResourcePath: String, fxmlMenuResourcePath:String = "", bundle: ResourceBundle? = null, width: Double, height:Double, depthBuffer: Boolean = false, antialising: SceneAntialiasing? = null)
            :super(1,appContext
        , SceneUtils.LoadWrappedFXMl(appContext,fxmlResourcePath,fxmlMenuResourcePath,bundle)
        ,width,height,depthBuffer,antialising){
        _initialize(fxmlResourcePath,fxmlMenuResourcePath)

    }

    /**
     * Extends the base implementation to also attach the FXML controller to the model.
     */
    @Suppress("UNCHECKED_CAST")
    override fun OnModelCached(model: IModel) {
        val m = (model as AbstractControllerModel<AppContext, AbstractScene<AppContext>, AbstractController<AppContext>>)
        m.attachScene(this)
        m.attachController(controller)
    }

    /**
     * Extends the base implementation to also detach the FXML controller from the model.
     */
    @Suppress("UNCHECKED_CAST")
    override fun OnModelUncached(model: IModel) {
        val m = (model as AbstractControllerModel<AppContext, AbstractScene<AppContext>, AbstractController<AppContext>>)
        m.detachScene()
        m.detachController()
    }

}
