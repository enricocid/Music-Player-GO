package com.iven.musicplayergo.musicloadutils

data class Music(
    val artist: String?,
    val year: Int,
    val track: Int,
    val title: String?,
    val displayName: String?,
    val duration: Long,
    val album: String?,
    val relativePath: String?,
    val id: Long?
)
