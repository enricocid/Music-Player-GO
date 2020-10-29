package com.iven.musicplayergo.player

import android.app.Activity
import android.content.Context.AUDIO_SERVICE
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicRepository
import com.iven.musicplayergo.R
import com.iven.musicplayergo.enums.LaunchedBy
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

class MediaPlayerHolder :
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener {

    // Media player
    lateinit var mediaPlayer: MediaPlayer
    var state = GoConstants.PAUSED
    val isMediaPlayer get() = ::mediaPlayer.isInitialized
    val isPlaying get() = isMediaPlayer && mediaPlayer.isPlaying
    var isPlay = false
    private val mStateBuilder = if (VersioningHelper.isQ()) {
        Builder().setActions(ACTION_SEEK_TO)
    } else {
        null
    }

    // Media player items
    lateinit var currentSong: Pair<Music?, Boolean> // First: current song, second: isFromQueue
    private var mPlayingAlbumSongs: List<Music>? = null
    private val mCurrentAlbumSize get() = mPlayingAlbumSongs?.size!! - 1
    private val mCurrentSongIndex get() = mPlayingAlbumSongs?.indexOf(currentSong.first)!!
    private val mNextSongIndex get() = mCurrentSongIndex + 1
    private val mPrevSongIndex get() = mCurrentSongIndex - 1
    private val mNextSong: Music?
        get() = when {
            mNextSongIndex <= mCurrentAlbumSize -> mPlayingAlbumSongs?.get(mNextSongIndex)
            isQueue -> stopQueueAndGetSkipSong(true)
            else -> mPlayingAlbumSongs?.get(0)
        }
    private val mPrevSong: Music?
        get() = when {
            mPrevSongIndex <= mCurrentAlbumSize && mPrevSongIndex != -1 -> mPlayingAlbumSongs?.get(
                    mPrevSongIndex
            )
            isQueue -> stopQueueAndGetSkipSong(false)
            else -> mPlayingAlbumSongs?.get(mPlayingAlbumSongs?.lastIndex!!)
        }
    private lateinit var preQueueSong: Pair<Music?, List<Music>?>
    var queueSongs = mutableListOf<Music>()

    // Media player booleans
    val isCurrentSong get() = ::currentSong.isInitialized
    var isPlayingFromFolder: LaunchedBy = LaunchedBy.ArtistView
    private var isPlayingFromFolderPreQueue: LaunchedBy = LaunchedBy.ArtistView
    var isSongRestoredFromPrefs = false
    var isSongFromLovedSongs = Pair(false, 0)
    val isLooping get() = mediaPlayer.isLooping
    var isRepeat1X = false
    var isQueue = false
    var isQueueStarted = false

    // Media player INTs/lONGs
    var currentVolumeInPercent = goPreferences.latestVolume
    val playerPosition
        get() = if (!isMediaPlayer) {
            goPreferences.latestPlayedSong?.startFrom!!
        } else {
            mediaPlayer.currentPosition
        }

    // SeekBar
    private var mExecutor: ScheduledExecutorService? = null
    private var mSeekBarPositionUpdateTask: Runnable? = null

    // Interfaces, classes
    lateinit var mediaPlayerInterface: MediaPlayerInterface
    private lateinit var mAudioFocusHandler: AudioFocusHandler

    private lateinit var mMusicNotificationManager: MusicNotificationManager

    private lateinit var mPlayerService: PlayerService

    fun setPlayerService(playerService: PlayerService) {
        mPlayerService = playerService
    }

    fun setCurrentSong(
            song: Music?,
            songs: List<Music>?,
            isFromQueue: Boolean,
            isFolderAlbum: LaunchedBy
    ) {
        isPlayingFromFolder = isFolderAlbum
        currentSong = Pair(song, isFromQueue)
        mPlayingAlbumSongs = songs
    }

    private fun updateMediaSessionMetaData() {
        val mediaMediaPlayerCompat = MediaMetadataCompat.Builder().apply {
            if (VersioningHelper.isQ()) {
                putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        currentSong.first?.duration!!
                )
            }
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.first?.artist)
            putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, currentSong.first?.artist)
            putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, currentSong.first?.artist)
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.first?.title)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentSong.first?.title)
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, currentSong.first?.album)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, currentSong.first?.album)
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong.first?.album)
            BitmapFactory.decodeResource(mPlayerService.resources, R.drawable.ic_music_note)
                    ?.let { bmp ->
                        putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bmp)
                    }
        }
        mPlayerService.getMediaSession().setMetadata(mediaMediaPlayerCompat.build())
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {

        mediaPlayerInterface.onStateChanged()
        mediaPlayerInterface.onPlaybackCompleted()

        if (isRepeat1X || mediaPlayer.isLooping) {
            repeatSong()
        } else if (isQueue) {
            manageQueue(true)
        } else {
            skip(true)
        }
    }

    fun onRestartSeekBarCallback() {
        if (mExecutor == null) {
            startUpdatingCallbackWithPosition()
        }
    }

    fun onPauseSeekBarCallback() {
        stopUpdatingCallbackWithPosition()
    }

    private fun updatePlaybackStatus(updateUI: Boolean) {
        mPlayerService.getMediaSession().setPlaybackState(
                mStateBuilder?.setState(
                        if (state == GoConstants.RESUMED) {
                            GoConstants.PLAYING
                        } else {
                            state
                        },
                        if (VersioningHelper.isQ()) {
                            mediaPlayer.currentPosition.toLong()
                        } else {
                            PLAYBACK_POSITION_UNKNOWN
                        },
                        1F
                )?.build()
        )
        if (updateUI) {
            mediaPlayerInterface.onStateChanged()
        }
    }

    fun resumeMediaPlayer() {
        if (!isPlaying) {
            if (isMediaPlayer) {
                mediaPlayer.start()
            }
            state = if (isSongRestoredFromPrefs) {
                isSongRestoredFromPrefs = false
                GoConstants.PLAYING
            } else {
                GoConstants.RESUMED
            }

            updatePlaybackStatus(true)

            mMusicNotificationManager.startNotificationForeground()

            if (!isPlay) {
                isPlay = true
            }
        }
    }

    fun pauseMediaPlayer() {
        if (isMediaPlayer) {
            mediaPlayer.pause()
            mPlayerService.stopForeground(false)
            state = GoConstants.PAUSED
            updatePlaybackStatus(true)
            mMusicNotificationManager.pauseNotificationForeground()
        }
    }

    fun repeatSong() {
        if (isMediaPlayer) {
            isRepeat1X = false
            mediaPlayer.setOnSeekCompleteListener { mp ->
                mp.setOnSeekCompleteListener(null)
                play()
            }
            mediaPlayer.seekTo(0)
        }
    }

    private fun manageQueue(isNext: Boolean) {

        if (isSongRestoredFromPrefs) {
            isSongRestoredFromPrefs = false
        }

        if (isQueueStarted) {
            val nextSong = getSkipSong(isNext)
            currentSong = Pair(nextSong, true)
        } else {
            setCurrentSong(
                    queueSongs[0],
                    queueSongs,
                    isFromQueue = true,
                    isFolderAlbum = LaunchedBy.ArtistView
            )
            isQueueStarted = true
        }

        initMediaPlayer(currentSong.first)
    }

    fun restorePreQueueSongs() {
        setCurrentSong(preQueueSong.first, preQueueSong.second, false, isPlayingFromFolderPreQueue)
    }

    private fun getSkipSong(isNext: Boolean): Music? {
        if (isNext) {
            if (mNextSong != null) {
                return mNextSong
            } else if (isQueue) {
                return stopQueueAndGetSkipSong(true)
            }
        } else {
            if (mPrevSong != null) {
                return mPrevSong
            } else if (isQueue) {
                return stopQueueAndGetSkipSong(false)
            }
        }
        return null
    }

    private fun stopQueueAndGetSkipSong(restorePreviousAlbum: Boolean): Music? =
            if (restorePreviousAlbum) {
                setQueueEnabled(false)
                restorePreQueueSongs()
                getSkipSong(true)
            } else {
                isQueueStarted = false
                preQueueSong.first
            }

    /**
     * Syncs the mMediaPlayer position with mPlaybackProgressCallback via recurring task.
     */
    private fun startUpdatingCallbackWithPosition() {

        if (mSeekBarPositionUpdateTask == null) {
            mSeekBarPositionUpdateTask =
                    Runnable { updateProgressCallbackTask() }
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
        if (isPlaying) {
            val currentPosition = mediaPlayer.currentPosition
            mediaPlayerInterface.onPositionChanged(currentPosition)
        }
    }

    fun instantReset() {
        if (isMediaPlayer && !isSongRestoredFromPrefs) {
            if (mediaPlayer.currentPosition < 5000) {
                skip(false)
            } else {
                repeatSong()
            }
        } else {
            skip(false)
        }
    }

    /**
     * Once the [MediaPlayer] is released, it can't be used again, and another one has to be
     * created. In the onStop() method of the [MainActivity] the [MediaPlayer] is
     * released. Then in the onStart() of the [MainActivity] a new [MediaPlayer]
     * object has to be created. That's why this method is private, and called by load(int) and
     * not the constructor.
     */
    fun initMediaPlayer(song: Music?) {

        val contentUri = song?.id?.toContentUri()

        contentUri?.let { uri ->
            if (isMediaPlayer) {
                mediaPlayer.reset()
            } else {

                mediaPlayer = MediaPlayer()

                mAudioFocusHandler = AudioFocusHandler(
                        mPlayerService.getSystemService(AUDIO_SERVICE) as AudioManager
                )

                mMusicNotificationManager = mPlayerService.musicNotificationManager

                mediaPlayer.run {
                    EqualizerUtils.openAudioEffectSession(
                            mPlayerService.applicationContext,
                            audioSessionId
                    )

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

                mAudioFocusHandler.tryToGetAudioFocus()

                if (goPreferences.isPreciseVolumeEnabled) {
                    setPreciseVolume(currentVolumeInPercent)
                }
            }

            mediaPlayer.setDataSource(mPlayerService, uri)
            mediaPlayer.prepareAsync()
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            mediaPlayer.release()
            val fallbackSong = if (currentSong.first != null) {
                currentSong.first
            } else {
                MusicRepository.getInstance().randomMusic
            }
            initMediaPlayer(fallbackSong)
        }
        return false
    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {

        if (isRepeat1X) {
            isRepeat1X = false
        }

        if (isSongRestoredFromPrefs) {
            if (goPreferences.isPreciseVolumeEnabled) {
                setPreciseVolume(currentVolumeInPercent)
            }
            mediaPlayer.seekTo(goPreferences.latestPlayedSong?.startFrom!!)
        } else if (isSongFromLovedSongs.first) {
            mediaPlayer.seekTo(isSongFromLovedSongs.second)
            isSongFromLovedSongs = Pair(false, 0)
        }

        if (isQueue) {
            mediaPlayerInterface.onQueueStartedOrEnded(isQueueStarted)
        }

        updateMediaSessionMetaData()

        if (mExecutor == null) {
            startUpdatingCallbackWithPosition()
        }

        play()
    }

    private fun play() {
        if (isPlay) {
            mediaPlayer.start()
            state = GoConstants.PLAYING
            updatePlaybackStatus(true)
            mMusicNotificationManager.startNotificationForeground()
        }
    }

    fun openEqualizer(activity: Activity) {
        EqualizerUtils.openEqualizer(activity, mediaPlayer)
    }

    fun release() {
        if (isMediaPlayer) {
            EqualizerUtils.closeAudioEffectSession(
                    mPlayerService,
                    mediaPlayer.audioSessionId
            )
            mediaPlayer.release()
            mAudioFocusHandler.giveUpAudioFocus()
            stopUpdatingCallbackWithPosition()
        }
        mPlayerService.unregisterActionsReceiver()
    }

    fun resumeOrPause() {
        if (isPlaying) {
            pauseMediaPlayer()
        } else {
            resumeMediaPlayer()
        }
    }

    private fun getRepeatMode() {
        var toastMessage = R.string.repeat_enabled
        when {
            isRepeat1X -> {
                isRepeat1X = false
                mediaPlayer.isLooping = true
                toastMessage = R.string.repeat_loop_enabled
            }
            mediaPlayer.isLooping -> {
                mediaPlayer.isLooping = false
                toastMessage = R.string.repeat_disabled
            }
            else -> isRepeat1X = true
        }
        mPlayerService.getString(toastMessage).toToast(mPlayerService)
    }

    fun repeat(updatePlaybackStatus: Boolean) {
        getRepeatMode()
        if (updatePlaybackStatus) {
            updatePlaybackStatus(true)
        }
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
        if (isQueue) {
            manageQueue(isNext)
        } else {
            val nextSong = getSkipSong(isNext)
            currentSong = Pair(nextSong, false)
            initMediaPlayer(currentSong.first)
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
        if (mPlayerService.isRunning && isMediaPlayer && stopPlayback) {
            mPlayerService.stopForeground(true)
            mPlayerService.stopSelf()
        }
        mediaPlayerInterface.onClose()
    }

    fun onFocusPrefChanged(enabled: Boolean) {
        if (isMediaPlayer) {
            mAudioFocusHandler.handleFocusPrefChange(enabled)
        }
    }

    companion object {

        @Volatile
        private var INSTANCE: MediaPlayerHolder? = null

        fun getInstance(): MediaPlayerHolder {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = MediaPlayerHolder()
                INSTANCE = instance
                return instance
            }
        }
    }
}
