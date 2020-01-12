package com.iven.musicplayergo.player

import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import androidx.media.MediaBrowserServiceCompat
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences


class PlayerService : MediaBrowserServiceCompat() {

    // Binder given to clients
    private val binder = LocalBinder()

    // Check if is already running
    var isRunning = false

    //media player
    lateinit var mediaPlayerHolder: MediaPlayerHolder
    lateinit var musicNotificationManager: MusicNotificationManager
    var isRestoredFromPause = false

    private lateinit var mMediaSessionCompat: MediaSessionCompat

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(null)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(getString(R.string.app_name), null)
    }

    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            return handleMediaIntent(mediaButtonEvent)
        }
    }

    private fun configureMediaSession() {
        mMediaSessionCompat = MediaSessionCompat(this, packageName)
        mMediaSessionCompat.setCallback(mMediaSessionCallback)
    }

    fun getMediaSession(): MediaSessionCompat {
        return mMediaSessionCompat
    }

    override fun onDestroy() {
        super.onDestroy()

        isRunning = false

        if (::mediaPlayerHolder.isInitialized) {
            //saves last played song and its position
            if (mediaPlayerHolder.isCurrentSong) goPreferences.latestPlayedSong =
                Pair(mediaPlayerHolder.currentSong.first, mediaPlayerHolder.playerPosition)

            goPreferences.latestVolume = mediaPlayerHolder.currentVolumeInPercent

            mMediaSessionCompat.release()
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

    override fun onCreate() {
        super.onCreate()
        configureMediaSession()
    }

    inner class LocalBinder : Binder() {
        // Return this instance of PlayerService so we can call public methods
        fun getService() = this@PlayerService
    }

    private fun handleMediaIntent(intent: Intent?): Boolean {

        var isSuccess = false

        intent?.let {
            val event = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_KEY_EVENT) as KeyEvent
            if (event.action == KeyEvent.ACTION_DOWN) { // do something
                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                        mediaPlayerHolder.resumeOrPause()
                        isSuccess = true
                    }
                    KeyEvent.KEYCODE_MEDIA_CLOSE, KeyEvent.KEYCODE_MEDIA_STOP -> {
                        mediaPlayerHolder.stopPlaybackService(true)
                        isSuccess = true
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        mediaPlayerHolder.skip(false)
                        isSuccess = true
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        mediaPlayerHolder.skip(true)
                        isSuccess = true
                    }
                    KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        mediaPlayerHolder.repeatSong()
                        isSuccess = true
                    }
                }
            }
        }
        return isSuccess
    }
}
