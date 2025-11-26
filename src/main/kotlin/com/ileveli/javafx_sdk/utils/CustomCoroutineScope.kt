package com.ileveli.javafx_sdk.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class CustomCoroutineScope : CoroutineScope {
    private val job = SupervisorJob()
    private val context: CoroutineContext
    constructor():this(Dispatchers.Main)
    constructor(baseDispatcher: CoroutineDispatcher){
        context = baseDispatcher + job
    }
    override val coroutineContext: CoroutineContext
        get() = context

    val isCanceled: Boolean
        get() = job.isCancelled
}