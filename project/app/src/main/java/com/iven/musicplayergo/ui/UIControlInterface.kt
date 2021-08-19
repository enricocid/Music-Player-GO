package com.iven.musicplayergo.ui

import com.iven.musicplayergo.models.Music


interface UIControlInterface {
    fun onAppearanceChanged(isThemeChanged: Boolean)
    fun onOpenNewDetailsFragment()
    fun onArtistOrFolderSelected(artistOrFolder: String, launchedBy: String)
    fun onFavoritesUpdated(clear: Boolean)
    fun onCloseActivity()
    fun onAddToFilter(stringToFilter: String?)
    fun onSongsChanged(sortedMusic: List<Music>?)
    fun onDenyPermission()
}
