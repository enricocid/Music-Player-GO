package com.iven.musicplayergo.ui

interface UIControlInterface {
    fun onAppearanceChanged(isThemeChanged: Boolean)
    fun onOpenNewDetailsFragment()
    fun onArtistOrFolderSelected(artistOrFolder: String, launchedBy: String)
    fun onFavoritesUpdated(clear: Boolean)
    fun onFavoriteAddedOrRemoved()
    fun onCloseActivity()
    fun onAddToFilter(stringsToFilter: List<String>?)
    fun onFiltersCleared()
    fun onDenyPermission()
    fun onOpenPlayingArtistAlbum()
    fun onOpenEqualizer()
    fun onOpenSleepTimerDialog()
    fun onEnableEqualizer()
}
