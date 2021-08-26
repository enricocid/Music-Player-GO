package com.iven.musicplayergo.player

interface MediaPlayerInterface {
    fun onPositionChanged(position: Int)
    fun onStateChanged()
    fun onPlaybackCompleted()
    fun onClose()
    fun onUpdateRepeatStatus()
    fun onQueueEnabled()
    fun onQueueStartedOrEnded(started: Boolean)
    fun onSaveSong()
    fun onFocusLoss()
    fun onPlaylistEnded()
}
