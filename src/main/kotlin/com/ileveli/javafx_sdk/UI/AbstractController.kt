package com.ileveli.javafx_sdk.UI


abstract class AbstractController<AppContext> : IAppContextProvider<AppContext>
        where AppContext : AbstractApplication
{
    private var _appContext:AppContext? = null
    override val appContext:AppContext
        get() = _appContext!!

    internal fun init(appContext: AppContext){
        _appContext = appContext
        appContext.Logger.info { "Controller initialized"}
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