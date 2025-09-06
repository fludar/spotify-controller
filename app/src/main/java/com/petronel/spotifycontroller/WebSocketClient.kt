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

    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    fun connect(serverUrl: String) {
        
        if (webSocket != null) {
            return
        }

        val request = Request.Builder().url(serverUrl).build()
        val listener = object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                webSocket = ws
                Log.d("WebSocket", "Connection Opened!")
                startPolling()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d("WebSocket", "Receiving: $text")
                try {
                    val newMediaInfo = json.decodeFromString<MediaInfo>(text)
                    _mediaInfo.value = newMediaInfo
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error parsing JSON: ${e.message}")
                    Log.d("WebSocket", "Received maybe a base64 image")
                    _albumArt.value = text
                }
            }
            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closing: $code / $reason")
                stopPolling()
                webSocket = null
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Connection Failed: ${t.message}")
                stopPolling()
                webSocket = null 
            }
        }
        
        client.newWebSocket(request, listener)
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
        val didSend = webSocket?.send(message)
        if (didSend == true) {
            
            if (message != "get_media") {
                Log.d("WebSocket", "Sent: $message")
            }
        } else {
            Log.e("WebSocket", "Could not send message. Not connected.")
        }
    }
}