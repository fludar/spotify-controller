package com.petronel.spotifycontroller

import android.util.Log
import androidx.activity.result.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener


object WebSocketClient {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    private val json = Json { ignoreUnknownKeys = true }

    private val _mediaInfo = MutableStateFlow(MediaInfo())
    val mediaInfo = _mediaInfo.asStateFlow()

    private val _albumArt = MutableStateFlow<String?>(null)
    val albumArt: StateFlow<String?> = _albumArt
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            webSocket = ws
            Log.d("WebSocket", "Connection Opened!")
            _isConnected.value = true 
            startPolling()
        }

        override fun onMessage(ws: WebSocket, text: String) {
            
            try {
                val newMediaInfo = json.decodeFromString<MediaInfo>(text)
                _mediaInfo.value = newMediaInfo
            } catch (e: Exception) {
                
                Log.d("WebSocket", "Received non-JSON message, assuming Base64 album art.")
                _albumArt.value = text
            }
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            Log.d("WebSocket", "Closing: $code / $reason")
            stopPolling()
            webSocket = null
            _isConnected.value = false 
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            val errorMessage = t.message ?: "Unknown error"
            Log.e("WebSocket", "Connection Failed: $errorMessage", t)
            stopPolling()
            webSocket = null
            _isConnected.value = false 
            
        }
    }
    

    
    fun connect(serverUrl: String) {
        
        disconnect()

        Log.d("WebSocket", "Attempting to connect to: $serverUrl")
        val request = Request.Builder().url(serverUrl).build()
        client.newWebSocket(request, webSocketListener) 
    }
    
    fun disconnect() {
        webSocket?.close(1000, "User initiated disconnect")
        stopPolling()
        webSocket = null
        _isConnected.value = false
        _mediaInfo.value = MediaInfo() 
        _albumArt.value = null 
        Log.d("WebSocket", "Disconnected.")
    }
    fun requestAlbumArt() {
        Log.d("WebSocket", "Sending command: get_thumbnail")
        send("get_thumbnail")
    }

    private fun startPolling() {
        pollingJob?.cancel() 
        pollingJob = clientScope.launch {
            while (isActive) { 
                send("get_media")
                delay(500)
            }
        }
        Log.d("WebSocket", "Started polling for media info.")
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d("WebSocket", "Stopped polling.")
    }

    fun send(message: String) {
        if (_isConnected.value) { 
            val didSend = webSocket?.send(message)
            if (didSend == true) {
                if (message != "get_media") { 
                    Log.d("WebSocket", "Sent: $message")
                }
            } else {
                Log.e("WebSocket", "Could not send message. WebSocket is null or failed to send.")
            }
        } else {
            Log.w("WebSocket", "Tried to send message '$message' but not connected.")
        }
    }
    
}