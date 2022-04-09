package com.iven.musicplayergo.ui

import com.iven.musicplayergo.dialogs.RecyclerSheet

interface UIControlInterface {
    fun onAppearanceChanged(isThemeChanged: Boolean)
    fun onOpenNewDetailsFragment()
    fun onArtistOrFolderSelected(artistOrFolder: String, launchedBy: String)
    fun onFavoritesUpdated(clear: Boolean)
    fun onFavoriteAddedOrRemoved()
    fun onCloseActivity()
    fun onAddToFilter(stringToFilter: String?)
    fun onDenyPermission()
    fun onOpenPlayingArtistAlbum()
    fun onOpenEqualizer()
    fun onOpenSleepTimerDialog()
}
