package com.ileveli.javafx_sdk.UI

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.SceneAntialiasing
import javafx.scene.control.MenuBar
import javafx.scene.layout.BorderPane
import java.security.InvalidParameterException
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.pathString
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

const val id_root_pan = "ileveli_root_pane"
const val id_menuBar = "ileveli_menuBar"

internal object SceneUtils{
    private val loaderDic = mutableMapOf<String, FXMLLoader>()
    internal fun DemandLoader(fxmlResourcePath: String): FXMLLoader {
        if(!loaderDic.containsKey(fxmlResourcePath))
            throw Exception("Critical exception! Missing FXMLLoader for key: $fxmlResourcePath")
        val result = loaderDic[fxmlResourcePath]!!
        loaderDic.remove(fxmlResourcePath)
        return result
    }
    internal fun MenuBarWrapper(parent: Parent, menuBar: MenuBar = MenuBar()) : Parent{
        val pane = BorderPane().also { pane ->
            pane.id = id_root_pan
        }
        menuBar.isUseSystemMenuBar = true
        menuBar.id = id_menuBar

        pane.top = menuBar
        pane.bottom = parent

        return pane
    }
    internal fun <AppContext>LoadFXMl(appContext:AppContext ,fxmlResourcePath:String, cached: Boolean = true): Parent
            where AppContext : Application{
        val fxmlLoader = FXMLLoader(appContext.javaClass.getResource(verifyFXMLPath(fxmlResourcePath)))
        if(cached)
            loaderDic[fxmlResourcePath] = fxmlLoader
        return fxmlLoader.load()
    }

    internal fun <AppContext>LoadWrappedFXMl(appContext:AppContext ,fxmlResourcePath:String,fxmlMenuResourcePath: String = ""): Parent
        where AppContext : Application{
            if(fxmlMenuResourcePath.isEmpty())
                return MenuBarWrapper(LoadFXMl(appContext,fxmlResourcePath))
            else {
                val content = LoadFXMl(appContext, fxmlResourcePath)
                val menuContainer = LoadFXMl(appContext, fxmlMenuResourcePath, false)
                val menu: Node = menuContainer.lookup("MenuBar")
                    ?: throw InterfaceException("Referenced 'fxmlMenuResourcePath: $fxmlMenuResourcePath' must contain item of type ${typeOf<MenuBar>()}")

                return MenuBarWrapper(content,menu as MenuBar)

            }

    }
    internal fun verifyFXMLPath(pathString: String):String{
        if(pathString.isBlank())
            throw InvalidParameterException("The FXML path must not be empty")
        var path = Path(pathString)

        if(path.extension.isBlank())
            path = Path(path.pathString + ".fxml")
        else if(!path.extension.equals("fxml", true))
            throw InvalidParameterException("Path extension should be 'fxml',the ${path.extension} extension is invalid ")

        return path.pathString
    }

}

/**
 * Simple Context aware scene abstraction Scene
 * @param AppContext The reference to the Application class
 */

