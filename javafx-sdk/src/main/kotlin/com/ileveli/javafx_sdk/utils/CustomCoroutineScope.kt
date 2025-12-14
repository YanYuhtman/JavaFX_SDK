package com.ileveli.javafx_sdk.utils

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.javafx.JavaFx
import kotlin.coroutines.CoroutineContext

class CustomCoroutineScope : CoroutineScope {
    private val job: Job
    private val context: CoroutineContext
    constructor(name: String){
        job = SupervisorJob()
        context = Dispatchers.Default + job + CoroutineName(name)
    }
    constructor(parentScope: CoroutineScope, name: String){
        job = SupervisorJob(parentScope.coroutineContext[Job])
        context = parentScope.coroutineContext + job + CoroutineName(name)
    }
    override val coroutineContext: CoroutineContext
        get() = context


}