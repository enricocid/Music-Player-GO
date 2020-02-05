package com.iven.musicplayergo.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Music(
    @PrimaryKey val id: Long?,
    val artist: String?,
    val year: Int,
    val track: Int,
    val title: String?,
    val displayName: String?,
    val duration: Long,
    val album: String?,
    val relativePath: String?
)
