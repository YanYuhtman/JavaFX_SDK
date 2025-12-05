package com.ileveli.javafx_sdk.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * This class buffers input data. It's useful in case a stream to harsh
 * For instance - updating UI
 * @param processRecord - this is a callback that will be triggered according buffered properties
 * @param scope - this is a Coroutine scope (if scope isn't defined at instance creation time @see scopeProvider)
 * @param scopeProvider - the closure getting the scope lazily
 * @param dispatcher - coroutine dispatcher
 * @param delayTime - delay time in millisecond, time for dispatched context give a rest if it <= 0, yield will be used
 * @param bundleSize - records count to process without rest
 * @
 */
class BufferedUpdater<T> constructor(
    val processRecord: (record:T)-> Unit,
    val scope: CoroutineScope = GlobalScope,
    val scopeProvider: ()-> CoroutineScope = {scope},
    val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    val delayTime: Long = 0,
    val bundleSize: Int = 10
){
    private var _buffer = mutableListOf<T>()
    private var _isPoolRunning = false
    private fun MutableList<T>.addSynced(record: T) = synchronized(_buffer){
        _buffer.add(record)
    }
    private fun MutableList<T>.removeFirstOrNullSynced(): T? = synchronized(_buffer){
        _buffer.removeFirstOrNull()
    }

    public fun add(record:T){
        _buffer.addSynced(record)
        if(!_isPoolRunning) {
            scopeProvider().launch {
                _isPoolRunning = true
                withContext(dispatcher) {
                    while (!_buffer.isEmpty()) {
                        //In case there are many records coming shortly, without buffering UI will freeze
                        if(delayTime <= 0) yield() else delay(delayTime)
                        for(i in 0..<bundleSize)
                            _buffer.removeFirstOrNullSynced()?.let { processRecord(it) } ?: continue
                    }
                }
                _isPoolRunning = false
            }
        }
    }
    fun clear() = synchronized(_buffer){
        _buffer.clear()
    }
}