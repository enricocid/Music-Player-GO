package com.iven.musicplayergo.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.callbacks.onShow
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.getListAdapter
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicRepository
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.QueueAdapter
import com.iven.musicplayergo.databinding.FragmentPlayerBinding
import com.iven.musicplayergo.databinding.NowPlayingBinding
import com.iven.musicplayergo.databinding.NowPlayingControlsBinding
import com.iven.musicplayergo.databinding.NowPlayingExtendedControlsBinding
import com.iven.musicplayergo.enums.LaunchedBy
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.MusicOrgHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.interfaces.MediaPlayerInterface
import com.iven.musicplayergo.interfaces.UIControlInterface
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.navigation.DetailsFragment
import de.halfbit.edgetoedge.Edge
import de.halfbit.edgetoedge.edgeToEdge


class PlayerFragment : Fragment(R.layout.fragment_player),
    MediaPlayerInterface {

    private var mUIControlInterface: UIControlInterface? = null

    // View binding classes
    private lateinit var mFragmentPlayerBinding: FragmentPlayerBinding

    private val mMusicRepository: MusicRepository get() = MusicRepository.getInstance()

    // Colors
    private val mResolvedAccentColor get() = ThemeHelper.resolveThemeAccent(requireContext())

    private val mResolvedIconsColor get() = R.color.widgetsColor.decodeColor(requireContext())
    private val mResolvedDisabledIconsColor
        get() = ThemeHelper.resolveColorAttr(
            requireContext(),
            android.R.attr.colorButtonNormal
        )

    // Booleans
    private val sLandscape get() = ThemeHelper.isDeviceLand(resources)

    // Now playing
    private lateinit var mNowPlayingDialog: MaterialDialog
    private lateinit var mNowPlayingBinding: NowPlayingBinding
    private lateinit var mNowPlayingControlsBinding: NowPlayingControlsBinding
    private lateinit var mNowPlayingExtendedControlsBinding: NowPlayingExtendedControlsBinding

    // Music player things
    private lateinit var mQueueDialog: MaterialDialog
    private lateinit var mQueueAdapter: QueueAdapter

    // The player
    private val mMediaPlayerHolder: MediaPlayerHolder get() = MediaPlayerHolder.getInstance()

    private val isNowPlaying get() = ::mNowPlayingDialog.isInitialized && mNowPlayingDialog.isShowing

    // Our PlayerService shit
    private lateinit var mPlayerService: PlayerService
    private var sBound = false
    private lateinit var mBindingIntent: Intent

    private lateinit var mNowPlayingView: View

    // Defines callbacks for service binding, passed to bindService()
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            val binder = service as PlayerService.LocalBinder
            mPlayerService = binder.getService()
            sBound = true
            mMediaPlayerHolder.setPlayerService(mPlayerService)

            handleRestore()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            sBound = false
        }
    }

    fun unbindService() {
        if (sBound) {
            requireContext().unbindService(connection)
        }
        if (!mMediaPlayerHolder.isPlaying && ::mPlayerService.isInitialized && mPlayerService.isRunning) {
            mPlayerService.stopForeground(true)
            requireContext().stopService(mBindingIntent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mFragmentPlayerBinding = FragmentPlayerBinding.bind(view)

        initMediaButtons()

        mMediaPlayerHolder.mediaPlayerInterface = this
        doBindService()
    }

    private fun skip(isNext: Boolean) {
        if (!mMediaPlayerHolder.isPlay) {
            mMediaPlayerHolder.isPlay = true
        }
        if (isNext) {
            mMediaPlayerHolder.skip(true)
        } else {
            mMediaPlayerHolder.instantReset()
        }
        if (mMediaPlayerHolder.isSongRestoredFromPrefs) {
            mMediaPlayerHolder.isSongRestoredFromPrefs =
                false
        }
    }

    private fun setRepeat() {
        mMediaPlayerHolder.repeat(mMediaPlayerHolder.isPlaying)
        updateRepeatStatus(false)
    }

    private fun doBindService() {
        mBindingIntent = Intent(requireContext(), PlayerService::class.java).also {
            requireContext().bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mPlayerService.isInitialized) {
            handleRestore()
        }
    }

    //method to handle intent to play audio file from external app
    private fun handleIntent(intent: Intent) {

        intent.data?.let { returnUri ->
            requireContext().contentResolver.query(returnUri, null, null, null, null)
        }?.use { cursor ->
            try {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                val song = mMusicRepository.getSongFromIntent(cursor.getString(displayNameIndex))
                //get album songs and sort them
                val albumSongs = MusicOrgHelper.getAlbumSongs(
                    song?.artist,
                    song?.album
                )

                onSongSelected(song, albumSongs, LaunchedBy.ArtistView)

            } catch (e: Exception) {
                e.printStackTrace()
                mUIControlInterface?.onError(GoConstants.TAG_NO_MUSIC_INTENT)
            }
        }
    }

    // Handle restoring: handle intent data, if any, or restore playback
    private fun handleRestore() {
        val intent = requireActivity().intent
        if (intent != null && Intent.ACTION_VIEW == intent.action && intent.data != null) {
            handleIntent(
                intent
            )
        } else {
            restorePlayerStatus()
        }
    }

    private fun initMediaButtons() {

        mFragmentPlayerBinding.apply {
            mFragmentPlayerBinding.playPauseButton.setOnClickListener { mMediaPlayerHolder.resumeOrPause() }

            queueButton.apply {
                setOnClickListener { openQueueDialog() }
                setOnLongClickListener {
                    if (mMediaPlayerHolder.isQueue) {
                        DialogHelper.showClearQueueDialog(
                            requireContext()
                        )
                    }
                    return@setOnLongClickListener true
                }
            }

            lovedSongsButton.apply {
                setOnClickListener { mUIControlInterface?.onOpenLovedSongsDialog() }
                setOnLongClickListener {
                    if (!goPreferences.lovedSongs.isNullOrEmpty()) {
                        DialogHelper.showClearLovedSongDialog(
                            requireContext()
                        )
                    }
                    return@setOnLongClickListener true
                }
            }

            onLovedSongUpdate(false)

            playingSongsContainer.apply {
                setOnClickListener { openNowPlaying() }
                setOnLongClickListener {
                    openPlayingArtistAlbum()
                    return@setOnLongClickListener true
                }
            }
        }


        mNowPlayingView = View.inflate(requireContext(), R.layout.now_playing, null)
        mNowPlayingBinding = NowPlayingBinding.bind(mNowPlayingView)


        mNowPlayingControlsBinding = NowPlayingControlsBinding.bind(mNowPlayingBinding.root)
        mNowPlayingExtendedControlsBinding =
            NowPlayingExtendedControlsBinding.bind(mNowPlayingBinding.root)

        mNowPlayingBinding.npSong.isSelected = true
        mNowPlayingBinding.npArtistAlbum.isSelected = true

        mNowPlayingBinding.npArtistAlbum.setOnClickListener { openPlayingArtistAlbum() }

        mNowPlayingControlsBinding.npSkipPrev.setOnClickListener { skip(false) }

        mNowPlayingControlsBinding.npEq.setOnClickListener {
            mMediaPlayerHolder.openEqualizer(
                requireActivity()
            )
        }
        mNowPlayingControlsBinding.npPlay.setOnClickListener { mMediaPlayerHolder.resumeOrPause() }

        mNowPlayingControlsBinding.npSkipNext.setOnClickListener { skip(true) }
        mNowPlayingControlsBinding.npRepeat.setOnClickListener { setRepeat() }
        mNowPlayingExtendedControlsBinding.npLove.setOnClickListener {
            ListsHelper.addToLovedSongs(
                requireContext(),
                mMediaPlayerHolder.currentSong.first,
                mMediaPlayerHolder.playerPosition,
                mMediaPlayerHolder.launchedBy
            )
            onLovedSongUpdate(false)
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as? UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    private fun setSeekBarProgressListener() {

        mNowPlayingBinding.npSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                val defaultPositionColor = mNowPlayingBinding.npSeek.currentTextColor
                var userSelectedPosition = 0
                var isUserSeeking = false

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (isUserSeeking) {
                        userSelectedPosition = progress
                        mNowPlayingBinding.npSeek.setTextColor(
                            mResolvedAccentColor
                        )
                    }
                    mNowPlayingBinding.npSeek.text =
                        progress.toLong().toFormattedDuration(isAlbum = false, isSeekBar = true)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (isUserSeeking) {
                        mNowPlayingBinding.npSeek.setTextColor(defaultPositionColor)
                        mMediaPlayerHolder.onPauseSeekBarCallback()
                        isUserSeeking = false
                    }
                    if (mMediaPlayerHolder.state != GoConstants.PLAYING) {
                        mFragmentPlayerBinding.songProgress.progress = userSelectedPosition
                        mNowPlayingBinding.npSeekBar.progress = userSelectedPosition
                    }
                    mMediaPlayerHolder.seekTo(
                        userSelectedPosition,
                        updatePlaybackStatus = mMediaPlayerHolder.isPlaying,
                        restoreProgressCallBack = !isUserSeeking
                    )
                }
            })
    }

    private fun setupPreciseVolumeHandler() {

        mMediaPlayerHolder.currentVolumeInPercent.apply {
            mNowPlayingExtendedControlsBinding.npVolume.setImageResource(
                ThemeHelper.getPreciseVolumeIcon(
                    this
                )
            )
            mNowPlayingExtendedControlsBinding.npVolumeSeek.progress = this
        }

        mNowPlayingExtendedControlsBinding.npVolumeSeek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {

            var isUserSeeking = false

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
                if (isUserSeeking) {

                    mMediaPlayerHolder.setPreciseVolume(progress)

                    mNowPlayingExtendedControlsBinding.npVolume.setImageResource(
                        ThemeHelper.getPreciseVolumeIcon(
                            progress
                        )
                    )

                    ThemeHelper.updateIconTint(
                        mNowPlayingExtendedControlsBinding.npVolume,
                        mResolvedAccentColor
                    )
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = false
                ThemeHelper.updateIconTint(
                    mNowPlayingExtendedControlsBinding.npVolume,
                    mResolvedIconsColor
                )
            }
        })
    }

    private fun openNowPlaying() {

        if (mMediaPlayerHolder.isCurrentSong) {

            mNowPlayingDialog =
                MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {

                    customView(null, mNowPlayingView)

                    mNowPlayingControlsBinding.npRepeat.setImageResource(
                        ThemeHelper.getRepeatIcon(
                            mMediaPlayerHolder
                        )
                    )

                    ThemeHelper.updateIconTint(
                        mNowPlayingControlsBinding.npRepeat,
                        if (mMediaPlayerHolder.isRepeat1X || mMediaPlayerHolder.isLooping) {
                            mResolvedAccentColor
                        } else {
                            mResolvedIconsColor
                        }
                    )

                    if (goPreferences.isPreciseVolumeEnabled) {
                        setupPreciseVolumeHandler()
                    } else {
                        mNowPlayingExtendedControlsBinding.npVolumeSeek.isEnabled = false
                        ThemeHelper.updateIconTint(
                            mNowPlayingExtendedControlsBinding.npVolume,
                            mResolvedDisabledIconsColor
                        )
                    }

                    setSeekBarProgressListener()

                    updateNowPlayingInfo()

                    onShow {
                        mNowPlayingBinding.npSeekBar.progress =
                            mFragmentPlayerBinding.songProgress.progress
                    }

                    onDismiss {
                        mNowPlayingBinding.npSeekBar.setOnSeekBarChangeListener(null)
                    }

                    if (goPreferences.isEdgeToEdge && !sLandscape) {
                        window?.apply {
                            ThemeHelper.handleLightSystemBars(
                                resources.configuration,
                                decorView
                            )
                            edgeToEdge {
                                mNowPlayingBinding.root.fit { Edge.Bottom }
                            }
                        }
                    }
                }
        }
    }

    private fun updatePlayingStatus(isNowPlaying: Boolean) {
        val isPlaying = mMediaPlayerHolder.state != GoConstants.PAUSED
        val drawable =
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        if (isNowPlaying) mNowPlayingControlsBinding.npPlay.setImageResource(drawable) else
            mFragmentPlayerBinding.playPauseButton.setImageResource(
                drawable
            )
    }

    override fun onLovedSongUpdate(clear: Boolean) {
        val lovedSongs = goPreferences.lovedSongs

        if (clear) {
            lovedSongs?.clear()
            goPreferences.lovedSongs = lovedSongs
        }

        val lovedSongsButtonColor = if (lovedSongs.isNullOrEmpty()) {
            mResolvedDisabledIconsColor
        } else {
            R.color.red.decodeColor(requireContext())
        }

        ThemeHelper.updateIconTint(
            mFragmentPlayerBinding.lovedSongsButton,
            lovedSongsButtonColor
        )
    }

    private fun restorePlayerStatus() {
        // If we are playing and the activity was restarted
        // update the controls panel
        mMediaPlayerHolder.apply {

            if (mMediaPlayerHolder.isPlaying) {

                onRestartSeekBarCallback()
                updatePlayingInfo(true)

            } else {

                isSongRestoredFromPrefs = goPreferences.latestPlayedSong != null

                val song =
                    if (isSongRestoredFromPrefs) {
                        MusicOrgHelper.getSongForRestore(
                            goPreferences.latestPlayedSong
                        )
                    } else {
                        mMusicRepository.randomMusic
                    }

                val songs = MusicOrgHelper.getAlbumSongs(
                    song.artist,
                    song.album
                )

                if (!songs.isNullOrEmpty()) {

                    isPlay = false

                    onStartPlayback(
                        song,
                        songs,
                        getLatestSongLaunchedBy()
                    )

                    updatePlayingInfo(false)

                    mFragmentPlayerBinding.songProgress.progress =
                        if (isSongRestoredFromPrefs) goPreferences.latestPlayedSong?.startFrom!! else 0
                } else {
                    mUIControlInterface?.onError(GoConstants.TAG_SD_NOT_READY)
                }
            }
        }
    }

    private fun getLatestSongLaunchedBy(): LaunchedBy {
        return goPreferences.latestPlayedSong?.launchedBy ?: LaunchedBy.ArtistView
    }

    // method to update info on controls panel
    private fun updatePlayingInfo(restore: Boolean) {

        val selectedSong = mMediaPlayerHolder.currentSong.first
        mFragmentPlayerBinding.songProgress.max = selectedSong?.duration!!.toInt()

        mFragmentPlayerBinding.playingSong.text = selectedSong.title

        mFragmentPlayerBinding.playingArtist.text =
            getString(
                R.string.artist_and_album,
                selectedSong.artist,
                selectedSong.album
            )

        updateRepeatStatus(false)

        if (isNowPlaying) updateNowPlayingInfo()

        if (restore) {

            if (!mMediaPlayerHolder.queueSongs.isNullOrEmpty() && !mMediaPlayerHolder.isQueueStarted) {
                onQueueEnabled()
            } else {
                onQueueStartedOrEnded(mMediaPlayerHolder.isQueueStarted)
            }

            updatePlayingStatus(false)

            if (::mPlayerService.isInitialized) {
                //stop foreground if coming from pause state
                mPlayerService.apply {
                    if (isRestoredFromPause) {
                        stopForeground(false)
                        musicNotificationManager.updateNotification()
                        isRestoredFromPause = false
                    }
                }
            }
        }
    }

    private fun updateRepeatStatus(onPlaybackCompletion: Boolean) {
        if (isNowPlaying) {
            mNowPlayingControlsBinding.npRepeat.setImageResource(
                ThemeHelper.getRepeatIcon(
                    mMediaPlayerHolder
                )
            )
            when {
                onPlaybackCompletion -> ThemeHelper.updateIconTint(
                    mNowPlayingControlsBinding.npRepeat,
                    mResolvedIconsColor
                )
                mMediaPlayerHolder.isRepeat1X or mMediaPlayerHolder.isLooping -> {
                    ThemeHelper.updateIconTint(
                        mNowPlayingControlsBinding.npRepeat,
                        mResolvedAccentColor
                    )
                }
                else -> ThemeHelper.updateIconTint(
                    mNowPlayingControlsBinding.npRepeat,
                    mResolvedIconsColor
                )
            }
        }
    }

    private fun updateNowPlayingInfo() {

        val selectedSong = mMediaPlayerHolder.currentSong.first
        val selectedSongDuration = selectedSong?.duration!!

        mNowPlayingBinding.npSong.text = selectedSong.title

        mNowPlayingBinding.npArtistAlbum.text =
            getString(
                R.string.artist_and_album,
                selectedSong.artist,
                selectedSong.album
            )

        mNowPlayingBinding.npSeek.text =
            mMediaPlayerHolder.playerPosition.toLong().toFormattedDuration(false, isSeekBar = true)
        mNowPlayingBinding.npDuration.text =
            selectedSongDuration.toFormattedDuration(false, isSeekBar = true)

        mNowPlayingBinding.npSeekBar.max = selectedSong.duration.toInt()

        selectedSong.id?.toContentUri()?.toBitrate(requireContext())?.let { bitrateInfo ->
            mNowPlayingBinding.npRates.text =
                getString(R.string.rates, bitrateInfo.first, bitrateInfo.second)
        }

        updatePlayingStatus(true)
    }

    private fun getSongSource(selectedSong: Music?, launchedBy: LaunchedBy): String? {
        return when (launchedBy) {
            LaunchedBy.FolderView -> selectedSong?.relativePath
            LaunchedBy.ArtistView -> selectedSong?.artist
            else -> selectedSong?.album
        }
    }

    private fun openPlayingArtistAlbum() {

        if (mMediaPlayerHolder.isCurrentSong) {

            val launchedBy = mMediaPlayerHolder.launchedBy
            val selectedSong = mMediaPlayerHolder.currentSong.first
            val selectedArtistOrFolder = getSongSource(selectedSong, launchedBy)

            if (getDetailsFragment() == null || getDetailsFragment()?.onSnapToPosition(
                    selectedArtistOrFolder
                )!!
            ) {
                mUIControlInterface?.onArtistOrFolderSelected(selectedArtistOrFolder!!, launchedBy)
            } else {
                getDetailsFragment()?.onSnapToPosition(selectedArtistOrFolder)
            }

            if (isNowPlaying) {
                mNowPlayingDialog.dismiss()
            }
        }
    }

    private fun getDetailsFragment(): DetailsFragment? {
        val navHostFragment = requireActivity().supportFragmentManager.primaryNavigationFragment
        val fragment = navHostFragment?.childFragmentManager!!.fragments[0]
        return if (fragment is DetailsFragment) {
            fragment
        } else {
            null
        }
    }

    private fun openQueueDialog() {
        if (mMediaPlayerHolder.queueSongs.isNotEmpty()
        ) {
            mQueueDialog = DialogHelper.showQueueSongsDialog(requireContext())
            mQueueAdapter = mQueueDialog.getListAdapter() as QueueAdapter
        } else {
            getString(R.string.error_no_queue).toToast(requireContext())
        }
    }

    // interface to let MediaPlayerHolder update the UI media player controls.
    override fun onPlaybackCompleted() {
        updateRepeatStatus(true)
    }

    override fun onUpdateRepeatStatus() {
        updateRepeatStatus(false)
    }

    override fun onClose() {
        //finish activity if visible
        requireActivity().finishAndRemoveTask()
    }

    override fun onPositionChanged(position: Int) {
        mFragmentPlayerBinding.songProgress.progress = position
        if (isNowPlaying) mNowPlayingBinding.npSeekBar.progress = position
    }

    override fun onStateChanged() {
        updatePlayingStatus(false)
        updatePlayingStatus(isNowPlaying)
        if (mMediaPlayerHolder.state != GoConstants.RESUMED && mMediaPlayerHolder.state != GoConstants.PAUSED) {
            updatePlayingInfo(false)
            if (::mQueueDialog.isInitialized && mQueueDialog.isShowing && mMediaPlayerHolder.isQueue)
                mQueueAdapter.swapSelectedSong(
                    mMediaPlayerHolder.currentSong.first
                )
        }
    }

    override fun onQueueEnabled() {
        ThemeHelper.updateIconTint(
            mFragmentPlayerBinding.queueButton,
            mResolvedIconsColor
        )
    }

    override fun onQueueCleared() {
        if (::mQueueDialog.isInitialized && mQueueDialog.isShowing) {
            mQueueDialog.dismiss()
        }
    }

    override fun onQueueStartedOrEnded(started: Boolean) {
        ThemeHelper.updateIconTint(
            mFragmentPlayerBinding.queueButton,
            when {
                started -> mResolvedAccentColor
                mMediaPlayerHolder.isQueue -> mResolvedIconsColor
                else -> mResolvedDisabledIconsColor
            }
        )
    }

    override fun onAddToQueue(song: Music?) {
        mMediaPlayerHolder.apply {
            if (queueSongs.isEmpty()) setQueueEnabled(true)
            song?.let { songToQueue ->
                queueSongs.add(songToQueue)
                getString(
                    R.string.queue_song_add,
                    songToQueue.title
                ).toToast(
                    requireContext()
                )
            }
        }
    }

    override fun onHandleFocusPref() {
        mMediaPlayerHolder.onFocusPrefChanged(goPreferences.isFocusEnabled)
    }

    override fun onShuffleSongs(songs: MutableList<Music>?, launchedBy: LaunchedBy) {
        val randomNumber = (0 until songs?.size!!).getRandom()
        songs.shuffle()
        val song = songs[randomNumber]
        onSongSelected(song, songs, launchedBy)
    }

    override fun onSongSelected(song: Music?, songs: List<Music>?, launchedBy: LaunchedBy) {
        mMediaPlayerHolder.apply {
            isSongRestoredFromPrefs = false
            if (!isPlay) isPlay = true
            if (isQueue) setQueueEnabled(false)
            onStartPlayback(song, songs, launchedBy)
        }
    }

    override fun onSaveSongToPref() {
        if (!mMediaPlayerHolder.isPlaying || mMediaPlayerHolder.state == GoConstants.PAUSED) mMediaPlayerHolder.apply {
            MusicOrgHelper.saveLatestSong(currentSong.first, launchedBy)
        }
    }

    override fun onStartPlayback(song: Music?, songs: List<Music>?, launchedBy: LaunchedBy) {
        if (::mPlayerService.isInitialized && !mPlayerService.isRunning) {
            requireContext().startService(
                mBindingIntent
            )
        }
        mMediaPlayerHolder.apply {
            setCurrentSong(song, songs, isFromQueue = false, isFolderAlbum = launchedBy)
            setSongDataSource(song)
        }
    }
}
