package com.petronel.spotifycontroller

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petronel.spotifycontroller.ui.theme.SpotifyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


@Composable
fun SongProgressIndicator(mediaInfo: MediaInfo, modifier: Modifier = Modifier) {
    
    var currentProgress by remember { mutableFloatStateOf(0f) }

    
    
    LaunchedEffect(mediaInfo.position, mediaInfo.isPlaying, mediaInfo.duration) {
        val startTime = System.currentTimeMillis()
        val startPosition = mediaInfo.position

        if (mediaInfo.isPlaying && mediaInfo.duration > 0) {
            
            while (isActive) {
                val elapsedMillis = System.currentTimeMillis() - startTime
                val elapsedSeconds = elapsedMillis / 1000f
                
                val newPosition = (startPosition + elapsedSeconds).coerceAtMost(mediaInfo.duration.toFloat())
                
                currentProgress = newPosition / mediaInfo.duration
                delay(16)
            }
        } else {
            if (mediaInfo.duration > 0) {
                currentProgress = mediaInfo.position.toFloat() / mediaInfo.duration
            }
        }
    }

    
    
    val animatedProgress by animateFloatAsState(
        targetValue = currentProgress,
        animationSpec = tween(durationMillis = 150), 
        label = "ProgressAnimation"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
    )
}


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

                        SongInfo(mediaInfo = mediaInfo, modifier = Modifier.padding(bottom = 30.dp))

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

@SuppressLint("Range")
@Composable
fun SongInfo(mediaInfo: MediaInfo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        Text(
            text = mediaInfo.title,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            maxLines = 1 
        )
        Text(
            text = mediaInfo.artist,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp),
            maxLines = 1
        )


        SongProgressIndicator(
            mediaInfo = mediaInfo,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = mediaInfo.position_formatted, fontSize = 12.sp)
            Text(text = mediaInfo.duration_formatted, fontSize = 12.sp)
        }
    }
}
