package com.iven.musicplayergo.ui

import com.iven.musicplayergo.music.Music

interface UIControlInterface {
    fun onAccentUpdated()
    fun onArtistOrFolderSelected(artistOrFolder: String, isFolder: Boolean)
    fun onSongSelected(song: Music?, songs: List<Music>?)
    fun onShuffleSongs(songs: MutableList<Music>?)
    fun onLovedSongsUpdate(clear: Boolean)
    fun onCloseActivity()
    fun onAddToQueue(song: Music)
}
