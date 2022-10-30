package com.iven.musicplayergo.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SavedEqualizerSettings(
    val enabled: Boolean,
    val preset: Int,
    val bandsSettings: List<Short>?,
    val bassBoost: Short,
    val virtualizer: Short
)
