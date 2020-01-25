package com.iven.musicplayergo.ui

import com.iven.musicplayergo.music.Music

interface UIControlInterface {
    fun onThemeChanged(isAccent: Boolean)
    fun onArtistOrFolderSelected(artistOrFolder: String, isFolder: Boolean)
    fun onSongSelected(song: Music?, songs: List<Music>?, isFolderAlbum: Boolean)
    fun onShuffleSongs(songs: MutableList<Music>?, isFolderAlbum: Boolean)
    fun onLovedSongsUpdate(clear: Boolean)
    fun onCloseActivity()
    fun onAddToQueue(song: Music)
    fun onDenyPermission()
}
