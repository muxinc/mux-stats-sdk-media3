package com.mux.player.media3.automatedtests.mockup.http

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class KtorHTTPServer @JvmOverloads constructor(
    private val port: Int,
    private val bandwidthLimit: Int,
    private val context: Context = ApplicationProvider.getApplicationContext()
) : ConnectionListener {

    companion object {
        private const val TAG = "KtorHTTPServer"
        const val FILE_NAME_RESPONSE_HEADER = "Automated-Test-File-Name"
        const val REQUEST_UUID_HEADER = "Request-segment-uuid"
        const val X_CDN_RESPONSE_HEADER = "x-cdn"
        const val CONTENT_TYPE_RESPONSE_HEADER = "Content-Type"
        const val REQUEST_NETWORK_DELAY_HEADER = "Request-Delay"
    }

    private var server: NettyApplicationEngine? = null
    private var job: Job? = null
    private val isRunning = AtomicBoolean(false)
    private var startupError: Throwable? = null
    private val serverStarted = AtomicBoolean(false)
    
    private var networkJamEndPeriod = -1L
    private var networkJamFactor = 1
    private var constantJam = false
    private var manifestDelay = 0L
    
    private val additionalHeaders = ConcurrentHashMap<String, String>()
    private val segmentsServed = ConcurrentHashMap<String, SegmentStatistics>()
    
    private val serverLock = ReentrantLock()
    private val newMediaSegmentStarted = serverLock.newCondition()
    
    init {
        start()
    }

    private fun start() {
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                server = embeddedServer(Netty, port = port) {
                    configureRouting()
                }.start(wait = false)
                
                isRunning.set(true)
                serverStarted.set(true)
                Log.i(TAG, "Ktor server started on port $port")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Ktor server", e)
                startupError = e
                serverStarted.set(false)
                isRunning.set(false)
            }
        }
    }

    fun isServerRunning(): Boolean = serverStarted.get()
    
    fun getStartupError(): Throwable? = startupError
    
    fun ensureServerStarted() {
        if (!isServerRunning() && startupError != null) {
            throw RuntimeException("Server failed to start", startupError)
        }
    }

    private fun Application.configureRouting() {
        routing {
            // Serve all assets
            get("/{path...}") {
                val path = call.parameters.getAll("path")?.joinToString("/") ?: ""
                val requestUuid = call.request.headers[REQUEST_UUID_HEADER]
                val networkDelay = call.request.headers[REQUEST_NETWORK_DELAY_HEADER]?.toLongOrNull() ?: manifestDelay
                
                Log.d(TAG, "Serving asset: $path")
                
                // Apply network delay if specified
                if (networkDelay > 0) {
                    delay(networkDelay)
                }
                
                try {
                    val assetStream = context.assets.open(path)
                    val contentType = getContentType(path)
                    val fileName = path.substringAfterLast("/")
                    
                    // Set response headers
                    call.response.headers.append(FILE_NAME_RESPONSE_HEADER, fileName)
                    call.response.headers.append(CONTENT_TYPE_RESPONSE_HEADER, contentType)
                    
                    requestUuid?.let {
                        call.response.headers.append(REQUEST_UUID_HEADER, it)
                    }
                    
                    // Add additional headers
                    additionalHeaders.forEach { (key, value) ->
                        call.response.headers.append(key, value)
                    }
                    
                    // Handle bandwidth limiting and network jamming
                    val startTime = System.currentTimeMillis()
                    val bytes = if (shouldApplyNetworkJam()) {
                        serveWithThrottling(assetStream, call, bandwidthLimit / networkJamFactor)
                    } else {
                        serveWithThrottling(assetStream, call, bandwidthLimit)
                    }
                    val endTime = System.currentTimeMillis()
                    
                    // Track segment statistics
                    requestUuid?.let { uuid ->
                        val stats = SegmentStatistics().apply {
                            setStartServeTimeMs(startTime)
                            setFinishServeTimeMs(endTime)
                            setBytesServed(bytes.toLong())
                        }
                        segmentServed(uuid, stats)
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error serving asset: $path", e)
                    call.respond(HttpStatusCode.NotFound, "Asset not found: $path")
                }
            }
        }
    }

    private suspend fun serveWithThrottling(
        inputStream: InputStream,
        call: ApplicationCall,
        bandwidthBytesPerSecond: Int
    ): Int {
        val bufferSize = 8192
        val buffer = ByteArray(bufferSize)
        val startTime = System.currentTimeMillis()
        var totalBytesServed = 0
        
        inputStream.use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                call.response.outputStream().write(buffer, 0, bytesRead)
                totalBytesServed += bytesRead
                
                // Apply bandwidth throttling
                if (bandwidthBytesPerSecond > 0) {
                    val elapsedMs = System.currentTimeMillis() - startTime
                    val expectedMs = (totalBytesServed * 1000L) / bandwidthBytesPerSecond
                    val delayMs = expectedMs - elapsedMs
                    
                    if (delayMs > 0) {
                        delay(delayMs)
                    }
                }
            }
        }
        
        return totalBytesServed
    }

    private fun shouldApplyNetworkJam(): Boolean {
        return if (constantJam) {
            networkJamFactor > 1
        } else {
            networkJamEndPeriod > 0 && System.currentTimeMillis() < networkJamEndPeriod
        }
    }

    private fun getContentType(path: String): String {
        return when (path.substringAfterLast(".").lowercase()) {
            "mp4" -> "video/mp4"
            "m4s" -> "video/mp4"
            "mpd" -> "application/dash+xml"
            "m3u8" -> "application/x-mpegURL"
            "aac" -> "audio/aac"
            "xml" -> "application/xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "application/octet-stream"
        }
    }

    fun waitForNextSegmentToLoad(timeoutInMs: Long): Boolean {
        return try {
            serverLock.withLock {
                newMediaSegmentStarted.await(timeoutInMs, TimeUnit.MILLISECONDS)
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while waiting for next segment", e)
            false
        }
    }

    fun getSegmentStatistics(segmentUuid: String): SegmentStatistics? {
        return segmentsServed[segmentUuid]
    }

    fun jamNetwork(jamPeriod: Long, jamFactor: Int, constantJam: Boolean) {
        this.networkJamEndPeriod = System.currentTimeMillis() + jamPeriod
        this.networkJamFactor = jamFactor
        this.constantJam = constantJam
        Log.d(TAG, "Network jam applied: period=$jamPeriod, factor=$jamFactor, constant=$constantJam")
    }

    fun setHLSManifestDelay(manifestDelay: Long) {
        this.manifestDelay = manifestDelay
        Log.d(TAG, "HLS manifest delay set to: $manifestDelay ms")
    }

    fun setAdditionalHeader(headerName: String, headerValue: String) {
        additionalHeaders[headerName] = headerValue
    }

    fun kill() {
        isRunning.set(false)
        
        runBlocking {
            try {
                server?.stop(1000, 2000)
                job?.cancel()
                job?.join()
                Log.i(TAG, "Ktor server stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping Ktor server", e)
            }
        }
    }

    override fun segmentServed(requestUuid: String, segmentStat: SegmentStatistics) {
        segmentsServed[requestUuid] = segmentStat
        serverLock.withLock {
            newMediaSegmentStarted.signalAll()
        }
    }
}