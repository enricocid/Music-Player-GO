package com.iven.musicplayergo.interfaces

import com.iven.musicplayergo.enums.LaunchedBy

interface UIControlInterface {
    fun onArtistOrFolderSelected(artistOrFolder: String, launchedBy: LaunchedBy)
    fun onAppearanceChanged(isAccentChanged: Boolean, restoreSettings: Boolean)
    fun onThemeChanged()
    fun onCloseActivity()
    fun onAddToFilter(stringToFilter: String?)
    fun onError(errorType: String?)
    fun onOpenLovedSongsDialog()
}
