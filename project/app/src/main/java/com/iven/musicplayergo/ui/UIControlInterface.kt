package com.iven.musicplayergo.ui

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.iven.musicplayergo.models.Album
import com.iven.musicplayergo.models.Music

interface UIControlInterface {
    fun onAppearanceChanged(isAccentChanged: Boolean, restoreSettings: Boolean)
    fun onThemeChanged()
    fun onPreciseVolumeToggled()
    fun onArtistOrFolderSelected(artistOrFolder: String, launchedBy: String)
    fun onSongSelected(song: Music?, songs: List<Music>?, launchedBy: String)
    fun onShuffleSongs(
            albumTitle: String?,
            artistAlbums: List<Album>?,
            songs: MutableList<Music>?,
            toBeQueued: Boolean,
            launchedBy: String
    ): MutableList<Music>?

    fun onLovedSongsUpdate(clear: Boolean)
    fun onLovedSongAdded(song: Music?, isAdded: Boolean)
    fun onCloseActivity()
    fun onAddToQueue(song: Music?, launchedBy: String)
    fun onAddAlbumToQueue(
            songs: MutableList<Music>?,
            isAlbumOrFolder: Pair<Boolean, Music?>,
            isLovedSongs: Boolean,
            isShuffleMode: Boolean,
            clearShuffleMode: Boolean,
            launchedBy: String
    )

    fun onAddToFilter(stringToFilter: String?)
    fun onDenyPermission()
    fun onHandleFocusPref()
    fun onHandleNotificationUpdate(isAdditionalActionsChanged: Boolean)
    fun onGetEqualizer(): Triple<Equalizer, BassBoost, Virtualizer>
    fun onEnableEqualizer(isEnabled: Boolean)
    fun onSaveEqualizerSettings(selectedPreset: Int, bassBoost: Short, virtualizer: Short)
}
