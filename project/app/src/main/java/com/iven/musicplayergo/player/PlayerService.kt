package com.iven.musicplayergo.player

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.iven.musicplayergo.goPreferences

class PlayerService : Service() {

    // Binder given to clients
    private val binder = LocalBinder()

    // Check if is already running
    var isRunning: Boolean = false

    //media player
    lateinit var mediaPlayerHolder: MediaPlayerHolder
    lateinit var musicNotificationManager: MusicNotificationManager
    var isRestoredFromPause = false

    override fun onDestroy() {
        super.onDestroy()

        isRunning = false

        if (::mediaPlayerHolder.isInitialized) {
            //saves last played song and its position
            if (mediaPlayerHolder.isCurrentSong) goPreferences.lastPlayedSong =
                Pair(mediaPlayerHolder.currentSong.first, mediaPlayerHolder.playerPosition)

            mediaPlayerHolder.release()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /*This mode makes sense for things that will be explicitly started
        and stopped to run for arbitrary periods of time, such as a service
        performing background music playback.*/
        isRunning = true
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        if (!::mediaPlayerHolder.isInitialized) {
            mediaPlayerHolder = MediaPlayerHolder(this).apply {
                registerActionsReceiver()
            }
            musicNotificationManager = MusicNotificationManager(this)
        }
        return binder
    }

    inner class LocalBinder : Binder() {
        // Return this instance of PlayerService so we can call public methods
        fun getService() = this@PlayerService
    }
}
