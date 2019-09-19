package com.iven.musicplayergo.ui

import com.iven.musicplayergo.music.Music

interface SongsSheetInterface {
    fun onPopulateAndShowSheet(
        isFolder: Boolean,
        header: String,
        subheading: String,
        songs: List<Music>
    )

    fun onShowSheet()
    fun onSongSelected(song: Music)
}
