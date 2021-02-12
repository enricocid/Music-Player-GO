package com.iven.musicplayergo.ui

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.iven.musicplayergo.models.Album
import com.iven.musicplayergo.models.Music

interface MediaControlInterface {
    fun onSongSelected(song: Music?, songs: List<Music>?, launchedBy: String)
    fun onShuffleSongs(
            albumTitle: String?,
            artistAlbums: List<Album>?,
            songs: MutableList<Music>?,
            toBeQueued: Boolean,
            launchedBy: String
    ): MutableList<Music>?
    fun onAddToQueue(song: Music?, launchedBy: String)
    fun onAddAlbumToQueue(
            songs: MutableList<Music>?,
            isAlbumOrFolder: Pair<Boolean, Music?>,
            isLovedSongs: Boolean,
            isShuffleMode: Boolean,
            clearShuffleMode: Boolean,
            launchedBy: String
    )
    fun onLovedSongAdded(song: Music?, isAdded: Boolean)
    fun onGetEqualizer(): Triple<Equalizer?, BassBoost?, Virtualizer?>

    fun onPreciseVolumeToggled()
    fun onPlaybackSpeedToggled()
    fun onHandleFocusPref()
    fun onHandleNotificationUpdate(isAdditionalActionsChanged: Boolean)
    fun onHandleCoverOptionsUpdate()
    fun onEnableEqualizer(isEnabled: Boolean)
    fun onSaveEqualizerSettings(selectedPreset: Int, bassBoost: Short, virtualizer: Short)
    fun onChangePlaybackSpeed(speed: Float)
}
