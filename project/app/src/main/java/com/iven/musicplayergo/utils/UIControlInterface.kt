package com.iven.musicplayergo.utils

import com.iven.musicplayergo.models.Music

interface UIControlInterface {
    fun onAppearanceChanged(isAccentChanged: Boolean)
    fun onThemeChanged()
    fun onArtistOrFolderSelected(artistOrFolder: String, isFolder: Boolean)
    fun onSongSelected(song: Music?, songs: List<Music>?, isFromFolder: Boolean)
    fun onShuffleSongs(songs: MutableList<Music>?, isFromFolder: Boolean)
    fun onLovedSongsUpdate(clear: Boolean)
    fun onCloseActivity()
    fun onAddToQueue(song: Music?)
    fun onAddToFilter(stringToFilter: String?)
    fun onDenyPermission()
    fun onStopPlaybackFromReloadDB()
}
