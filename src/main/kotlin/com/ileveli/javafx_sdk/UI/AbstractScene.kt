package com.ileveli.javafx_sdk.UI

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.SceneAntialiasing
import java.security.InvalidParameterException
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.pathString
import kotlin.reflect.KClass

internal object SceneUtils{
    private val loaderDic = mutableMapOf<String, FXMLLoader>()
    internal fun DemandLoader(fxmlResourcePath: String): FXMLLoader {
        if(!loaderDic.containsKey(fxmlResourcePath))
            throw Exception("Critical exception! Missing FXMLLoader for key: $fxmlResourcePath")
        val result = loaderDic[fxmlResourcePath]!!
        loaderDic.remove(fxmlResourcePath)
        return result
    }
    internal fun <AppContext>LoadFXMl(appContext:AppContext ,fxmlResourcePath:String): Parent
            where AppContext : Application{
        val fxmlLoader = FXMLLoader(appContext.javaClass.getResource(verifyFXMLPath(fxmlResourcePath)))
        loaderDic[fxmlResourcePath] = fxmlLoader
        return fxmlLoader.load()
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

abstract class AbstractScene<AppContext> : IAppContextProvider<AppContext>,Scene
        where AppContext : AbstractApplication {
    val _appContext: AppContext
    override val appContext: AppContext
        get() = _appContext

    constructor(appContext: AppContext, root: Parent, depthBuffer: Boolean = false, antialising: SceneAntialiasing? = null)
            : super (root,-1.0,-1.0,depthBuffer,antialising){
        this._appContext = appContext
    }

    constructor(appContext: AppContext, root: Parent, width: Double, height:Double, depthBuffer: Boolean = false, antialising: SceneAntialiasing?)
            : super (root,width,height,depthBuffer,antialising){
        this._appContext = appContext
    }

    private val modelCache = mutableMapOf<KClass<out IModel>, IModel>()
    fun attachModel(model: IModel, forceDetachOld: Boolean = false){
        if(modelCache.containsKey(model::class)) {
            if(forceDetachOld)
                detachModel(model::class)
            else
                throw UI_Excpetion("The model type ${model::class} already attached to the Scene ${this@AbstractScene::class}")
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


abstract class AbstractFXMLScene<AppContext,Controller> : AbstractScene<AppContext>
        where AppContext : AbstractApplication, Controller : AbstractController<AppContext>{
    protected var loader: FXMLLoader

    val controller:Controller
        get() = loader.getController<Controller>()

    constructor(appContext: AppContext, fxmlResourcePath: String, depthBuffer: Boolean = false, antialising: SceneAntialiasing? = null)
            :super(appContext, SceneUtils.LoadFXMl(appContext,fxmlResourcePath),depthBuffer,antialising){
        this.loader = SceneUtils.DemandLoader(fxmlResourcePath)
        controller.init(appContext)
    }

    constructor(appContext: AppContext, fxmlResourcePath: String, width: Double, height:Double, depthBuffer: Boolean = false, antialising: SceneAntialiasing? = null)
            :super(appContext, SceneUtils.LoadFXMl(appContext,fxmlResourcePath),width,height,depthBuffer,antialising){
        this.loader = SceneUtils.DemandLoader(fxmlResourcePath)
        controller.init(appContext)

    }

    override fun OnModelCached(model: IModel) {
        val m = (model as AbstractControllerModel<AppContext, AbstractScene<AppContext>, AbstractController<AppContext>>)
        m.attachScene(this)
        m.attachController(controller)
    }

    override fun OnModelUncached(model: IModel) {
        val m = (model as AbstractControllerModel<AppContext, AbstractScene<AppContext>, AbstractController<AppContext>>)
        m.detachScene()
        m.detachController()
    }
}


