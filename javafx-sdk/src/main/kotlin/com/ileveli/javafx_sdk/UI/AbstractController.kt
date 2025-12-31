package com.ileveli.javafx_sdk.UI

import com.ileveli.javafx_sdk.utils.Logger
import javafx.collections.ObservableList
import javafx.scene.Parent
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import kotlinx.coroutines.CoroutineScope


/**
 * An abstract base class for UI controllers, providing context-awareness and access to the application's
 * lifecycle and coroutine scopes. It serves as a bridge between the view (defined in FXML or programmatically)
 * and the application's business logic.
 *
 * @param AppContext The type of the [AbstractApplication] providing the application-level context.
 * @property appContext Provides access to the main application instance.
 * @property appScope Provides access to the application-level [CoroutineScope].
 * @property controllerScope A [CoroutineScope] that can be defined for this controller's lifecycle.
 *   It must be initialized before use.
 * @property root The root [Parent] node of the view managed by this controller.
 * @property menuBar The [MenuBar] associated with the controller's view.
 * @property menus An [ObservableList] of the [Menu]s within the [menuBar].
 */
abstract class AbstractController<AppContext> :  IAppContextProvider<AppContext>
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


    /**
     * Internal function to initialize the controller with its context and UI components.
     * This is typically called by the scene or framework after the view is loaded.
     * In case this is an **INTERNAL CONTROLLER** (the controller defined inside other controller with @FXML fooController property)
     *          - This function must be called first thing **onContextInitialized** body of the main controller
     *
     *
     * @param appContext The application context.
     * @param root The root [Parent] of the view.
     * @param menuBar The [MenuBar] for the view.
     */
    fun init(appContext: AppContext, root: Parent, menuBar: MenuBar){
        _appContext = appContext
        _root = root
        _menuBar = menuBar
        onContextInitialized(appContext)
        Logger.debug { "Controller initialized"}
    }

    /**
     * A hook method that is called after the controller has been initialized with the application context.
     * Subclasses must implement this method to perform their own initialization, such as setting up
     * event handlers, binding data, or launching initial coroutines.
     *
     * @param appContext The fully initialized application context.
     */
    protected abstract fun onContextInitialized(appContext: AppContext)

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