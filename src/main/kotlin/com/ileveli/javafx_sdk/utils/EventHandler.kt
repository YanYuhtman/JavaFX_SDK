package com.ileveli.javafx_sdk.utils

import com.ileveli.javafx_sdk.UI.Logger
import java.lang.ref.WeakReference
import kotlin.collections.mutableMapOf




abstract class AbstractEventHandler<F> where F : Function<*>  {
    private var handlersList = mutableMapOf<Int,Pair<WeakReference<Any>,F>>()

    fun getKey(handler: F):Int = getComponentsFromClosure(handler, { key, receiver -> return@getComponentsFromClosure key })

    /**
     * Seams that weak reference has no effect cause the closure reference (hard) holds the instance
     * Despite making it weak drops immediately. There is option to use softReference ... but not much difference
     */
    private fun <T> getComponentsFromClosure(handler: F, result: (key:Int,receiver:Any)-> T) : T {
        var caller = handler as kotlin.jvm.internal.FunctionReference
        val receiver = caller.boundReceiver
        val key = System.identityHashCode(receiver)
        return result(key,receiver)
    }

    fun addHandler(handler: F):Int =
        getComponentsFromClosure(handler,{key, receiver ->
            handlersList[key] = WeakReference(receiver) to handler
            return@getComponentsFromClosure key
        })

    fun removeHandler(closure:F): Boolean = removeHandler(getKey(closure))

    fun removeHandler(key: Int): Boolean = handlersList.remove(key)?.let { true } ?: false

    protected fun fireRawEvent(vararg obj: Any){
        handlersList.forEach { entry ->
            entry.value.takeIf { it.first.get() != null }
                ?.let {
                  onFired(it.second, *obj)
                } ?: run {
                Logger.warn { "Removing handler ${entry.key} since its receiver has died" }
                handlersList.remove(entry.key)
            }
        }
    }
    protected abstract fun onFired(handler:F, vararg obj: Any)
}



typealias Action<T> =  (T)-> Unit
class EventHandler<T> : AbstractEventHandler<Action<T>>() {
    fun fireEvent(item:T) = fireRawEvent(item as Any)
    @Suppress("UNCHECKED_CAST")
    override fun onFired(handler: Action<T>, vararg obj: Any) {
        handler.invoke(obj[0] as T)
    }
}


typealias Action2<T0,T1> =  (T0, T1)-> Unit
class EventHandler2<T0,T1> : AbstractEventHandler<Action2<T0,T1>>() {
    fun fireEvent(item0:T0,item1:T1) = fireRawEvent(item0 as Any, item1 as Any)
    @Suppress("UNCHECKED_CAST")
    override fun onFired(handler: Action2<T0,T1>, vararg obj: Any) {
        handler.invoke(obj[0] as T0, obj[1] as T1)
    }
}

typealias Action3<T0,T1,T2> =  (T0, T1, T2)-> Unit
class EventHandler3<T0,T1,T2> : AbstractEventHandler<Action3<T0,T1,T2>>() {
    fun fireEvent(item0:T0, item1:T1, item2:T2) = fireRawEvent(item0 as Any, item1 as Any, item2 as Any)
    @Suppress("UNCHECKED_CAST")
    override fun onFired(handler: Action3<T0,T1,T2>, vararg obj: Any) {
        handler.invoke(obj[0] as T0, obj[1] as T1, obj[2] as T2)
    }
}


typealias Action4<T0, T1, T2, T3> = (T0, T1, T2, T3) -> Unit
class EventHandler4<T0, T1, T2, T3> : AbstractEventHandler<Action4<T0, T1, T2, T3>>() {
    fun fireEvent(item0: T0, item1: T1, item2: T2, item3: T3) =
        fireRawEvent(item0 as Any, item1 as Any, item2 as Any, item3 as Any)
    @Suppress("UNCHECKED_CAST")
    override fun onFired(handler: Action4<T0, T1, T2, T3>, vararg obj: Any) {
        handler.invoke(obj[0] as T0, obj[1] as T1, obj[2] as T2, obj[3] as T3)
    }
}

typealias Action5<T0,T1,T2,T3,T4> =  (T0, T1, T2, T3, T4)-> Unit
class EventHandler5<T0,T1,T2,T3,T4> : AbstractEventHandler<Action5<T0,T1,T2,T3,T4>>() {
    fun fireEvent(item0:T0, item1:T1, item2:T2, item3:T3, item4:T4) = fireRawEvent(item0 as Any, item1 as Any, item2 as Any, item3 as Any, item4 as Any)
    @Suppress("UNCHECKED_CAST")
    override fun onFired(handler: Action5<T0,T1,T2,T3,T4>, vararg obj: Any) {
        handler.invoke(obj[0] as T0, obj[1] as T1, obj[2] as T2, obj[3] as T3, obj[4] as T4)
    }
}


