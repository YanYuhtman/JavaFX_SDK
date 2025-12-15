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

/**
 * A data class to monitor Frames Per Second (FPS) and dynamically adjust parameters
 * like bundle size based on performance.
 *
 * @property maxBundleSize The maximum number of items to process in a single bundle.
 * @property avgFrameRateThreshold The average FPS above which the bundle size can increase.
 * @property deltaThreshold The amount by which the bundle size increases or decreases.
 */
data class FPS_Monitor (
    val maxBundleSize: Int = 200,
    val avgFrameRateThreshold: Int = 30,
    val deltaThreshold: Int = 10
) {

    private var lastTime: Long? = null
    /**
     * The instantaneous frames per second.
     */
    var instantFps: Long = 0
        private set
    /**
     * The smoothed average frames per second.
     */
    var avgFps: Long = 0
        private set

    /**
     * Records a frame, updating the instantaneous and average FPS.
     * Call this method once per frame rendering.
     */
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

    /**
     * Calculates the next optimal bundle size based on the average FPS.
     * If FPS is above the threshold, bundle size increases; otherwise, it decreases.
     *
     * @param current The current bundle size.
     * @return The new, adjusted bundle size.
     */
    fun nextBundleSize(current: Int): Int {
        return if (avgFps > avgFrameRateThreshold)
            min(maxBundleSize, current + deltaThreshold)
        else
            max(1, current - deltaThreshold)
    }

    /**
     * Resets the FPS monitor's internal state.
     */
    fun reset() {
        lastTime = null
        instantFps = 0
        avgFps = 0
    }
}
/**
 * A utility class that buffers incoming data and processes it in batches using coroutines.
 * This is particularly useful for smoothing out UI updates or other operations that
 * might otherwise cause the application to freeze if processed immediately.
 *
 * @param processRecord A callback function that defines how each buffered record should be processed.
 * @param scope The [CoroutineScope] in which the processing coroutine will be launched. Defaults to [GlobalScope].
 * @param scopeProvider A lazy provider for the [CoroutineScope], useful if the scope isn't available at instantiation.
 * @param dispatcher The [CoroutineDispatcher] on which `processRecord` will be executed. Defaults to [Dispatchers.Main].
 * @param delayTime The time in milliseconds to delay between processing bundles. If `0` or less, `yield()` is used instead of `delay()`.
 * @param bundleSize The initial number of records to process in a single batch before yielding/delaying.
 * @param enableFpsMonitor If `true`, an [FPS_Monitor] will be used to dynamically adjust `bundleSize`.
 * @param fpsMonitor An optional [FPS_Monitor] instance to use if `enableFpsMonitor` is true.
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

    /**
     * Adds a new record to the buffer. If the processing pool is not running, it starts a new coroutine
     * to process the buffered records in batches.
     *
     * @param record The data record to add to the buffer.
     */
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

    /**
     * Clears all records currently in the buffer.
     */
    fun clear() = synchronized(_buffer){
        _buffer.clear()
    }
}