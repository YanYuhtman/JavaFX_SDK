package com.ileveli.javafx_sdk.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.max
import kotlin.math.min

data class FPS_Monitor (
    val maxBundleSize: Int = 200,
    val avgFrameRateThreshold: Int = 30,
    val deltaThreshold: Int = 10
) {

    private var lastTime: Long? = null
    var instantFps: Long = 0
        private set
    var avgFps: Long = 0
        private set
    fun frame() {
        synchronized(this) {
            if(lastTime == null) {
                lastTime = System.nanoTime()
                return
            }
            val now = System.nanoTime()
            instantFps = 1_000_000_000 / (now - lastTime!!)
            avgFps = ((avgFps + instantFps) shr 1)
            lastTime = now
        }
//        println("instantFps  fps: $instantFps, avg $avgFps")
    }


    fun nextBundleSize(current: Int): Int {
        return if (avgFps > avgFrameRateThreshold)
            min(maxBundleSize, current + deltaThreshold)
        else
            max(1, current - deltaThreshold)
    }

    fun reset() {
        lastTime = null
        instantFps = 0
        avgFps = 0
    }
}
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
    val bundleSize: Int = 10,
    val enableFpsMonitor: Boolean = true,
    val fpsMonitor: FPS_Monitor = FPS_Monitor()

){
    private var _buffer = mutableListOf<T>()
    private var _isPoolRunning = false
    private fun MutableList<T>.addSynced(record: T) = synchronized(_buffer){
        _buffer.add(record)
    }
    private fun MutableList<T>.removeFirstOrNullSynced(): T? = synchronized(_buffer){
        _buffer.removeFirstOrNull()
    }
    fun add(record:T){
        _buffer.addSynced(record)
        if(!_isPoolRunning) {
            scopeProvider().launch {
                _isPoolRunning = true
                var _bundleSize = bundleSize
                withContext(dispatcher) {
                    while (!_buffer.isEmpty()) {
                        fpsMonitor.frame()
                        if(enableFpsMonitor)
                            _bundleSize = fpsMonitor.nextBundleSize(_bundleSize)
//                        if(fpsMonitor != null)
//                            println("Performance: bundleSize: $_bundleSize instant: ${fpsMonitor.instantFps} fps: ${fpsMonitor.avgFps}")
                        //In case there are many records coming shortly, without buffering UI will freeze
                        if(delayTime <= 0) yield() else delay(delayTime)
                        for(i in 0..<_bundleSize) {
                            _buffer.removeFirstOrNullSynced()?.let { processRecord(it) } ?: continue
                        }
                    }
                    fpsMonitor.reset()
                }
                _isPoolRunning = false
            }
        }
    }
    fun clear() = synchronized(_buffer){
        _buffer.clear()
    }
}