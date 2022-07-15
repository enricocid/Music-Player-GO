package com.iven.musicplayergo.player

interface MediaPlayerInterface {
    fun onPositionChanged(position: Int)
    fun onStateChanged()
    fun onClose()
    fun onUpdateRepeatStatus()
    fun onQueueEnabled()
    fun onQueueStartedOrEnded(started: Boolean)
    fun onBackupSong()
    fun onUpdateSleepTimerCountdown(value: Long)
    fun onStopSleepTimer()
    fun onUpdateFavorites()
}
