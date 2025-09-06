package com.petronel.spotifycontroller

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaInfo(
    @SerialName("is_playing")
    val isPlaying: Boolean = false,
    val title: String = "Nothing Playing",
    val artist: String = "---",
    val app: String = "Spotify",
    val position: Int = 0,
    val duration: Int = 0,
    val position_formatted: String = "0:00",
    val duration_formatted: String = "0:00",
    val progress_percent: Float = 0f
)