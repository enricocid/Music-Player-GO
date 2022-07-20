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
import androidx.core.content.getSystemService
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toSavedMusic
import com.iven.musicplayergo.extensions.toToast
import com.iven.musicplayergo.utils.Lists
import com.iven.musicplayergo.utils.Versioning


private const val WAKELOCK_MILLI: Long = 25000

private const val DOUBLE_CLICK = 400

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

    var headsetClicks = 0
    private var mLastTimeClick = 0L

    private lateinit var mMediaSessionCompat: MediaSessionCompat

    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            mediaPlayerHolder.resumeOrPause()
        }

        override fun onPause() {
            mediaPlayerHolder.resumeOrPause()
        }

        override fun onSkipToNext() {
            mediaPlayerHolder.skip(isNext = true)
        }

        override fun onSkipToPrevious() {
            mediaPlayerHolder.skip(isNext = false)
        }

        override fun onStop() {
            mediaPlayerHolder.stopPlaybackService(stopPlayback = true)
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

    fun configureMediaSession() {

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        val mediaButtonReceiverComponentName = ComponentName(applicationContext, MediaBtnReceiver::class.java)

        val mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(applicationContext, 0, mediaButtonIntent, if (Versioning.isMarshmallow()) {
            PendingIntent.FLAG_IMMUTABLE or 0
        } else {
            0
        })

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

            // Saves last played song and its position if user is ok :)
            val preferences = GoPreferences.getPrefsInstance()
            with(mediaPlayerHolder) {
                preferences.latestPlayedSong = if (isQueue != null && isQueueStarted) {
                    preferences.isQueue = isQueue
                    currentSong
                } else {
                    if (preferences.isQueue != null) { preferences.isQueue = null }
                    currentSong?.toSavedMusic(playerPosition, launchedBy)
                }
                if (queueSongs.isNotEmpty()) { preferences.queue = queueSongs }
            }
            preferences.latestVolume = mediaPlayerHolder.currentVolumeInPercent
        }

        if (::mMediaSessionCompat.isInitialized && mMediaSessionCompat.isActive) {
            with(mMediaSessionCompat) {
                isActive = false
                setCallback(null)
                setMediaButtonReceiver(null)
                release()
            }
            mediaPlayerHolder.release()
            releaseWakeLock()
            isRunning = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!::mWakeLock.isInitialized) {
            getSystemService<PowerManager>()?.let { pm ->
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
                mWakeLock.setReferenceCounted(false)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /*This mode makes sense for things that will be explicitly started
        and stopped to run for arbitrary periods of time, such as a service
        performing background music playback.*/
        isRunning = true

        try {
            intent?.action?.let { act ->

                with(mediaPlayerHolder) {
                    when (act) {
                        GoConstants.FAVORITE_ACTION -> {
                            Lists.addToFavorites(
                                this@PlayerService,
                                currentSong,
                                canRemove = true,
                                0,
                                launchedBy
                            )
                            musicNotificationManager.updateFavoriteIcon()
                            mediaPlayerHolder.mediaPlayerInterface.onUpdateFavorites()
                        }
                        GoConstants.FAVORITE_POSITION_ACTION -> Lists.addToFavorites(
                            this@PlayerService,
                            currentSong,
                            canRemove = false,
                            playerPosition,
                            launchedBy
                        )
                        GoConstants.REWIND_ACTION -> fastSeek(isForward = false)
                        GoConstants.PREV_ACTION -> instantReset()
                        GoConstants.PLAY_PAUSE_ACTION -> resumeOrPause()
                        GoConstants.NEXT_ACTION -> skip(isNext = true)
                        GoConstants.FAST_FORWARD_ACTION -> fastSeek(isForward = true)
                        GoConstants.REPEAT_ACTION -> {
                            repeat(updatePlaybackStatus = true)
                            mediaPlayerInterface.onUpdateRepeatStatus()
                        }
                        GoConstants.CLOSE_ACTION -> if (isRunning && isMediaPlayer) {
                            stopPlaybackService(stopPlayback = true)
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
            mediaPlayerHolder = MediaPlayerHolder().apply {
                synchronized(initializeNotificationManager()) {
                    setMusicService(this@PlayerService)
                }
            }
        }
        return binder
    }

    private fun initializeNotificationManager() {
        if (!::musicNotificationManager.isInitialized) {
            musicNotificationManager = MusicNotificationManager(this)
        }
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

    inner class LocalBinder : Binder() {
        // Return this instance of PlayerService so we can call public methods
        fun getService() : PlayerService = this@PlayerService
    }

    fun handleMediaIntent(intent: Intent?): Boolean {

        try {
            intent?.let {
                val event =
                    intent.getParcelableExtra<Parcelable>(Intent.EXTRA_KEY_EVENT) as KeyEvent

                val eventTime =
                    if (event.eventTime != 0L) event.eventTime else System.currentTimeMillis()

                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                            // respond to double click
                            if (eventTime - mLastTimeClick <= DOUBLE_CLICK) {
                                headsetClicks = 2
                            }
                            if (headsetClicks == 2) {
                                mediaPlayerHolder.skip(isNext = true)
                            } else {
                                mediaPlayerHolder.resumeOrPause()
                            }
                            mLastTimeClick = eventTime
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_CLOSE, KeyEvent.KEYCODE_MEDIA_STOP -> {
                            mediaPlayerHolder.stopPlaybackService(stopPlayback = true)
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            mediaPlayerHolder.skip(isNext = false)
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            mediaPlayerHolder.skip(isNext = true)
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            mediaPlayerHolder.repeatSong(0)
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            R.string.error_media_buttons.toToast(this)
            e.printStackTrace()
            return false
        }
        return false
    }

    private inner class MediaBtnReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (handleMediaIntent(intent) && isOrderedBroadcast) {
                abortBroadcast()
            }
        }
    }
}
