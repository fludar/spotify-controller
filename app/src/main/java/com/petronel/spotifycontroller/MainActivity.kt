package com.petronel.spotifycontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petronel.spotifycontroller.ui.theme.SpotifyTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpotifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val mediaInfo by WebSocketClient.mediaInfo.collectAsStateWithLifecycle()
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        LaunchedEffect(Unit) {
                            WebSocketClient.connect("ws://192.168.5.8:8765")

                        }

                        Row(
                            modifier = Modifier.padding(bottom = 30.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SkipButton(symbol = "≪") { WebSocketClient.send("prev") }
                            PlayButton(modifier = Modifier.padding(bottom = 30.dp), isPlaying = mediaInfo.isPlaying) { WebSocketClient.send("toggle_playback") }
                            SkipButton(symbol = "≫") { WebSocketClient.send("next") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayButton(modifier: Modifier = Modifier, isPlaying: Boolean, onClick: () -> Unit) {

    val textFontSize = 70.sp
    val buttonSize = 100.dp
    val buttonText = if (isPlaying) "∥" else "►"

    Button(
        onClick = {
                    onClick()
                  },
        modifier = modifier.size(buttonSize),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(text = buttonText , fontSize = textFontSize, fontFamily = FontFamily.Serif, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterVertically))
    }
}

@Composable
fun SkipButton(modifier: Modifier = Modifier, symbol: String, onClick: () -> Unit) {
    val buttonSize = 70.dp
    val textFontSize = 30.sp


    Button(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(text = symbol, fontSize = textFontSize, fontFamily = FontFamily.Serif, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterVertically))

    }
}