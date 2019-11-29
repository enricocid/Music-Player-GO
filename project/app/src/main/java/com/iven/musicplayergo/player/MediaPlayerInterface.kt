package com.iven.musicplayergo.player

interface MediaPlayerInterface {
    fun onPositionChanged(position: Int)
    fun onStateChanged()
    fun onPlaybackCompleted()
    fun onClose(stopPlayback: Boolean)
    fun onQueueEnabled()
    fun onQueueCleared()
    fun onQueueStartedOrEnded(started: Boolean)
}
