package com.ileveli.javafx_sdk.utils

import com.ileveli.javafx_sdk.UI.Logger
import java.lang.ref.WeakReference
import kotlin.collections.mutableMapOf

/**
 * An abstract base class for implementing event handlers with weak references to their receivers.
 * This helps prevent memory leaks by allowing garbage collection of objects that register handlers
 * when those objects are no longer strongly referenced elsewhere.
 *
 * Handlers are identified and stored based on the identity hash code of their bound receiver (the object
 * on which the handler function is defined). If a receiver is garbage collected, its associated handlers
 * are automatically removed when the event is fired.
 *
 * @param F The type of the function (handler) that this event handler manages. It must extend `Function<*>`.
 */
abstract class AbstractEventHandler<F> where F : Function<*>  {
    private var handlersList = mutableMapOf<Int,Pair<WeakReference<Any>,F>>()

    /**
     * Retrieves the unique key (identity hash code of the receiver) for a given handler function.
     * @param handler The handler function.
     * @return The integer key representing the handler's receiver.
     */
    fun getKey(handler: F):Int = getComponentsFromClosure(handler, { key, receiver -> return@getComponentsFromClosure key })

    /**
     * Seams that weak reference has no effect cause the closure reference (hard) holds the instance
     * Despite making it weak drops immediately. There is option to use softReference ... but not much difference
     *
     * Extracts the identity hash code and the bound receiver object from a KFunction reference.
     * This is used internally to uniquely identify and manage handlers based on their owning object.
     *
     * @param handler The KFunction handler.
     * @param result A lambda to process the extracted key and receiver.
     * @return The result of the `result` lambda.
     */
    private fun <T> getComponentsFromClosure(handler: F, result: (key:Int,receiver:Any)-> T) : T {
        val caller = handler as kotlin.jvm.internal.FunctionReference
        val receiver = caller.boundReceiver
        val key = System.identityHashCode(receiver)
        return result(key,receiver)
    }

    /**
     * Adds a handler function to the list of listeners. The handler is associated with its receiver
     * via a weak reference.
     * @param handler The function to add as a handler.
     * @return The integer key of the added handler.
     */
    fun addHandler(handler: F):Int =
        getComponentsFromClosure(handler,{key, receiver ->
            handlersList[key] = WeakReference(receiver) to handler
            return@getComponentsFromClosure key
        })

    /**
     * Removes a handler function from the list of listeners based on its closure.
     * @param closure The handler function to remove.
     * @return `true` if the handler was successfully removed, `false` otherwise.
     */
    fun removeHandler(closure:F): Boolean = removeHandler(getKey(closure))

    /**
     * Removes a handler from the list of listeners using its unique key.
     * @param key The integer key of the handler to remove.
     * @return `true` if the handler was successfully removed, `false` otherwise.
     */
    fun removeHandler(key: Int): Boolean = handlersList.remove(key)?.let { true } ?: false

    /**
     * Fires the event, invoking all registered handlers whose receivers are still alive.
     * Handlers whose receivers have been garbage collected are automatically removed.
     *
     * @param obj Variable number of arguments to pass to the handler functions.
     */
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

    /**
     * Abstract method to be implemented by subclasses to define how the handler function is invoked.
     * @param handler The handler function to invoke.
     * @param obj Variable number of arguments to pass to the handler.
     */
    protected abstract fun onFired(handler:F, vararg obj: Any)
}

/**
 * Type alias for a function that takes one argument of type [T] and returns Unit.
 */
typealias Action<T> =  (T)-> Unit
/**
 * A concrete implementation of [AbstractEventHandler] for functions that take one argument.
 * @param T The type of the single argument the event handler function expects.
 */
class EventHandler<T> : AbstractEventHandler<Action<T>>() {
    /**
     * Fires the event with a single argument.
     * @param item The argument to pass to the handler functions.
     */
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


