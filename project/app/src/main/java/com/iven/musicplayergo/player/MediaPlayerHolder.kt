package com.iven.musicplayergo.player

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toContentUri
import com.iven.musicplayergo.extensions.toToast
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.VersioningHelper
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.ui.MainActivity
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.ln

/**
 * Exposes the functionality of the [MediaPlayer]
 */

// The volume we set the media player to when we lose audio focus, but are
// allowed to reduce the volume instead of stopping playback.
private const val VOLUME_DUCK = 0.2f

// The volume we set the media player when we have audio focus.
private const val VOLUME_NORMAL = 1.0f

// We don't have audio focus, and can't duck (play at a low volume)
private const val AUDIO_NO_FOCUS_NO_DUCK = 0

// We don't have focus, but can duck (play at a low volume)
private const val AUDIO_NO_FOCUS_CAN_DUCK = 1

// We have full audio focus
private const val AUDIO_FOCUSED = 2

// The headset connection states (0,1)
private const val HEADSET_DISCONNECTED = 0
private const val HEADSET_CONNECTED = 1

class MediaPlayerHolder(private val playerService: PlayerService) :
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener {

    private val mStateBuilder =
        Builder().apply {
            if (VersioningHelper.isQ()) setActions(ACTION_SEEK_TO)
        }

    lateinit var mediaPlayerInterface: MediaPlayerInterface

    // Audio focus
    private var mAudioManager = playerService.getSystemService(AUDIO_SERVICE) as AudioManager
    private lateinit var mAudioFocusRequestOreo: AudioFocusRequest
    private val mHandler = Handler()

    private var mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
    private var sPlayOnFocusGain = false

    private val mOnAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> mCurrentAudioFocusState = AUDIO_FOCUSED
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                    mCurrentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // Lost audio focus, but will gain it back (shortly), so note whether
                    // playback should resume
                    mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
                    sPlayOnFocusGain =
                        isMediaPlayer && state == GoConstants.PLAYING || state == GoConstants.RESUMED
                }
                AudioManager.AUDIOFOCUS_LOSS ->
                    // Lost audio focus, probably "permanently"
                    mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
            }
            // Update the player state based on the change
            if (isMediaPlayer) configurePlayerState()
        }

    // Media player
    private lateinit var mediaPlayer: MediaPlayer
    private var mExecutor: ScheduledExecutorService? = null
    private var mSeekBarPositionUpdateTask: Runnable? = null

    // First: current song, second: isFromQueue
    lateinit var currentSong: Pair<Music?, Boolean>
    var isPlayingFromFolder = false
    private var isPlayingFromFolderPreQueue = false
    private var mPlayingAlbumSongs: List<Music>? = null

    var currentVolumeInPercent = goPreferences.latestVolume
    val playerPosition get() = if (!isMediaPlayer) goPreferences.latestPlayedSong?.startFrom!! else mediaPlayer.currentPosition

    // Media player state/booleans
    val isPlaying get() = isMediaPlayer && mediaPlayer.isPlaying
    val isMediaPlayer get() = ::mediaPlayer.isInitialized

    private var sNotificationForeground = false

    val isCurrentSong get() = ::currentSong.isInitialized
    var isRepeat1X = false
    var isLoop = false

    var isQueue = false
    var isQueueStarted = false
    private lateinit var preQueueSong: Pair<Music?, List<Music>?>
    var queueSongs = mutableListOf<Music>()

    var isSongRestoredFromPrefs = false
    var isSongFromLovedSongs = Pair(false, 0)

    var state = GoConstants.PAUSED
    var isPlay = false

    // Notifications
    private lateinit var mNotificationActionsReceiver: NotificationReceiver
    private lateinit var mMusicNotificationManager: MusicNotificationManager

    private fun startForeground() {
        if (!sNotificationForeground) {
            playerService.startForeground(
                GoConstants.NOTIFICATION_ID,
                mMusicNotificationManager.createNotification()
            )
            sNotificationForeground = true
        } else {
            mMusicNotificationManager.apply {
                updateNotificationText()
                updatePlayPauseAction()
                updateRepeatIcon()
                updateNotification()
            }
        }
    }

    fun registerActionsReceiver() {
        mNotificationActionsReceiver = NotificationReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(GoConstants.REPEAT_ACTION)
            addAction(GoConstants.PREV_ACTION)
            addAction(GoConstants.PLAY_PAUSE_ACTION)
            addAction(GoConstants.NEXT_ACTION)
            addAction(GoConstants.CLOSE_ACTION)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        playerService.registerReceiver(mNotificationActionsReceiver, intentFilter)
    }

    private fun unregisterActionsReceiver() {
        try {
            playerService.unregisterReceiver(mNotificationActionsReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    fun setCurrentSong(
        song: Music?,
        songs: List<Music>?,
        isFromQueue: Boolean,
        isFolderAlbum: Boolean
    ) {
        isPlayingFromFolder = isFolderAlbum
        currentSong = Pair(song, isFromQueue)
        mPlayingAlbumSongs = songs
    }

    private fun updateMediaSessionMetaData() {
        val mediaMediaPlayerCompat = MediaMetadataCompat.Builder().apply {
            if (VersioningHelper.isQ()) putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentSong.first?.duration!!)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.first?.artist)
            putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, currentSong.first?.artist)
            putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, currentSong.first?.artist)
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.first?.title)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentSong.first?.title)
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, currentSong.first?.album)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, currentSong.first?.album)
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong.first?.album)
            BitmapFactory.decodeResource(playerService.resources, R.drawable.ic_music_note)?.let { bmp ->
                putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bmp)
            }
        }
        playerService.getMediaSession().setMetadata(mediaMediaPlayerCompat.build())
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {

        mediaPlayerInterface.onStateChanged()
        mediaPlayerInterface.onPlaybackCompleted()

        when {
            isRepeat1X or isLoop -> if (isMediaPlayer) repeatSong()
            isQueue -> manageQueue(true)
            else -> skip(true)
        }
    }

    fun onRestartSeekBarCallback() {
        if (mExecutor == null) startUpdatingCallbackWithPosition()
    }

    fun onPauseSeekBarCallback() {
        stopUpdatingCallbackWithPosition()
    }

    fun tryToGetAudioFocus() {
        mCurrentAudioFocusState = when (getAudioFocusResult()) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> AUDIO_FOCUSED
            else -> AUDIO_NO_FOCUS_NO_DUCK
        }
    }

    @Suppress("DEPRECATION")
    private fun getAudioFocusResult() = when {
        VersioningHelper.isOreoMR1() -> {
            mAudioFocusRequestOreo =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                    setAudioAttributes(AudioAttributes.Builder().run {
                        setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    })
                    setOnAudioFocusChangeListener(mOnAudioFocusChangeListener, mHandler)
                    build()
                }
            mAudioManager.requestAudioFocus(mAudioFocusRequestOreo)
        }
        else -> mAudioManager.requestAudioFocus(
            mOnAudioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }

    @Suppress("DEPRECATION")
    fun giveUpAudioFocus() {
        when {
            VersioningHelper.isOreo() -> if (::mAudioFocusRequestOreo.isInitialized) mAudioManager.abandonAudioFocusRequest(
                mAudioFocusRequestOreo
            )
            else -> {
                mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener)
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
            }
        }
    }

    private fun updatePlaybackStatus(updateUI: Boolean) {
        playerService.getMediaSession().setPlaybackState(
            mStateBuilder.setState(
                if (state == GoConstants.RESUMED) GoConstants.PLAYING else state,
                if (VersioningHelper.isQ()) mediaPlayer.currentPosition.toLong() else PLAYBACK_POSITION_UNKNOWN,
                1F
            ).build()
        )
        if (updateUI) mediaPlayerInterface.onStateChanged()
    }

    fun resumeMediaPlayer() {
        if (!isPlaying) {
            if (isMediaPlayer) mediaPlayer.start()
            state = if (isSongRestoredFromPrefs) {
                isSongRestoredFromPrefs = false
                GoConstants.PLAYING
            } else {
                GoConstants.RESUMED
            }

            updatePlaybackStatus(true)

            startForeground()

            if (!isPlay) isPlay = true
        }
    }

    fun pauseMediaPlayer() {
        mediaPlayer.pause()
        playerService.stopForeground(false)
        sNotificationForeground = false
        state = GoConstants.PAUSED
        updatePlaybackStatus(true)
        mMusicNotificationManager.apply {
            updatePlayPauseAction()
            updateNotification()
        }
    }

    fun repeatSong() {
        isRepeat1X = false
        mediaPlayer.setOnSeekCompleteListener { mp ->
            mp.setOnSeekCompleteListener(null)
            play()
        }
        mediaPlayer.seekTo(0)
    }

    private fun manageQueue(isNext: Boolean) {

        if (isSongRestoredFromPrefs) isSongRestoredFromPrefs = false

        when {
            isQueueStarted -> currentSong = Pair(getSkipSong(isNext), true)
            else -> {
                setCurrentSong(queueSongs[0], queueSongs, isFromQueue = true, isFolderAlbum = false)
                isQueueStarted = true
            }
        }
        initMediaPlayer(currentSong.first)
    }

    fun restorePreQueueSongs() {
        setCurrentSong(preQueueSong.first, preQueueSong.second, false, isPlayingFromFolderPreQueue)
    }

    private fun getSkipSong(isNext: Boolean): Music? {

        val currentIndex = mPlayingAlbumSongs?.indexOf(currentSong.first)

        try {
            return mPlayingAlbumSongs?.get(
                if (isNext) currentIndex?.plus(1)!! else currentIndex?.minus(
                    1
                )!!
            )
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
            return when {
                isQueue -> {

                    when {
                        isNext -> {
                            setQueueEnabled(false)
                            restorePreQueueSongs()
                            getSkipSong(true)
                        }
                        else -> {
                            isQueueStarted = false
                            preQueueSong.first
                        }
                    }
                }
                else -> if (currentIndex != 0) mPlayingAlbumSongs?.get(0) else mPlayingAlbumSongs?.get(
                    mPlayingAlbumSongs?.size?.minus(1)!!
                )
            }
        }
    }

    /**
     * Syncs the mMediaPlayer position with mPlaybackProgressCallback via recurring task.
     */
    private fun startUpdatingCallbackWithPosition() {

        if (mSeekBarPositionUpdateTask == null) mSeekBarPositionUpdateTask =
            Runnable { updateProgressCallbackTask() }

        mExecutor = Executors.newSingleThreadScheduledExecutor()
        mExecutor?.scheduleAtFixedRate(
            mSeekBarPositionUpdateTask!!,
            0,
            1000,
            TimeUnit.MILLISECONDS
        )
    }

    // Reports media playback position to mPlaybackProgressCallback.
    private fun stopUpdatingCallbackWithPosition() {
        mExecutor?.shutdownNow()
        mExecutor = null
        mSeekBarPositionUpdateTask = null
    }

    private fun updateProgressCallbackTask() {
        if (isPlaying) {
            val currentPosition = mediaPlayer.currentPosition
            mediaPlayerInterface.onPositionChanged(currentPosition)
        }
    }

    fun instantReset() {
        if (isMediaPlayer && !isSongRestoredFromPrefs) when {
            mediaPlayer.currentPosition < 5000 -> skip(false)
            else -> repeatSong()
        } else skip(false)
    }

    /**
     * Once the [MediaPlayer] is released, it can't be used again, and another one has to be
     * created. In the onStop() method of the [MainActivity] the [MediaPlayer] is
     * released. Then in the onStart() of the [MainActivity] a new [MediaPlayer]
     * object has to be created. That's why this method is private, and called by load(int) and
     * not the constructor.
     */
    fun initMediaPlayer(song: Music?) {

        try {
            if (isMediaPlayer) {
                mediaPlayer.reset()
            } else {
                mediaPlayer = MediaPlayer().apply {

                    EqualizerUtils.openAudioEffectSession(
                        playerService.applicationContext,
                        audioSessionId
                    )

                    setOnPreparedListener(this@MediaPlayerHolder)
                    setOnCompletionListener(this@MediaPlayerHolder)
                    setWakeMode(playerService, PowerManager.PARTIAL_WAKE_LOCK)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                }

                mMusicNotificationManager = playerService.musicNotificationManager

                if (goPreferences.isFocusEnabled) tryToGetAudioFocus()
                if (goPreferences.isPreciseVolumeEnabled) setPreciseVolume(currentVolumeInPercent)
            }

            song?.id?.toContentUri()?.let { uri ->
                mediaPlayer.setDataSource(playerService, uri)
            }

            mediaPlayer.prepareAsync()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {

        if (isRepeat1X) isRepeat1X = false

        if (isSongRestoredFromPrefs) {
            if (goPreferences.isPreciseVolumeEnabled) setPreciseVolume(currentVolumeInPercent)
            mediaPlayer.seekTo(goPreferences.latestPlayedSong?.startFrom!!)
        } else if (isSongFromLovedSongs.first) {
            mediaPlayer.seekTo(isSongFromLovedSongs.second)
            isSongFromLovedSongs = Pair(false, 0)
        }

        if (isQueue) mediaPlayerInterface.onQueueStartedOrEnded(isQueueStarted)

        updateMediaSessionMetaData()

        if (mExecutor == null) startUpdatingCallbackWithPosition()

        if (isPlay) play()
    }

    private fun play() {
        mediaPlayer.start()
        state = GoConstants.PLAYING
        updatePlaybackStatus(true)
        startForeground()
    }

    fun openEqualizer(activity: Activity) {
        EqualizerUtils.openEqualizer(activity, mediaPlayer)
    }

    fun release() {
        if (isMediaPlayer) {
            EqualizerUtils.closeAudioEffectSession(
                playerService,
                mediaPlayer.audioSessionId
            )
            mediaPlayer.release()
            if (goPreferences.isFocusEnabled) giveUpAudioFocus()
            stopUpdatingCallbackWithPosition()
        }
        unregisterActionsReceiver()
    }

    fun resumeOrPause() {
        if (isPlaying) pauseMediaPlayer() else resumeMediaPlayer()
    }

    private fun getRepeatMode() {
        var toastMessage = R.string.repeat_enabled
        when {
            isRepeat1X -> {
                isRepeat1X = false
                isLoop = true
                toastMessage = R.string.repeat_loop_enabled
            }
            isLoop -> {
                isLoop = false
                toastMessage = R.string.repeat_disabled
            }
            else -> isRepeat1X = true
        }
        playerService.getString(toastMessage)
            .toToast(playerService)
    }


    fun repeat(updatePlaybackStatus: Boolean) {
        getRepeatMode()
        if (updatePlaybackStatus) updatePlaybackStatus(true)
        mMusicNotificationManager.updateRepeatIcon()
    }

    fun setQueueEnabled(enabled: Boolean) {
        isQueue = enabled
        isQueueStarted = false

        when {
            isQueue -> {
                preQueueSong = Pair(currentSong.first, mPlayingAlbumSongs)
                isPlayingFromFolderPreQueue = isPlayingFromFolder
                isQueue = true
                mediaPlayerInterface.onQueueEnabled()
            }
            else -> {
                queueSongs.clear()
                mediaPlayerInterface.onQueueCleared()
                mediaPlayerInterface.onQueueStartedOrEnded(false)
            }
        }
    }

    fun skip(isNext: Boolean) {
        when {
            isQueue -> manageQueue(isNext)
            else -> {
                currentSong = Pair(getSkipSong(isNext), false)
                initMediaPlayer(currentSong.first)
            }
        }
    }

    fun seekTo(position: Int, updatePlaybackStatus: Boolean, restoreProgressCallBack: Boolean) {
        if (isMediaPlayer) {
            mediaPlayer.setOnSeekCompleteListener { mp ->
                mp.setOnSeekCompleteListener(null)
                if (restoreProgressCallBack) startUpdatingCallbackWithPosition()
                if (updatePlaybackStatus) updatePlaybackStatus(!restoreProgressCallBack)
            }
            mediaPlayer.seekTo(position)
        }
    }

    /**
     * Reconfigures the player according to audio focus settings and starts/restarts it. This method
     * starts/restarts the MediaPlayer instance respecting the current audio focus state. So if we
     * have focus, it will play normally; if we don't have focus, it will either leave the player
     * paused or set it to a low volume, depending on what is permitted by the current focus
     * settings.
     */
    private fun configurePlayerState() {

        if (isMediaPlayer) {
            when (mCurrentAudioFocusState) {
                AUDIO_NO_FOCUS_NO_DUCK -> pauseMediaPlayer()
                else -> {
                    when (mCurrentAudioFocusState) {
                        AUDIO_NO_FOCUS_CAN_DUCK -> mediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK)
                        else -> mediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL)
                    }
                    // If we were playing when we lost focus, we need to resume playing.
                    if (sPlayOnFocusGain) {
                        resumeMediaPlayer()
                        sPlayOnFocusGain = false
                    }
                }
            }
        }
    }

    /* Sets the volume of the media player */
    fun setPreciseVolume(percent: Int) {

        currentVolumeInPercent = percent

        if (isMediaPlayer) {
            fun volFromPercent(percent: Int): Float {
                if (percent == 100) return 1f
                return (1 - (ln((101 - percent).toFloat()) / ln(101f)))
            }

            val new = volFromPercent(percent)
            mediaPlayer.setVolume(new, new)
        }
    }

    fun stopPlaybackService(stopPlayback: Boolean) {
        if (playerService.isRunning && isMediaPlayer && stopPlayback) {
            playerService.stopForeground(true)
            playerService.stopSelf()
        }
        mediaPlayerInterface.onClose()
    }

    private inner class NotificationReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            val action = intent.action

            if (action != null) {
                when (action) {
                    GoConstants.PREV_ACTION -> instantReset()
                    GoConstants.PLAY_PAUSE_ACTION -> resumeOrPause()
                    GoConstants.NEXT_ACTION -> skip(true)
                    GoConstants.REPEAT_ACTION -> {
                        repeat(true)
                        mediaPlayerInterface.onUpdateRepeatStatus()
                    }
                    GoConstants.CLOSE_ACTION -> if (playerService.isRunning && isMediaPlayer) stopPlaybackService(
                        stopPlayback = true
                    )

                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> if (::currentSong.isInitialized && goPreferences.isHeadsetPlugEnabled) pauseMediaPlayer()
                    BluetoothDevice.ACTION_ACL_CONNECTED -> if (::currentSong.isInitialized && goPreferences.isHeadsetPlugEnabled) resumeMediaPlayer()

                    AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED ->
                        when (intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)) {
                            AudioManager.SCO_AUDIO_STATE_CONNECTED -> if (isCurrentSong && goPreferences.isHeadsetPlugEnabled) resumeMediaPlayer()
                            AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> if (isCurrentSong && goPreferences.isHeadsetPlugEnabled) pauseMediaPlayer()
                        }

                    Intent.ACTION_HEADSET_PLUG -> if (isCurrentSong && goPreferences.isHeadsetPlugEnabled) {
                        when (intent.getIntExtra("state", -1)) {
                            // 0 means disconnected
                            HEADSET_DISCONNECTED -> if (isCurrentSong && goPreferences.isHeadsetPlugEnabled) pauseMediaPlayer()
                            // 1 means connected
                            HEADSET_CONNECTED -> if (isCurrentSong && goPreferences.isHeadsetPlugEnabled) resumeMediaPlayer()
                        }
                    }
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> if (isPlaying && goPreferences.isHeadsetPlugEnabled) pauseMediaPlayer()
                }
            }
            if (isOrderedBroadcast) abortBroadcast()
        }
    }
}
