package com.ileveli.javafx_sdk.utils

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.javafx.JavaFx
import kotlin.coroutines.CoroutineContext

/**
 * A custom implementation of [CoroutineScope] that provides structured concurrency
 * with a [SupervisorJob]. This allows child coroutines to fail independently without
 * cancelling other siblings.
 *
 * This scope can be created as a standalone scope or as a child of another [CoroutineScope],
 * inheriting its context but maintaining its own [SupervisorJob] for fault isolation.
 */
class CustomCoroutineScope : CoroutineScope {
    private val job: Job
    private val context: CoroutineContext

    /**
     * Creates a new standalone [CustomCoroutineScope].
     * Coroutines launched in this scope will run on [Dispatchers.Default] by default.
     *
     * @param name A name for the coroutine scope, useful for debugging.
     */
    constructor(name: String){
        job = SupervisorJob()
        context = Dispatchers.Default + job + CoroutineName(name)
    }

    /**
     * Creates a new [CustomCoroutineScope] that is a child of a [parentScope].
     * This scope inherits the parent's [CoroutineContext] and uses its `Job` as the parent for its `SupervisorJob`,
     * ensuring proper lifecycle management within the parent's hierarchy.
     *
     * @param parentScope The parent [CoroutineScope] this new scope will be a child of.
     * @param name A name for the coroutine scope, useful for debugging.
     */
    constructor(parentScope: CoroutineScope, name: String){
        job = SupervisorJob(parentScope.coroutineContext[Job])
        context = parentScope.coroutineContext + job + CoroutineName(name)
    }

    /**
     * The [CoroutineContext] for this scope, composed of a [SupervisorJob], a dispatcher (either
     * [Dispatchers.Default] or inherited from a parent), and a [CoroutineName].
     */
    override val coroutineContext: CoroutineContext
        get() = context

}