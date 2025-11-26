package com.ileveli.javafx_sdk.UI

import com.ileveli.javafx_sdk.utils.CustomCoroutineScope
import javafx.collections.ObservableList
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext


/**
 * Abstract Context aware controller
 */
abstract class AbstractController<AppContext> : Initializable, IAppContextProvider<AppContext>
        where AppContext : AbstractApplication
{
    private lateinit var _appContext:AppContext
    override val appContext:AppContext
        get() = _appContext

    override val appScope: CoroutineScope
        get() = appContext.appScope

    internal var _controllerScope: CoroutineScope? = null
    val controllerScope: CoroutineScope
        get() = _controllerScope?.let { it } ?: throw InterfaceException("The controller scope not yet initialized")


    private lateinit var _root: Parent
    val root: Parent
        get() = _root

    private lateinit var _menuBar: MenuBar
    val menuBar: MenuBar
        get() = _menuBar

    val menus: ObservableList<Menu>
        get() = menuBar.menus


    internal fun init(appContext: AppContext, root: Parent, menuBar: MenuBar){
        _appContext = appContext
        _root = root
        _menuBar = menuBar
        Logger.debug { "Controller initialized"}
    }

    //    private val modelsMap = mutableMapOf<KClass<*>,AbstractModel<AppContext, AbstractController<AppContext>>>()
//    fun attachModel(model: AbstractModel<AppContext, AbstractController<AppContext>>){
//        modelsMap[model::class] = model
//        model.attachController(this)
//    }
//    fun detachModel(clazz: KClass<AbstractModel<AppContext, AbstractController<AppContext>>>){
//        modelsMap[clazz]?.let {
//            modelsMap.remove(clazz)
//            it.detachController(this@AbstractController)
//        }
//    }
//    fun getModel(clazz: KClass<AbstractModel<AppContext, AbstractController<AppContext>>>)
//            : AbstractModel<AppContext, AbstractController<AppContext>>? {
//        return modelsMap[clazz]
//    }
}