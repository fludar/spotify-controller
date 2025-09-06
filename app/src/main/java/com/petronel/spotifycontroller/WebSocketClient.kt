package com.petronel.spotifycontroller

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener


object WebSocketClient {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun connect(serverUrl: String) {
        
        if (webSocket != null) {
            return
        }

        val request = Request.Builder().url(serverUrl).build()
        val listener = object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                
                webSocket = ws
                Log.d("WebSocket", "Connection Opened!")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                
                Log.d("WebSocket", "Receiving: $text")
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Connection Failed: ${t.message}")
                webSocket = null 
            }
        }
        
        client.newWebSocket(request, listener)
    }

    
    fun send(message: String) {
        val didSend = webSocket?.send(message)
        if (didSend == true) {
            Log.d("WebSocket", "Sent: $message")
        } else {
            Log.e("WebSocket", "Could not send message. Not connected.")
        }
    }
}