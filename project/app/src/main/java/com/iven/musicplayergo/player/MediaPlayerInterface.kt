package com.iven.musicplayergo.player

interface MediaPlayerInterface {
    fun onPositionChanged(position: Int)
    fun onStateChanged()
    fun onPlaybackCompleted()
    fun onClose()
}