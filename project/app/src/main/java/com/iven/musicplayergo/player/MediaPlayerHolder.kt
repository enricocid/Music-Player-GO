package com.iven.musicplayergo.player

import android.annotation.TargetApi
import android.app.Activity
import android.app.ForegroundServiceStartNotAllowedException
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.CountDownTimer
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.util.AndroidRuntimeException
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.models.SavedEqualizerSettings
import com.iven.musicplayergo.ui.MainActivity
import com.iven.musicplayergo.ui.UIControlInterface
import com.iven.musicplayergo.utils.Lists
import com.iven.musicplayergo.utils.Versioning
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

// We don't have audio focus, can't play
private const val AUDIO_FOCUS_FAILED = -1

// We don't have audio focus, and can't duck (play at a low volume)
private const val AUDIO_NO_FOCUS_NO_DUCK = 0

// We don't have focus, but can duck (play at a low volume)
private const val AUDIO_NO_FOCUS_CAN_DUCK = 1

// We have full audio focus
private const val AUDIO_FOCUSED = 2

// We don't have focus, but we can resume
private const val AUDIO_FOCUS_LOSS_TRANSIENT = 3

// The headset connection states (0,1)
private const val HEADSET_DISCONNECTED = 0
private const val HEADSET_CONNECTED = 1


