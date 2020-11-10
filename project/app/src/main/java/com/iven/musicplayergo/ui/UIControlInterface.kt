package com.iven.musicplayergo.ui

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
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
    fun onHandleNotificationUpdate(isAdditionalActionsChanged: Boolean)
    fun onGetEqualizer(): Triple<Equalizer, BassBoost, Virtualizer>?
    fun onEnableEqualizer(isEnabled: Boolean)
    fun onSaveEqualizerSettings(selectedPreset: Int, bassBoost: Short, virtualizer: Short)
}
