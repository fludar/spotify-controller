package com.petronel.spotifycontroller

import kotlinx.serialization.Serializable

@Serializable
data class AudioDevice(
    val index: Int,
    val name: String,
    val default: Boolean
)