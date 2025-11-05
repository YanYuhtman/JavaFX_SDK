package com.ileveli.javafx_sdk.UI

import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.MenuBar


/**
 * Abstract Context aware controller
 */
abstract class AbstractController<AppContext> : Initializable, IAppContextProvider<AppContext>
        where AppContext : AbstractApplication
{
    private lateinit var _appContext:AppContext
    override val appContext:AppContext
        get() = _appContext

    private lateinit var _root: Parent
    val root: Parent
        get() = _root

    private lateinit var _menuBar: MenuBar
    val menuBar: MenuBar
        get() = _menuBar

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