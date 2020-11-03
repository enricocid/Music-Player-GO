package com.iven.musicplayergo.models

import com.iven.musicplayergo.enums.LaunchedBy

data class SavedMusic(
    val artist: String?,
    val title: String?,
    val displayName: String?,
    val year: Int,
    val startFrom: Int,
    val duration: Long,
    val album: String?,
    val launchedBy: LaunchedBy
)
