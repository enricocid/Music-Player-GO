package com.iven.musicplayergo.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.view.View
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.getListAdapter
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicRepository
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.QueueAdapter
import com.iven.musicplayergo.databinding.FragmentPlayerBinding
import com.iven.musicplayergo.dialogs.NowPlayingBottomSheet
import com.iven.musicplayergo.enums.LaunchedBy
import com.iven.musicplayergo.extensions.decodeColor
import com.iven.musicplayergo.extensions.getRandom
import com.iven.musicplayergo.extensions.toToast
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.MusicOrgHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.interfaces.MediaPlayerInterface
import com.iven.musicplayergo.interfaces.UIControlInterface
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.navigation.DetailsFragment


class PlayerFragment : Fragment(R.layout.fragment_player),
    MediaPlayerInterface {

    private var mUIControlInterface: UIControlInterface? = null

    // View binding classes
    private lateinit var mFragmentPlayerBinding: FragmentPlayerBinding

    private fun getDetailsFragment(): Pair<Boolean, DetailsFragment?> {
        val navHostFragment = requireActivity().supportFragmentManager.primaryNavigationFragment
        val fragment = navHostFragment?.childFragmentManager!!.fragments[0] as? DetailsFragment
        return Pair(fragment != null, fragment)
    }

    private val mMusicRepository: MusicRepository get() = MusicRepository.getInstance()

    // Colors
    private val mResolvedAccentColor get() = ThemeHelper.resolveThemeAccent(requireContext())

    private val mResolvedIconsColor get() = R.color.widgetsColor.decodeColor(requireContext())
    private val mResolvedDisabledIconsColor
        get() = ThemeHelper.resolveColorAttr(
            requireContext(),
            android.R.attr.colorButtonNormal
        )

    // Music player things
    private lateinit var mQueueDialog: MaterialDialog
    private lateinit var mQueueAdapter: QueueAdapter

    //first: is null, second: fragment
    private var mNowPlayingBottomSheet: NowPlayingBottomSheet? = null

    // The player
    private val mMediaPlayerHolder: MediaPlayerHolder get() = MediaPlayerHolder.getInstance()

    // Our PlayerService shit
    private lateinit var mPlayerService: PlayerService
    private var sBound = false
    private lateinit var mBindingIntent: Intent

    private var sThemeApplied = false

    // Defines callbacks for service binding, passed to bindService()
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {

            val binder = service as PlayerService.LocalBinder
            mPlayerService = binder.getService()

            sBound = true

            mMediaPlayerHolder.setPlayerService(mPlayerService)

            if (::mPlayerService.isInitialized && !mPlayerService.isRunning) {
                requireContext().startService(
                    mBindingIntent
                )
            }
            handleRestore()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            sBound = false
        }
    }

    // Pause SeekBar callback
    override fun onPause() {
        super.onPause()
        if (mMediaPlayerHolder.isCurrentSong) {
            onSaveSongToPref()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        doUnbindService()
    }

    override fun onThemeApplied() {
        sThemeApplied = true
    }

    private fun doUnbindService() {
        if (sBound) {
            requireContext().unbindService(connection)
            if (mMediaPlayerHolder.state != GoConstants.PLAYING && ::mPlayerService.isInitialized && mPlayerService.isRunning) {
                mPlayerService.stopForeground(true)
                if (!sThemeApplied) {
                    requireContext().stopService(mBindingIntent)
                }
                if (sThemeApplied) {
                    sThemeApplied = false
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mFragmentPlayerBinding = FragmentPlayerBinding.bind(view)

        mMediaPlayerHolder.mediaPlayerInterface = this

        initMediaButtons()

        doBindService()
    }

    override fun onSkipNP(isNext: Boolean) {
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

    override fun onSetRepeatNP() {
        mMediaPlayerHolder.repeat(mMediaPlayerHolder.state == GoConstants.PAUSED)
        if (mNowPlayingBottomSheet != null) {
            mNowPlayingBottomSheet?.updateRepeatStatus(false)
        }
    }

    override fun onOpenPlayingArtistAlbumNP() {
        openPlayingArtistAlbum()
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
                setOnClickListener {
                    if (mMediaPlayerHolder.isCurrentSong) {
                        NowPlayingBottomSheet.newInstance().show(
                            childFragmentManager,
                            NowPlayingBottomSheet.TAG_NOW_PLAYING
                        )
                    }
                }
                setOnLongClickListener {
                    openPlayingArtistAlbum()
                    return@setOnLongClickListener true
                }
            }
        }
    }

    override fun onBottomSheetCreated(nowPlayingBottomSheet: NowPlayingBottomSheet) {
        mNowPlayingBottomSheet = nowPlayingBottomSheet
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

    private fun updatePlayingStatus() {
        val isPlaying = mMediaPlayerHolder.state != GoConstants.PAUSED
        val drawable =
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
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

            if (state != GoConstants.PAUSED) {

                startUpdatingCallbackWithPosition()
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

        if (mNowPlayingBottomSheet != null) {
            mNowPlayingBottomSheet?.apply {
                updateRepeatStatus(false)
                updateNowPlayingInfo()
            }
        }

        if (restore) {

            if (!mMediaPlayerHolder.queueSongs.isNullOrEmpty() && !mMediaPlayerHolder.isQueueStarted) {
                onQueueEnabled()
            } else {
                onQueueStartedOrEnded(mMediaPlayerHolder.isQueueStarted)
            }

            updatePlayingStatus()

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

            if (!getDetailsFragment().first || getDetailsFragment().first && getDetailsFragment().second?.onSnapToPosition(
                    selectedArtistOrFolder
                )!!
            ) {
                mUIControlInterface?.onArtistOrFolderSelected(selectedArtistOrFolder!!, launchedBy)
            } else if (getDetailsFragment().first) {
                getDetailsFragment().second?.onSnapToPosition(selectedArtistOrFolder)
            }
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
        if (mNowPlayingBottomSheet != null) {
            mNowPlayingBottomSheet?.updateRepeatStatus(true)
        }
    }

    override fun onUpdateRepeatStatus() {
        if (mNowPlayingBottomSheet != null) {
            mNowPlayingBottomSheet?.updateRepeatStatus(false)
        }
    }

    override fun onClose() {
        //finish activity if visible
        mUIControlInterface?.onCloseActivity(false)
    }

    override fun onPositionChanged(position: Int) {
        mFragmentPlayerBinding.songProgress.progress = position
        if (mNowPlayingBottomSheet != null) {
            mNowPlayingBottomSheet?.updateProgress(position)
        }
    }

    override fun onStateChanged() {

        updatePlayingStatus()

        if (mNowPlayingBottomSheet != null) {
            mNowPlayingBottomSheet?.updatePlayingStatus()
        }

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

    override fun onDismissNP() {
        mNowPlayingBottomSheet = null
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
        if (mMediaPlayerHolder.state == GoConstants.PAUSED) {
            mMediaPlayerHolder.apply {
                MusicOrgHelper.saveLatestSong(currentSong.first, launchedBy)
            }
        }
    }

    override fun onStartPlayback(song: Music?, songs: List<Music>?, launchedBy: LaunchedBy) {
        mMediaPlayerHolder.apply {
            setCurrentSong(song, songs, isFromQueue = false, isFolderAlbum = launchedBy)
            setSongDataSource(song)
        }
    }
}
