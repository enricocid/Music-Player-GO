package com.iven.musicplayergo.utils

import com.iven.musicplayergo.models.Music

interface UIControlInterface {
    fun onThemeChanged(isAccent: Boolean)
    fun onArtistOrFolderSelected(artistOrFolder: String, isFolder: Boolean)
    fun onSongSelected(song: Music?, songs: List<Music>?, isFromFolder: Boolean)
    fun onShuffleSongs(songs: MutableList<Music>?, isFromFolder: Boolean)
    fun onLovedSongsUpdate(clear: Boolean)
    fun onCloseActivity()
    fun onAddToQueue(song: Music)
    fun onDenyPermission()
    fun onStopPlaybackFromReloadDB()
}
