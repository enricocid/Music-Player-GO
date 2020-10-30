package com.iven.musicplayergo.ui

import com.iven.musicplayergo.enums.LaunchedBy
import com.iven.musicplayergo.models.Music

interface UIControlInterface {
    fun onAppearanceChanged(isAccentChanged: Boolean, restoreSettings: Boolean)
    fun onThemeChanged()
    fun onArtistOrFolderSelected(artistOrFolder: String, launchedBy: LaunchedBy)
    fun onSongSelected(song: Music?, songs: List<Music>?, launchedBy: LaunchedBy)
    fun onShuffleSongs(songs: MutableList<Music>?, launchedBy: LaunchedBy)
    fun onLovedSongsUpdate(clear: Boolean)
    fun onCloseActivity()
    fun onAddToQueue(song: Music?)
    fun onAddToFilter(stringToFilter: String?)
    fun onDenyPermission()
    fun onHandleFocusPref()
    fun onHandleCoversPref()
}