class MediaPlayerHolder:
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener {

    private lateinit var mPlayerService: PlayerService

    private var mMediaMetadataCompat: MediaMetadataCompat? = null
    private val mMediaSessionActions = ACTION_PLAY or ACTION_PAUSE or ACTION_PLAY_PAUSE or ACTION_SKIP_TO_NEXT or ACTION_SKIP_TO_PREVIOUS or ACTION_STOP or ACTION_SEEK_TO
    lateinit var mediaPlayerInterface: MediaPlayerInterface

    // Equalizer
    private var mEqualizer: Equalizer? = null
    private var mBassBoost: BassBoost? = null
    private var mVirtualizer: Virtualizer? = null

    // Audio focus
    private var mAudioManager: AudioManager? = null
    private lateinit var mAudioFocusRequestCompat: AudioFocusRequestCompat

    private val sFocusEnabled get() = GoPreferences.getPrefsInstance().isFocusEnabled
    private var mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
    private var sHasFocus = mCurrentAudioFocusState != AUDIO_NO_FOCUS_NO_DUCK || mCurrentAudioFocusState != AUDIO_FOCUS_FAILED

    private val sHasHeadsetsControl get() = GoPreferences.getPrefsInstance().isHeadsetPlugEnabled

    private var sRestoreVolume = false
    private var sPlayOnFocusGain = false

    private val mOnAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> mCurrentAudioFocusState = AUDIO_FOCUSED
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                    mCurrentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK
                    sPlayOnFocusGain = false
                    sRestoreVolume = isMediaPlayer && state == GoConstants.PLAYING || state == GoConstants.RESUMED
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // Lost audio focus, but will gain it back (shortly), so note whether
                    // playback should resume
                    mCurrentAudioFocusState = AUDIO_FOCUS_LOSS_TRANSIENT
                    sRestoreVolume = false
                    sPlayOnFocusGain =
                        isMediaPlayer && state == GoConstants.PLAYING || state == GoConstants.RESUMED
                }
                // Lost audio focus, probably "permanently"
                AudioManager.AUDIOFOCUS_LOSS -> mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> mCurrentAudioFocusState = AUDIO_FOCUS_FAILED
            }
            // Update the player state based on the change
            if (sHasFocus) {
                if (isPlaying || state == GoConstants.PAUSED && sRestoreVolume || state == GoConstants.PAUSED && sPlayOnFocusGain) {
                    configurePlayerState()
                }
            }
        }

    // Media player
    private lateinit var mediaPlayer: MediaPlayer
    private var mExecutor: ScheduledExecutorService? = null
    private var mSeekBarPositionUpdateTask: Runnable? = null

    var currentSong: Music? = null
    private var mPlayingSongs: List<Music>? = null
    var launchedBy = GoConstants.ARTIST_VIEW

    var currentVolumeInPercent = GoPreferences.getPrefsInstance().latestVolume
    private var currentPlaybackSpeed = GoPreferences.getPrefsInstance().latestPlaybackSpeed

    val playerPosition get() = when {
        isSongFromPrefs && !isCurrentSongFM -> GoPreferences.getPrefsInstance().latestPlayedSong?.startFrom!!
        isCurrentSongFM -> 0
        else -> mediaPlayer.currentPosition
    }

    // Sleep Timer
    private var mSleepTimer: CountDownTimer? = null
    val isSleepTimer get() = mSleepTimer != null

    // Media player state/booleans
    val isMediaPlayer get() = ::mediaPlayer.isInitialized
    val isPlaying get() = isMediaPlayer && state != GoConstants.PAUSED

    private var sNotificationOngoing = false

    val isCurrentSongFM get() = currentSongFM != null
    val isCurrentSong get() = currentSong != null || isCurrentSongFM

    private val sPlaybackSpeedPersisted get() = GoPreferences.getPrefsInstance().playbackSpeedMode != GoConstants.PLAYBACK_SPEED_ONE_ONLY
    var isRepeat1X = false
    var isLooping = false
    private val continueOnEnd get() = GoPreferences.getPrefsInstance().continueOnEnd

    // isQueue saves the current song when queue starts
    var isQueue: Music? = null
    var isQueueStarted = false
    var queueSongs = mutableListOf<Music>()
    var canRestoreQueue = false
    var restoreQueueSong: Music? = null

    var isSongFromPrefs = false
    var currentSongFM: Music? = null

    var state = GoConstants.PAUSED
    var isPlay = false

    // Notifications
    private lateinit var mPlayerBroadcastReceiver: PlayerBroadcastReceiver
    private var mMusicNotificationManager: MusicNotificationManager? = null

    fun setMusicService(playerService: PlayerService) {
        mediaPlayer = MediaPlayer()
        mPlayerService = playerService
        mAudioManager = mPlayerService.getSystemService()
        if (mMusicNotificationManager == null) mMusicNotificationManager = mPlayerService.musicNotificationManager
        registerActionsReceiver()
        mPlayerService.configureMediaSession()
        openOrCloseAudioEffectAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
    }

    fun getMediaMetadataCompat() = mMediaMetadataCompat

    private fun startForeground() {
        if (!sNotificationOngoing) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                sNotificationOngoing = try {
                    mMusicNotificationManager?.createNotification { notification ->
                        mPlayerService.startForeground(GoConstants.NOTIFICATION_ID, notification)
                    }
                    true
                } catch (fsNotAllowed: ForegroundServiceStartNotAllowedException) {
                    synchronized(pauseMediaPlayer()) {
                        mMusicNotificationManager?.createNotificationForError()
                    }
                    fsNotAllowed.printStackTrace()
                    false
                }
            } else {
                mMusicNotificationManager?.createNotification { notification ->
                    mPlayerService.startForeground(GoConstants.NOTIFICATION_ID, notification)
                    sNotificationOngoing = true
                }
            }
        }
    }

    private fun registerActionsReceiver() {
        mPlayerBroadcastReceiver = PlayerBroadcastReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(Intent.ACTION_MEDIA_BUTTON)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        try {
            mPlayerService.applicationContext.registerReceiver(mPlayerBroadcastReceiver, intentFilter)
        } catch (e: AndroidRuntimeException) {
            e.printStackTrace()
            LocalBroadcastManager.getInstance(mPlayerService.applicationContext).registerReceiver(mPlayerBroadcastReceiver, intentFilter)
        }
    }

    private fun unregisterActionsReceiver() {
        try {
            mPlayerService.applicationContext.unregisterReceiver(mPlayerBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            LocalBroadcastManager.getInstance(mPlayerService).unregisterReceiver(mPlayerBroadcastReceiver)
        }
    }

    fun updateCurrentSong(song: Music?, albumSongs: List<Music>?, songLaunchedBy: String) {
        currentSong = song
        mPlayingSongs = albumSongs
        launchedBy = songLaunchedBy
    }

    fun updateCurrentSongs(sortedMusic: List<Music>?) {
        if (sortedMusic.isNullOrEmpty()) {
            currentSong?.findRestoreSorting(launchedBy)?.let { sorting ->
                mPlayingSongs = Lists.getSortedMusicList(sorting, mPlayingSongs?.toMutableList())
            }
            return
        }
        mPlayingSongs = sortedMusic
    }

    fun getCurrentAlbumSize() = mPlayingSongs?.size ?: 0

    private fun openOrCloseAudioEffectAction(event: String) {
        mPlayerService.sendBroadcast(
            Intent(event)
                .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mPlayerService.packageName)
                .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mediaPlayer.audioSessionId)
                .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        )
    }

    fun updateMediaSessionMetaData() {
        with(MediaMetadataCompat.Builder()) {
            (currentSongFM ?: currentSong)?.run {
                putLong(METADATA_KEY_DURATION, duration)
                putString(METADATA_KEY_ARTIST, artist)
                putString(METADATA_KEY_AUTHOR, artist)
                putString(METADATA_KEY_COMPOSER, artist)
                var songTitle = title
                if (GoPreferences.getPrefsInstance().songsVisualization == GoConstants.FN) {
                    songTitle = displayName.toFilenameWithoutExtension()
                }
                putString(METADATA_KEY_TITLE, songTitle)
                putString(METADATA_KEY_DISPLAY_TITLE, songTitle)
                putString(METADATA_KEY_ALBUM_ARTIST, album)
                putString(METADATA_KEY_DISPLAY_SUBTITLE, album)
                putString(METADATA_KEY_ALBUM, album)
                putBitmap(
                    METADATA_KEY_DISPLAY_ICON,
                    ContextCompat.getDrawable(mPlayerService, R.drawable.ic_music_note)?.toBitmap()
                )
                putLong(METADATA_KEY_TRACK_NUMBER, track.toLong())
                albumId?.waitForCover(mPlayerService) { bmp, _ ->
                    putBitmap(METADATA_KEY_ALBUM_ART, bmp)
                    putBitmap(METADATA_KEY_ART, bmp)
                    mMediaMetadataCompat = build()
                    mPlayerService.getMediaSession()?.setMetadata(mMediaMetadataCompat)
                    if (isPlay) {
                        startForeground()
                        mMusicNotificationManager?.run {
                            updatePlayPauseAction()
                            updateNotificationContent {
                                updateNotification()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {

        if (isCurrentSongFM) currentSongFM = null

        when {
            !continueOnEnd -> {
                GoPreferences.getPrefsInstance().hasCompletedPlayback = true
                pauseMediaPlayer()
            }
            isRepeat1X or isLooping -> if (isMediaPlayer) {
                repeatSong(0)
            }
            isQueue != null && !canRestoreQueue -> manageQueue(isNext = true)
            canRestoreQueue -> manageRestoredQueue()
            else -> {
                if (mPlayingSongs?.findIndex(currentSong) == mPlayingSongs?.size?.minus(1)) {
                    if (GoPreferences.getPrefsInstance().onListEnded == GoConstants.CONTINUE) {
                        skip(isNext = true)
                    } else {
                        GoPreferences.getPrefsInstance().hasCompletedPlayback = true
                        pauseMediaPlayer()
                        mediaPlayerInterface.onListEnded()
                    }
                } else {
                    skip(isNext = true)
                }
            }
        }

        mPlayerService.acquireWakeLock()
        if (::mediaPlayerInterface.isInitialized) mediaPlayerInterface.onStateChanged()
    }

    fun onRestartSeekBarCallback() {
        if (mExecutor == null) startUpdatingCallbackWithPosition()
    }

    fun onPauseSeekBarCallback() {
        stopUpdatingCallbackWithPosition()
    }

    fun onHandleNotificationUpdate(isAdditionalActionsChanged: Boolean) {
        mMusicNotificationManager?.onHandleNotificationUpdate(isAdditionalActionsChanged = isAdditionalActionsChanged)
    }

    fun tryToGetAudioFocus() {
        if (!::mAudioFocusRequestCompat.isInitialized) {
            initializeAudioFocusRequestCompat()
        }
        mAudioManager?.let { am ->
            val requestFocus =
                AudioManagerCompat.requestAudioFocus(am, mAudioFocusRequestCompat)
            mCurrentAudioFocusState = when (requestFocus) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> AUDIO_FOCUSED
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> AUDIO_FOCUS_FAILED
                else -> AUDIO_NO_FOCUS_NO_DUCK
            }
        }
    }

    private fun initializeAudioFocusRequestCompat() {
        val audioAttributes = AudioAttributesCompat.Builder()
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            .build()
        mAudioFocusRequestCompat =
            AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(mOnAudioFocusChangeListener)
                .build()
    }

    fun giveUpAudioFocus() {
        if (::mAudioFocusRequestCompat.isInitialized && sHasFocus) {
            mAudioManager?.let { am ->
                AudioManagerCompat.abandonAudioFocusRequest(am, mAudioFocusRequestCompat)
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
            }
        }
    }

    private fun updatePlaybackStatus(updateUI: Boolean) {
        mPlayerService.getMediaSession()?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(mMediaSessionActions)
                .setState(
                    if (isPlaying) GoConstants.PLAYING else GoConstants.PAUSED,
                    mediaPlayer.currentPosition.toLong(),
                    currentPlaybackSpeed
                ).build()
        )
        if (updateUI && ::mediaPlayerInterface.isInitialized) {
            mediaPlayerInterface.onStateChanged()
        }
    }

    private fun startOrChangePlaybackSpeed() {
        with(mediaPlayer) {
            if (!sFocusEnabled || sFocusEnabled && sHasFocus) {
                if (sPlaybackSpeedPersisted && Versioning.isMarshmallow()) {
                    playbackParams = playbackParams.setSpeed(currentPlaybackSpeed)
                } else {
                    MediaPlayerUtils.safePlay(this)
                }
            }
        }
    }

    fun resumeMediaPlayer() {

        if (!isPlaying) {

            if (sFocusEnabled) tryToGetAudioFocus()

            val hasCompletedPlayback = GoPreferences.getPrefsInstance().hasCompletedPlayback
            if (!continueOnEnd && isSongFromPrefs && hasCompletedPlayback || !continueOnEnd && hasCompletedPlayback || GoPreferences.getPrefsInstance().onListEnded != GoConstants.CONTINUE && hasCompletedPlayback) {
                GoPreferences.getPrefsInstance().hasCompletedPlayback = false
                skip(isNext = true)
            } else {
                startOrChangePlaybackSpeed()
            }

            state = if (isSongFromPrefs) {
                isSongFromPrefs = false
                GoConstants.PLAYING
            } else {
                GoConstants.RESUMED
            }

            isPlay = true

            updatePlaybackStatus(updateUI = true)
            startForeground()
        }
    }

    fun pauseMediaPlayer() {
        // Do not pause foreground service, we will need to resume likely
        MediaPlayerUtils.safePause(mediaPlayer)
        sNotificationOngoing = false
        state = GoConstants.PAUSED
        updatePlaybackStatus(updateUI = true)
        mMusicNotificationManager?.run {
            updatePlayPauseAction()
            updateNotification()
        }
        if (::mediaPlayerInterface.isInitialized && !isCurrentSongFM) {
            mediaPlayerInterface.onBackupSong()
        }
    }

    fun repeatSong(startFrom: Int) {
        isRepeat1X = false
        mediaPlayer.setOnSeekCompleteListener { mp ->
            mp.setOnSeekCompleteListener(null)
            play()
        }
        mediaPlayer.seekTo(startFrom)
    }

    private fun manageQueue(isNext: Boolean) {

        if (isSongFromPrefs) isSongFromPrefs = false

        when {
            isQueueStarted -> currentSong = getSkipSong(isNext = isNext)
            else -> {
                isQueue = currentSong?.copy(startFrom = playerPosition)
                isQueueStarted = true
                currentSong = queueSongs.first()
            }
        }
        initMediaPlayer(currentSong, forceReset = false)
    }

    private fun manageRestoredQueue() {
        currentSong = restoreQueueSong
        initMediaPlayer(currentSong, forceReset = false)

        isQueueStarted = true
        restoreQueueSong = null
        canRestoreQueue = false
    }

    private fun getSkipSong(isNext: Boolean): Music? {

        var listToSeek = mPlayingSongs
        if (isQueue != null) listToSeek = queueSongs

        try {
            if (isNext) {
                return listToSeek?.get(listToSeek.findIndex(currentSong).plus(1))
            }
            return listToSeek?.get(listToSeek.findIndex(currentSong).minus(1))
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
            when {
                isQueue != null -> {
                    val returnedSong = isQueue
                    if (isNext) {
                        setQueueEnabled(enabled = false, canSkip = false)
                    } else {
                        isQueueStarted = false
                    }
                    return returnedSong
                }
                else -> {
                    if (listToSeek?.findIndex(currentSong) != 0) {
                        return listToSeek?.first()
                    }
                    return listToSeek.last()
                }
            }
        }
    }

    /**
     * Syncs the mMediaPlayer position with mSeekBarPositionUpdateTask via recurring task.
     */
    private fun startUpdatingCallbackWithPosition() {

        if (mSeekBarPositionUpdateTask == null) {
            mSeekBarPositionUpdateTask = Runnable { updateProgressCallbackTask() }
        }

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
        if (isPlaying && ::mediaPlayerInterface.isInitialized) {
            mediaPlayerInterface.onPositionChanged(mediaPlayer.currentPosition)
        }
    }

    fun instantReset() {
        if (isMediaPlayer && !isSongFromPrefs || isMediaPlayer) {
            when {
                mediaPlayer.currentPosition < 5000 -> skip(isNext = false)
                else -> repeatSong(0)
            }
            return
        }
        skip(isNext = false)
    }

    /**
     * Once the [MediaPlayer] is released, it can't be used again, and another one has to be
     * created. In the onStop() method of the [MainActivity] the [MediaPlayer] is
     * released. Then in the onStart() of the [MainActivity] a new [MediaPlayer]
     * object has to be created. That's why this method is private, and called by load(int) and
     * not the constructor.
     */
    fun initMediaPlayer(song: Music?, forceReset: Boolean) {

        try {

            if (isMediaPlayer && !forceReset) {
                mediaPlayer.reset()
            } else {
                mediaPlayer = MediaPlayer()
            }

            with(mediaPlayer) {
                setOnPreparedListener(this@MediaPlayerHolder)
                setOnCompletionListener(this@MediaPlayerHolder)
                setOnErrorListener(this@MediaPlayerHolder)
                setWakeMode(mPlayerService, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
            }

            song?.id?.toContentUri()?.let { uri ->
                mediaPlayer.setDataSource(mPlayerService, uri)
            }

            mediaPlayer.prepare()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {

        mediaPlayer.release()
        releaseBuiltInEqualizer()

        println("MediaPlayer error: $what")
        if (isCurrentSong) currentSongFM = null

        //restore media player
        initMediaPlayer(currentSong, forceReset = true)

        //restore equalizer
        if (mEqualizer != null) {
            mEqualizer = null
            initOrGetBuiltInEqualizer()
        }
        return true
    }

    override fun onPrepared(mp: MediaPlayer) {

        if (currentSong?.startFrom != 0 && !isCurrentSongFM) {
            mediaPlayer.seekTo(currentSong?.startFrom!!)
        }

        if (!sPlaybackSpeedPersisted) currentPlaybackSpeed = 1.0F

        if (isRepeat1X or isLooping) {
            isRepeat1X = false
            isLooping = false
        }

        if (GoPreferences.getPrefsInstance().isPreciseVolumeEnabled) {
            setPreciseVolume(currentVolumeInPercent)
        }

        if (isQueue != null && ::mediaPlayerInterface.isInitialized) {
            mediaPlayerInterface.onQueueStartedOrEnded(started = isQueueStarted)
        }

        if (mExecutor == null) startUpdatingCallbackWithPosition()

        if (!::mAudioFocusRequestCompat.isInitialized) {
            initializeAudioFocusRequestCompat()
        }

        if (mPlayerService.headsetClicks != 0) mPlayerService.headsetClicks = 0

        if (isPlay) {
            if (sFocusEnabled && !sHasFocus) {
                tryToGetAudioFocus()
            }
            play()
        }

        updateMediaSessionMetaData()

        mPlayerService.releaseWakeLock()

        onBuiltInEqualizerEnabled()
    }

    private fun play() {
        startOrChangePlaybackSpeed()
        state = GoConstants.PLAYING
        updatePlaybackStatus(updateUI = true)
    }

    private fun restoreCustomEqSettings() {

        GoPreferences.getPrefsInstance().savedEqualizerSettings?.let {
                (enabled, preset, bandSettings, bassBoost, virtualizer) ->

            setEqualizerEnabled(isEnabled = enabled)

            mEqualizer?.usePreset(preset.toShort())

            bandSettings?.iterator()?.withIndex()?.let { iterate ->
                while (iterate.hasNext()) {
                    val item = iterate.next()
                    mEqualizer?.setBandLevel(
                        item.index.toShort(),
                        item.value.toInt().toShort()
                    )
                }
            }
            mBassBoost?.setStrength(bassBoost)
            mVirtualizer?.setStrength(virtualizer)
        }
    }

    fun openEqualizer(activity: Activity, resultLauncher: ActivityResultLauncher<Intent>) {
        when (mediaPlayer.audioSessionId) {
            AudioEffect.ERROR_BAD_VALUE -> Toast.makeText(activity, R.string.error_bad_id, Toast.LENGTH_SHORT).show()
            else -> {
                try {
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(
                            AudioEffect.EXTRA_AUDIO_SESSION,
                            mediaPlayer.audioSessionId
                        )
                        putExtra(
                            AudioEffect.EXTRA_CONTENT_TYPE,
                            AudioEffect.CONTENT_TYPE_MUSIC
                        )
                        resultLauncher.launch(this)
                    }
                } catch (e: Exception) {
                    Toast.makeText(activity, R.string.error_sys_eq, Toast.LENGTH_SHORT).show()
                    GoPreferences.getPrefsInstance().isEqForced = true
                    (activity as UIControlInterface).onEnableEqualizer()
                    e.printStackTrace()
                }
            }
        }
    }

    fun onBuiltInEqualizerEnabled() {
        GoPreferences.getPrefsInstance().savedEqualizerSettings?.run {
            if (enabled) initOrGetBuiltInEqualizer()
        }
    }

    fun initOrGetBuiltInEqualizer(): Triple<Equalizer?, BassBoost?, Virtualizer?> {
        if (mEqualizer == null) {
            try {
                val sessionId = mediaPlayer.audioSessionId
                mBassBoost = BassBoost(0, sessionId)
                mVirtualizer = Virtualizer(0, sessionId)
                mEqualizer = Equalizer(0, sessionId)
                restoreCustomEqSettings()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Triple(mEqualizer, mBassBoost, mVirtualizer)
    }

    fun releaseBuiltInEqualizer() {
        if (mEqualizer != null) {
            mEqualizer?.release()
            mBassBoost?.release()
            mVirtualizer?.release()
            mEqualizer = null
            mBassBoost = null
            mVirtualizer = null
        }
    }

    fun setEqualizerEnabled(isEnabled: Boolean) {
        mEqualizer?.enabled = isEnabled
        mBassBoost?.enabled = isEnabled
        mVirtualizer?.enabled = isEnabled
    }

    fun onSaveEqualizerSettings(selectedPreset: Int, bassBoost: Short, virtualizer: Short) {
        mEqualizer?.run {
            GoPreferences.getPrefsInstance().savedEqualizerSettings = SavedEqualizerSettings(
                enabled,
                selectedPreset,
                properties.bandLevels.toList(),
                bassBoost,
                virtualizer
            )
        }
    }

    fun release() {
        if (isMediaPlayer) {
            openOrCloseAudioEffectAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
            releaseBuiltInEqualizer()
            mediaPlayer.release()
            giveUpAudioFocus()
            stopUpdatingCallbackWithPosition()
        }
        if (mMusicNotificationManager != null) {
            mMusicNotificationManager?.cancelNotification()
            mMusicNotificationManager = null
        }
        state = GoConstants.PAUSED
        unregisterActionsReceiver()
        destroyInstance()
    }

    fun cancelSleepTimer() {
        mSleepTimer?.cancel()
        mSleepTimer = null
    }

    fun pauseBySleepTimer(minutes: Long): Boolean {
        return if (isPlaying) {
            mSleepTimer = object : CountDownTimer(TimeUnit.MINUTES.toMillis(minutes), 1000) {
                override fun onTick(p0: Long) {
                    if (::mediaPlayerInterface.isInitialized) {
                        mediaPlayerInterface.onUpdateSleepTimerCountdown(p0)
                    }
                }
                override fun onFinish() {
                    if (::mediaPlayerInterface.isInitialized) {
                        mediaPlayerInterface.onStopSleepTimer()
                    }
                    pauseMediaPlayer()
                    cancelSleepTimer()
                }
            }.start()
            true
        } else {
            mSleepTimer = null
            false
        }
    }

    fun resumeOrPause() {
        try {
            if (isPlaying) {
                pauseMediaPlayer()
            } else {
                if (isSongFromPrefs) updateMediaSessionMetaData()
                resumeMediaPlayer()
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    private fun getRepeatMode() {
        var toastMessage = R.string.repeat_enabled
        when {
            isRepeat1X -> {
                isRepeat1X = false
                isLooping = true
                toastMessage = R.string.repeat_loop_enabled
            }
            isLooping -> {
                isLooping = false
                toastMessage = R.string.repeat_disabled
            }
            else -> isRepeat1X = true
        }
        mediaPlayerInterface.onRepeat(toastMessage)
    }

    fun repeat(updatePlaybackStatus: Boolean) {
        getRepeatMode()
        if (updatePlaybackStatus) updatePlaybackStatus(updateUI = true)
        mMusicNotificationManager?.updateRepeatIcon()
    }

    fun onUpdateFavorites() {
        if (GoPreferences.getPrefsInstance().notificationActions.first == GoConstants.FAVORITE_ACTION) {
            mPlayerService.musicNotificationManager.updateFavoriteIcon()
        }
        mediaPlayerInterface.onUpdateFavorites()
    }

    fun setQueueEnabled(enabled: Boolean, canSkip: Boolean) {

        if (enabled && ::mediaPlayerInterface.isInitialized) {
            mediaPlayerInterface.onQueueEnabled()
            return
        }

        if (isQueue != null) {
            currentSong = isQueue
            isQueue = null
        }

        restoreQueueSong = null
        canRestoreQueue = false
        isQueueStarted = false
        GoPreferences.getPrefsInstance().isQueue = null
        if (::mediaPlayerInterface.isInitialized) {
            mediaPlayerInterface.onQueueStartedOrEnded(started = false)
        }
        if (canSkip) initMediaPlayer(currentSong, forceReset = false)
    }

    fun skip(isNext: Boolean) {
        if (isCurrentSongFM) currentSongFM = null
        when {
            isQueue != null && !canRestoreQueue -> manageQueue(isNext = isNext)
            canRestoreQueue -> manageRestoredQueue()
            else -> {
                currentSong = getSkipSong(isNext = isNext)
                initMediaPlayer(currentSong, forceReset = false)
            }
        }
    }

    fun fastSeek(isForward: Boolean) {
        val step = GoPreferences.getPrefsInstance().fastSeekingStep * 1000
        if (isMediaPlayer) {
            with(mediaPlayer) {
                val newPosition = if (isForward) {
                    currentPosition.plus(step)
                } else {
                    currentPosition.minus(step)
                }
                seekTo(newPosition, updatePlaybackStatus = true, restoreProgressCallBack = false)
            }
        }
    }

    fun seekTo(position: Int, updatePlaybackStatus: Boolean, restoreProgressCallBack: Boolean) {
        if (isMediaPlayer) {
            mediaPlayer.setOnSeekCompleteListener { mp ->
                mp.setOnSeekCompleteListener(null)
                if (restoreProgressCallBack) startUpdatingCallbackWithPosition()
                if (updatePlaybackStatus) updatePlaybackStatus(updateUI = !restoreProgressCallBack)
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
        when (mCurrentAudioFocusState) {
            AUDIO_NO_FOCUS_NO_DUCK -> {
                stopPlaybackService(stopPlayback = true, fromUser = false, fromFocus = true)
                pauseMediaPlayer()
            }
            AUDIO_FOCUS_LOSS_TRANSIENT -> {
                stopPlaybackService(stopPlayback = true, fromUser = false, fromFocus = true)
                pauseMediaPlayer()
            }
            AUDIO_NO_FOCUS_CAN_DUCK -> mediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK)
            else -> {
                if (sRestoreVolume) {
                    sRestoreVolume = false
                    mediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL)
                }
                // If we were playing when we lost focus, we need to resume playing.
                if (sPlayOnFocusGain) {
                    resumeMediaPlayer()
                    sPlayOnFocusGain = false
                }
            }
        }
    }

    /* Sets the playback speed of the media player */
    @TargetApi(Build.VERSION_CODES.M)
    fun setPlaybackSpeed(speed: Float) {
        if (isMediaPlayer) {
            currentPlaybackSpeed = speed
            if (sPlaybackSpeedPersisted) {
                GoPreferences.getPrefsInstance().latestPlaybackSpeed = currentPlaybackSpeed
            }
            if (state != GoConstants.PAUSED) {
                MediaPlayerUtils.safeSetPlaybackSpeed(mediaPlayer, currentPlaybackSpeed)
                updatePlaybackStatus(updateUI = false)
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

    fun stopPlaybackService(stopPlayback: Boolean, fromUser: Boolean, fromFocus: Boolean) {
        try {
            if (mPlayerService.isRunning && isMediaPlayer && stopPlayback) {
                if (sNotificationOngoing) {
                    ServiceCompat.stopForeground(mPlayerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    sNotificationOngoing = false
                } else {
                    mMusicNotificationManager?.cancelNotification()
                }
                if (!fromFocus) mPlayerService.stopSelf()
            }
            if (::mediaPlayerInterface.isInitialized && fromUser) {
                mediaPlayerInterface.onClose()
            }
        } catch (e: java.lang.IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    private inner class PlayerBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            try {
                intent.action?.let { act ->

                    val isHeadsetsControl = isCurrentSong && sHasHeadsetsControl
                    when (act) {

                        BluetoothDevice.ACTION_ACL_DISCONNECTED -> if (isHeadsetsControl) {
                            pauseMediaPlayer()
                        }
                        BluetoothDevice.ACTION_ACL_CONNECTED -> if (isHeadsetsControl) {
                            resumeMediaPlayer()
                        }

                        Intent.ACTION_MEDIA_BUTTON -> if (handleMediaButton(intent)) {
                            resultCode = Activity.RESULT_OK
                        }

                        AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> if (isHeadsetsControl) {
                            when (intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)) {
                                AudioManager.SCO_AUDIO_STATE_CONNECTED -> resumeMediaPlayer()
                                AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> pauseMediaPlayer()
                            }
                        }

                        Intent.ACTION_HEADSET_PLUG -> if (isHeadsetsControl) {
                            when (intent.getIntExtra("state", -1)) {
                                // 0 means disconnected
                                HEADSET_DISCONNECTED -> pauseMediaPlayer()
                                // 1 means connected
                                HEADSET_CONNECTED -> resumeMediaPlayer()
                            }
                        }

                        AudioManager.ACTION_AUDIO_BECOMING_NOISY -> if (isPlaying && sHasHeadsetsControl) {
                            pauseMediaPlayer()
                        }
                    }
                }

                if (isOrderedBroadcast) abortBroadcast()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Handles "media button" which is how external applications
     * communicate with our app
     */
    @Suppress("DEPRECATION")
    fun handleMediaButton(intent: Intent): Boolean {
        val event: KeyEvent = if (Versioning.isTiramisu()) {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        } ?: return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_PAUSE -> if (isPlaying) {
                    pauseMediaPlayer()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> if (!isPlaying) {
                    resumeMediaPlayer()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    resumeOrPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    if (isPlaying) {
                        skip(true)
                        return true
                    }
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> if (isPlaying) {
                    // Because this comes from an AI, we may want to
                    // not have the immediate reset because the user
                    // specifically requested the previous song
                    // for example by saying "previous song"
                    skip(false)
                    return true
                }
            }
        }
        return false
    }

    companion object {
        @Volatile private var INSTANCE: MediaPlayerHolder? = null

        /** Get/Instantiate the single instance of [MediaPlayerHolder]. */
        fun getInstance(): MediaPlayerHolder {
            val currentInstance = INSTANCE

            if (currentInstance != null) return currentInstance

            synchronized(this) {
                val newInstance = MediaPlayerHolder()
                INSTANCE = newInstance
                return newInstance
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
