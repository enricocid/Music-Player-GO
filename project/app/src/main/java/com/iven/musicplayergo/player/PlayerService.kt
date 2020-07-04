package com.iven.musicplayergo.player

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Parcelable
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toSavedMusic
import com.iven.musicplayergo.extensions.toToast
import com.iven.musicplayergo.goPreferences


class PlayerService : Service() {

    // Binder given to clients
    private val binder = LocalBinder()

    // Check if is already running
    var isRunning = false
    var isRestoredFromPause = false

    // Media player
    private val mMediaPlayerHolder: MediaPlayerHolder get() = MediaPlayerHolder.getInstance()
    private lateinit var mNotificationActionsReceiver: NotificationReceiver
    lateinit var musicNotificationManager: MusicNotificationManager

    private lateinit var mMediaSessionCompat: MediaSessionCompat

    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onSeekTo(pos: Long) {
            mMediaPlayerHolder.seekTo(
                pos.toInt(),
                updatePlaybackStatus = true,
                restoreProgressCallBack = false
            )
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?) =
            handleMediaIntent(mediaButtonEvent)
    }

    private fun configureMediaSession() {
        mMediaSessionCompat = MediaSessionCompat(this, packageName).apply {
            isActive = true
            setCallback(mMediaSessionCallback)
        }
    }

    fun getMediaSession(): MediaSessionCompat {
        return mMediaSessionCompat
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        if (mMediaPlayerHolder.isCurrentSong && !mMediaPlayerHolder.isPlaying) {

            stopForeground(true)
            stopSelf()

            // Saves last played song and its position
            mMediaPlayerHolder.apply {
                currentSong.first?.let { musicToSave ->
                    goPreferences.latestPlayedSong =
                        musicToSave.toSavedMusic(playerPosition!!, launchedBy)
                }
            }

            goPreferences.latestVolume = mMediaPlayerHolder.currentVolumeInPercent

            if (::mMediaSessionCompat.isInitialized && mMediaSessionCompat.isActive) {
                mMediaSessionCompat.isActive = false
                mMediaSessionCompat.setCallback(null)
                mMediaSessionCompat.release()
            }

            isRunning = false

            mMediaPlayerHolder.release()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /*This mode makes sense for things that will be explicitly started
        and stopped to run for arbitrary periods of time, such as a service
        performing background default_cover playback.*/
        isRunning = true
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        if (!::mNotificationActionsReceiver.isInitialized) {
            mNotificationActionsReceiver = NotificationReceiver(this, mMediaPlayerHolder)
            val intentFilter = mNotificationActionsReceiver.createIntentFilter()
            registerReceiver(mNotificationActionsReceiver, intentFilter)

            musicNotificationManager = MusicNotificationManager(this)
        }
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        configureMediaSession()
    }

    inner class LocalBinder : Binder() {
        // Return this instance of PlayerService so we can call public methods
        fun getService() = this@PlayerService
    }

    fun unregisterActionsReceiver() {
        try {
            unregisterReceiver(mNotificationActionsReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    private fun handleMediaIntent(intent: Intent?): Boolean {

        var isSuccess = false

        try {
            intent?.let {
                val event =
                    intent.getParcelableExtra<Parcelable>(Intent.EXTRA_KEY_EVENT) as KeyEvent
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                            mMediaPlayerHolder.resumeOrPause()
                            isSuccess = true
                        }
                        KeyEvent.KEYCODE_MEDIA_CLOSE, KeyEvent.KEYCODE_MEDIA_STOP -> {
                            mMediaPlayerHolder.stopPlaybackService(stopPlayback = true)
                            isSuccess = true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            mMediaPlayerHolder.skip(false)
                            isSuccess = true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            mMediaPlayerHolder.skip(true)
                            isSuccess = true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            mMediaPlayerHolder.repeatSong()
                            isSuccess = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            isSuccess = false
            getString(R.string.error_media_buttons).toToast(this)
            e.printStackTrace()
        }
        return isSuccess
    }
}
