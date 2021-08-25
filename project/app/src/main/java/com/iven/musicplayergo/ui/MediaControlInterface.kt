package com.iven.musicplayergo.ui

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.iven.musicplayergo.models.Music

interface MediaControlInterface {
    fun onSongSelected(song: Music?, songs: List<Music>?, launchedBy: String)
    fun onSongsShuffled(
        songs: List<Music>?,
        toBeQueued: Boolean,
        launchedBy: String
    ): List<Music>?
    fun onAddToQueue(song: Music?, forcePlay: Boolean, launchedBy: String)
    fun onAddAlbumToQueue(
        songs: List<Music>?,
        launchedBy: String,
        forcePlay: Boolean
    )
    fun onUpdatePlayingAlbumSongs(songs: List<Music>?)
    fun onFavoriteAddedOrRemoved()
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
