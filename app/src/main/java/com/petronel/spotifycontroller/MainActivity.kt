package com.petronel.spotifycontroller

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.petronel.spotifycontroller.ui.theme.SpotifyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.isActive

const val PREFS_NAME = "SpotifyControllerPrefs"
const val KEY_LAST_IP = "last_ip_address"

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

@Composable
fun AlbumArt(base64String: String?, modifier: Modifier = Modifier) {
    val imageBitmap by remember(base64String) {
        mutableStateOf(
            if (base64String != null) {
                try {
                    val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } catch (e: Exception) {
                    Log.e("AlbumArt", "Failed to decode Base64 string", e)
                    null
                }
            } else {
                null
            }
        )
    }

    AsyncImage(
        model = imageBitmap,
        contentDescription = "Album Art",
        modifier = modifier
            .size(300.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ExpandableActionPanel(
    modifier: Modifier = Modifier,
    panelHeightFraction: Float = 0.33f,
    collapsedHeight: Dp = 30.dp,
    collapsedWidthFraction: Float = 0.5f,
    collapsedLabel: String = "Audio Devices",
    horizontalMargin: Dp = 16.dp,
    bottomPadding: Dp = 150.dp,
    onOptionSelected: (String) -> Unit = {
        Log.d("ExpandableActionPanel", "Option selected: $it")
        WebSocketClient.send("set_audio_device $it")
        val updatedDevices = WebSocketClient.audioDevices.value.map { device ->
            device.copy(default = device.index == it.toInt())
        }
        WebSocketClient.updateAudioDevices(updatedDevices)
    }
) {
    require(panelHeightFraction in 0f..1f) { "panelHeightFraction must be 0..1" }
    require(collapsedWidthFraction in 0f..1f) { "collapsedWidthFraction must be 0..1" }

    var expanded by remember { mutableStateOf(false) }
    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp.dp
    val screenWidth = config.screenWidthDp.dp

    val transition = updateTransition(expanded, label = "panelTransition")

    val targetExpandedHeight = remember(screenHeight, panelHeightFraction) { screenHeight * panelHeightFraction }
    val targetExpandedWidth = remember(screenWidth, horizontalMargin) { screenWidth - horizontalMargin * 2 }

    val height by transition.animateDp(label = "height", transitionSpec = { tween(280) }) { if (it) targetExpandedHeight else collapsedHeight }
    val width by transition.animateDp(label = "width", transitionSpec = { tween(280) }) { if (it) targetExpandedWidth else screenWidth * collapsedWidthFraction }
    val corner by transition.animateDp(label = "corner", transitionSpec = { tween(280) }) { if (it) 24.dp else 16.dp }
    val bg by transition.animateColor(label = "bg", transitionSpec = { tween(250) }) { if (it) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary }
    val scrimAlpha by transition.animateFloat(label = "scrim", transitionSpec = { tween(200) }) { if (it) 0.35f else 0f }
    val labelAlpha by transition.animateFloat(label = "labelAlpha", transitionSpec = { tween(180) }) { if (it) 0f else 1f }

    val contentColor = if (expanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary

    Box(modifier = modifier.fillMaxSize()) {
        if (scrimAlpha > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { expanded = false }
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding)
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(corner))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    WebSocketClient.requestAudioDevices()
                    if (!expanded) expanded = true
                }
                .zIndex(10f),
            color = bg,
            contentColor = contentColor,
            shape = RoundedCornerShape(corner),
            tonalElevation = if (expanded) 8.dp else 4.dp,
            shadowElevation = if (expanded) 8.dp else 4.dp
        ) {
            if (!expanded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        collapsedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        modifier = Modifier.alpha(labelAlpha)
                    )
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Choose an action", style = MaterialTheme.typography.titleMedium)
                    WebSocketClient.audioDevices.value.forEach { device ->
                        OptionRow(text = device.name, active = device.default) {
                            onOptionSelected(device.index.toString())
                            expanded = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if(active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpotifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val context = LocalContext.current

                    var ipAddress by remember {
                        mutableStateOf(
                            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .getString(KEY_LAST_IP, "192.168") ?: "192.168"
                        )
                    }
                    val isConnected by WebSocketClient.isConnected.collectAsStateWithLifecycle()
                    val mediaInfo by WebSocketClient.mediaInfo.collectAsStateWithLifecycle()
                    val albumArtBase64 by WebSocketClient.albumArt.collectAsStateWithLifecycle()

                    LaunchedEffect(mediaInfo.title) {

                        if (isConnected && mediaInfo.isPlaying && mediaInfo.title != "Nothing Playing") {
                            Log.d("MainActivity", "New song detected: '${mediaInfo.title}'. Requesting thumbnail.")
                            WebSocketClient.requestAlbumArt()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally

                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = ipAddress,
                                onValueChange = { ipAddress = it },
                                label = { Text("PC IP Address") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = !isConnected
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (!isConnected) {
                                Button(onClick = {
                                    val wsUrl = "ws://$ipAddress:8765"
                                    val savedSuccessfully = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                        .edit()
                                        .putString(KEY_LAST_IP, ipAddress)
                                        .commit()
                                    WebSocketClient.connect(wsUrl)
                                }, enabled = ipAddress.isNotBlank()) {
                                    Text("Connect")
                                }
                            } else {
                                Button(onClick = { WebSocketClient.disconnect() }) {
                                    Text("Disconnect")
                                }
                            }
                        }




                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            if (!isConnected) {
                                Text(
                                    text = "Not Connected to PC",
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(bottom = 100.dp)
                                )
                            } else {
                                AlbumArt(
                                    base64String = albumArtBase64,
                                    modifier = Modifier.padding(bottom = 30.dp)
                                )
                                SongInfo(mediaInfo = mediaInfo, modifier = Modifier.padding(bottom = 60.dp))
                                Row(
                                    modifier = Modifier
                                        .padding(bottom = 30.dp)
                                        .zIndex(5f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    SkipButton(symbol = "≪") { WebSocketClient.send("prev") }
                                    PlayButton(isPlaying = mediaInfo.isPlaying) { WebSocketClient.send("toggle_playback") }
                                    SkipButton(symbol = "≫") { WebSocketClient.send("next") }
                                }
                            }
                        }
                    }
                    if(isConnected) {
                        ExpandableActionPanel(
                            bottomPadding = 180.dp
                        )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = mediaInfo.position_formatted, fontSize = 12.sp)
            Text(text = mediaInfo.duration_formatted, fontSize = 12.sp)
        }
    }
}
