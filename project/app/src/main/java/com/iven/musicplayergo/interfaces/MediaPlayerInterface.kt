package com.iven.musicplayergo.interfaces

import com.iven.musicplayergo.dialogs.NowPlayingBottomSheet
import com.iven.musicplayergo.enums.LaunchedBy
import com.iven.musicplayergo.models.Music

interface MediaPlayerInterface {
    fun onSongSelected(song: Music?, songs: List<Music>?, launchedBy: LaunchedBy)
    fun onStartPlayback(song: Music?, songs: List<Music>?, launchedBy: LaunchedBy)
    fun onStateChanged()
    fun onPositionChanged(position: Int)
    fun onSetRepeatNP()
    fun onOpenPlayingArtistAlbumNP()
    fun onSkipNP(isNext: Boolean)
    fun onPlaybackCompleted()
    fun onUpdateRepeatStatus()
    fun onShuffleSongs(songs: MutableList<Music>?, launchedBy: LaunchedBy)
    fun onAddToQueue(song: Music?)
    fun onQueueEnabled()
    fun onQueueCleared()
    fun onQueueStartedOrEnded(started: Boolean)
    fun onLovedSongUpdate(clear: Boolean)
    fun onClose()
    fun onSaveSongToPref()
    fun onHandleFocusPref()
    fun onBottomSheetCreated(nowPlayingBottomSheet: NowPlayingBottomSheet)
    fun onDismissNP()
    fun onThemeApplied()
}