abstract class AbstractScene<AppContext> : IAppContextProvider<AppContext>,Scene
        where AppContext : AbstractApplication {
    internal lateinit var _appContext: AppContext
    override val appContext: AppContext
        get() = _appContext


    internal lateinit var _menuBar: MenuBar
    val menuBar: MenuBar
        get() =  menuBar


    private fun _initialize(){
        _appContext = appContext
        _menuBar = this.lookup("#$id_menuBar") as MenuBar
    }
    /**
     * @constructor Construct scene with ready to use MenuBar [menuBar]
     * @param appContext Application class reference
     * @see javafx.scene.Scene for the rest params
     */
    constructor(appContext: AppContext, root: Parent, depthBuffer: Boolean = false, antialising: SceneAntialiasing? = null)
            : super ( SceneUtils.MenuBarWrapper(root),-1.0,-1.0,depthBuffer,antialising){
        this._appContext = appContext

    }

    /**
     * @constructor Construct scene with ready to use MenuBar [menuBar]
     * @param appContext Application class reference
     * @see javafx.scene.Scene for the rest params
     */
    constructor(appContext: AppContext, root: Parent, width: Double, height:Double, depthBuffer: Boolean = false, antialising: SceneAntialiasing?)
            : super (root,width,height,depthBuffer,antialising){
        this._appContext = appContext
    }

    private val modelCache = mutableMapOf<KClass<out IModel>, IModel>()

    /**
     * In bind model to the controller and the Scene one must call this method
     * @param model Model instance
     * @param forceDetachOld Detach old instance if attached
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

    @Suppress("UNCHECKED_CAST")
    protected open fun OnModelCached(model: IModel){
        (model as AbstractSceneModel<AppContext, AbstractScene<AppContext>>).attachScene(this)
    }

    fun detachModel(clazz: KClass<out IModel>) : Boolean{
        return modelCache[clazz]?.let {
            OnModelUncached(it)
            true
        }?: false
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun OnModelUncached(model: IModel){
        (model as AbstractSceneModel<AppContext, AbstractScene<AppContext>>).detachScene()
    }

    fun getModel(clazz: KClass<out IModel>) : IModel?{
        return modelCache[clazz]
    }
 }


/**
 * Context aware scene abstraction for FXML solution with ready to use Manu Bar
 * @param AppContext The reference to the Application class
 * @param Controller The FXML Controller that is defined in the fxml descriptor
 * @see MenuBar
 */
abstract class AbstractFXMLScene<AppContext,Controller> : AbstractScene<AppContext>
        where AppContext : AbstractApplication, Controller : AbstractController<AppContext>{
    protected lateinit var loader: FXMLLoader

    val controller:Controller
        get() = loader.getController<Controller>()

    private fun _initialize(fxmlResourcePath: String){
        this.loader = SceneUtils.DemandLoader(fxmlResourcePath)
        val rootPane = this.lookup("#$id_root_pan") as Parent
        val menuBar = this.lookup("#$id_menuBar") as MenuBar
        controller.init(appContext,rootPane,menuBar)

    }
    /**
     * @constructor Constructs context aware scene by FXML with ready to use ManuBar [AbstractController.menuBar]
     * @param appContext Application class reference
     * @param fxmlResourcePath Subpath to the FXML file - **Relative to the toot of the Context classpath!**
     * @param fxmlMenuResourcePath Optional, subpath to the FXML file with menu declaration - **Relative to the toot of the Context classpath!**
     * @see javafx.scene.Scene for the rest params
     */
    constructor(appContext: AppContext, fxmlResourcePath: String, fxmlMenuResourcePath:String = "", depthBuffer: Boolean = false, antialising: SceneAntialiasing? = null)
            :super(appContext, SceneUtils.LoadWrappedFXMl(appContext,fxmlResourcePath,fxmlMenuResourcePath),depthBuffer,antialising){
        _initialize(fxmlResourcePath)
    }

    /**
     * @constructor Constructs context aware scene by FXML with ready to use ManuBar [AbstractController.menuBar]
     * @param appContext Application class reference
     * @param fxmlResourcePath Subpath to the FXML file - **Relative to the toot of the Context classpath!**
     * @see javafx.scene.Scene for the rest params
     */
    constructor(appContext: AppContext, fxmlResourcePath: String,  fxmlMenuResourcePath:String = "", width: Double, height:Double, depthBuffer: Boolean = false, antialising: SceneAntialiasing? = null)
            :super(appContext, SceneUtils.LoadWrappedFXMl(appContext,fxmlResourcePath,fxmlMenuResourcePath),width,height,depthBuffer,antialising){
        _initialize(fxmlResourcePath)

    }

    @Suppress("UNCHECKED_CAST")
    override fun OnModelCached(model: IModel) {
        val m = (model as AbstractControllerModel<AppContext, AbstractScene<AppContext>, AbstractController<AppContext>>)
        m.attachScene(this)
        m.attachController(controller)
    }

    @Suppress("UNCHECKED_CAST")
    override fun OnModelUncached(model: IModel) {
        val m = (model as AbstractControllerModel<AppContext, AbstractScene<AppContext>, AbstractController<AppContext>>)
        m.detachScene()
        m.detachController()
    }

}


