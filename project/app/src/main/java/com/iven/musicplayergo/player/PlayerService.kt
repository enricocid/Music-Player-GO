package com.iven.musicplayergo.player

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import androidx.core.content.getSystemService
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.extensions.toSavedMusic
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
    private val mMediaPlayerHolder get() = MediaPlayerHolder.getInstance()
    lateinit var musicNotificationManager: MusicNotificationManager
    var isRestoredFromPause = false

    var headsetClicks = 0
    private var mLastTimeClick = 0L

    private var mMediaSessionCompat: MediaSessionCompat? = null

    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            mMediaPlayerHolder.resumeOrPause()
        }

        override fun onPause() {
            mMediaPlayerHolder.resumeOrPause()
        }

        override fun onSkipToNext() {
            mMediaPlayerHolder.skip(isNext = true)
        }

        override fun onSkipToPrevious() {
            mMediaPlayerHolder.skip(isNext = false)
        }

        override fun onStop() {
            mMediaPlayerHolder.stopPlaybackService(stopPlayback = true, fromUser = true)
        }

        override fun onSeekTo(pos: Long) {
            mMediaPlayerHolder.seekTo(
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

        var flags = 0
        if (Versioning.isMarshmallow()) {
            flags = PendingIntent.FLAG_IMMUTABLE or 0
        }
        val mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(applicationContext,
            0, mediaButtonIntent, flags)

        mMediaSessionCompat = MediaSessionCompat(this, packageName, mediaButtonReceiverComponentName, mediaButtonReceiverPendingIntent).apply {
            isActive = true
            setCallback(mMediaSessionCallback)
            setMediaButtonReceiver(mediaButtonReceiverPendingIntent)
        }
    }

    fun getMediaSession(): MediaSessionCompat? = mMediaSessionCompat

    override fun onDestroy() {
        super.onDestroy()

        if (mMediaPlayerHolder.isCurrentSong && mMediaPlayerHolder.currentSongFM == null) {

            // Saves last played song and its position if user is ok :)
            val prefs = GoPreferences.getPrefsInstance()
            with(mMediaPlayerHolder) {
                if (queueSongs.isNotEmpty()) { prefs.queue = queueSongs }
                prefs.latestPlayedSong = currentSong?.toSavedMusic(playerPosition, launchedBy)
                if (isQueue != null && isQueueStarted) {
                    prefs.isQueue = isQueue
                } else {
                    if (prefs.isQueue != null) { prefs.isQueue = null }
                }
            }
            prefs.latestVolume = mMediaPlayerHolder.currentVolumeInPercent
        }

        mMediaSessionCompat?.run {
            if (isActive) {
                isActive = false
                setCallback(null)
                setMediaButtonReceiver(null)
                release()
            }
        }

        mMediaSessionCompat = null
        mMediaPlayerHolder.release()
        releaseWakeLock()
        isRunning = false
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

                with(mMediaPlayerHolder) {
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
                            mMediaPlayerHolder.mediaPlayerInterface.onUpdateFavorites()
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
                            stopPlaybackService(stopPlayback = true, fromUser = true)
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
        synchronized(initializeNotificationManager()) {
            mMediaPlayerHolder.setMusicService(this@PlayerService)
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

    @Suppress("DEPRECATION")
    fun handleMediaIntent(intent: Intent?): Boolean {

        try {
            intent?.let {

                val event = if (Versioning.isTiramisu()) {
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                } ?: return false

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
                                mMediaPlayerHolder.skip(isNext = true)
                            } else {
                                mMediaPlayerHolder.resumeOrPause()
                            }
                            mLastTimeClick = eventTime
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_CLOSE, KeyEvent.KEYCODE_MEDIA_STOP -> {
                            mMediaPlayerHolder.stopPlaybackService(stopPlayback = true, fromUser = true)
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            mMediaPlayerHolder.skip(isNext = false)
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            mMediaPlayerHolder.skip(isNext = true)
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            mMediaPlayerHolder.repeatSong(0)
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
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
