package com.iven.musicplayergo.player

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Parcelable
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.getSystemService
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toSavedMusic
import com.iven.musicplayergo.goPreferences


private const val WAKELOCK_MILLI: Long = 25000

class PlayerService : Service() {

    // Binder given to clients
    private val binder = LocalBinder()

    // Check if is already running
    var isRunning = false

    // WakeLock
    private lateinit var mWakeLock: PowerManager.WakeLock

    // Media player
    lateinit var mediaPlayerHolder: MediaPlayerHolder
    lateinit var musicNotificationManager: MusicNotificationManager
    var isRestoredFromPause = false

    private lateinit var mMediaSessionCompat: MediaSessionCompat

    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            mediaPlayerHolder.resumeOrPause()
        }

        override fun onPause() {
            mediaPlayerHolder.resumeOrPause()
        }

        override fun onSkipToNext() {
            mediaPlayerHolder.skip(true)
        }

        override fun onSkipToPrevious() {
            mediaPlayerHolder.skip(false)
        }

        override fun onStop() {
            mediaPlayerHolder.stopPlaybackService(true)
        }

        override fun onSeekTo(pos: Long) {
            mediaPlayerHolder.seekTo(
                    pos.toInt(),
                    updatePlaybackStatus = true,
                    restoreProgressCallBack = false
            )
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?) = handleMediaIntent(mediaButtonEvent)
    }

    private fun configureMediaSession() {

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        val mediaButtonReceiverComponentName = ComponentName(applicationContext, MediaBtnReceiver::class.java)
        val mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(applicationContext, 0, mediaButtonIntent, 0)

        mMediaSessionCompat = MediaSessionCompat(this, packageName, mediaButtonReceiverComponentName, mediaButtonReceiverPendingIntent).apply {
            isActive = true
            setCallback(mMediaSessionCallback)
            setMediaButtonReceiver(mediaButtonReceiverPendingIntent)
        }
    }

    fun getMediaSession(): MediaSessionCompat = mMediaSessionCompat

    override fun onDestroy() {
        super.onDestroy()

        if (::mediaPlayerHolder.isInitialized && mediaPlayerHolder.isCurrentSong) {
            // Saves last played song and its position
            mediaPlayerHolder.run {
                currentSong.first?.let { musicToSave ->
                    goPreferences.latestPlayedSong =
                        musicToSave.toSavedMusic(playerPosition, launchedBy)
                }
            }

            goPreferences.latestVolume = mediaPlayerHolder.currentVolumeInPercent

            if (::mMediaSessionCompat.isInitialized && mMediaSessionCompat.isActive) {
                with(mMediaSessionCompat) {
                    isActive = false
                    setCallback(null)
                    setMediaButtonReceiver(null)
                    release()
                }
            }

            mediaPlayerHolder.release()
            releaseWakeLock()
            isRunning = false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /*This mode makes sense for things that will be explicitly started
        and stopped to run for arbitrary periods of time, such as a service
        performing background music playback.*/
        isRunning = true

        try {
            val action = intent?.action

            if (action != null) {
                with(mediaPlayerHolder) {
                    when (action) {
                        GoConstants.REWIND_ACTION -> fastSeek(false)
                        GoConstants.PREV_ACTION -> instantReset()
                        GoConstants.PLAY_PAUSE_ACTION -> resumeOrPause()
                        GoConstants.NEXT_ACTION -> skip(true)
                        GoConstants.FAST_FORWARD_ACTION -> fastSeek(true)
                        GoConstants.REPEAT_ACTION -> {
                            repeat(true)
                            mediaPlayerInterface.onUpdateRepeatStatus()
                        }
                        GoConstants.CLOSE_ACTION -> if (isRunning && isMediaPlayer) {
                            stopPlaybackService(true)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        if (!::mediaPlayerHolder.isInitialized) {
            mediaPlayerHolder = MediaPlayerHolder(this).apply {
                registerActionsReceiver()
            }
        }
        if (!::musicNotificationManager.isInitialized) {
            musicNotificationManager = MusicNotificationManager(this)
        }
        return binder
    }

    fun acquireWakeLock() {
        if (::mWakeLock.isInitialized && !mWakeLock.isHeld) {
            mWakeLock.acquire(WAKELOCK_MILLI)
        }
    }

    fun releaseWakeLock() {
        if (::mWakeLock.isInitialized && mWakeLock.isHeld) {
            mWakeLock.release()
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!::mWakeLock.isInitialized) {
            val powerManager = getSystemService<PowerManager>()!!
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
            mWakeLock.setReferenceCounted(false)
        }
        configureMediaSession()
    }

    inner class LocalBinder : Binder() {
        // Return this instance of PlayerService so we can call public methods
        fun getService() = this@PlayerService
    }

    fun handleMediaIntent(intent: Intent?): Boolean {

        var isSuccess = false

        try {
            intent?.let {
                val event =
                    intent.getParcelableExtra<Parcelable>(Intent.EXTRA_KEY_EVENT) as KeyEvent

                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                            mediaPlayerHolder.resumeOrPause()
                            isSuccess = true
                        }
                        KeyEvent.KEYCODE_MEDIA_CLOSE, KeyEvent.KEYCODE_MEDIA_STOP -> {
                            mediaPlayerHolder.stopPlaybackService(stopPlayback = true)
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
        } catch (e: Exception) {
            isSuccess = false
            Toast.makeText(this, getString(R.string.error_media_buttons), Toast.LENGTH_LONG)
                    .show()
            e.printStackTrace()
        }

        return isSuccess
    }

    private inner class MediaBtnReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (handleMediaIntent(intent) && isOrderedBroadcast) {
                abortBroadcast()
            }
        }
    }
}
