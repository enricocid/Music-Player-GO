package com.iven.musicplayergo

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.google.android.material.tabs.TabLayout
import com.iven.musicplayergo.adapters.QueueAdapter
import com.iven.musicplayergo.fragments.*
import com.iven.musicplayergo.music.Album
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.music.MusicViewModel
import com.iven.musicplayergo.player.*
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.UIControlInterface
import com.iven.musicplayergo.ui.Utils
import com.oze.music.musicbar.FixedMusicBar
import com.oze.music.musicbar.MusicBar
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.player_controls_panel.*
import kotlin.properties.Delegates

@Suppress("UNUSED_PARAMETER")
class MainActivity : AppCompatActivity(), UIControlInterface {

    //colors
    private var mResolvedAccentColor: Int by Delegates.notNull()
    private var mResolvedAlphaAccentColor: Int by Delegates.notNull()
    private var mResolvedIconsColor: Int by Delegates.notNull()
    private var mResolvedDisabledIconsColor: Int by Delegates.notNull()

    //fragments
    private lateinit var mArtistsFragment: ArtistsFragment
    private lateinit var mAllMusicFragment: AllMusicFragment
    private lateinit var mFoldersFragment: FoldersFragment
    private lateinit var mSettingsFragment: SettingsFragment
    private lateinit var mDetailsFragment: DetailsFragment

    private lateinit var mActiveFragments: MutableList<String>

    //views
    private lateinit var mViewPager: ViewPager
    private lateinit var mTabLayout: TabLayout
    private lateinit var mPlayerControlsContainer: View

    //settings/controls panel
    private lateinit var mPlayingArtist: TextView
    private lateinit var mPlayingSong: TextView
    private lateinit var mSeekProgressBar: ProgressBar
    private lateinit var mPlayPauseButton: ImageView
    private lateinit var mLovedSongsButton: ImageView
    private lateinit var mLoveSongsNumber: TextView
    private lateinit var mQueueButton: ImageView

    private var mControlsPaddingNoTabs: Int by Delegates.notNull()
    private var mControlsPaddingNormal: Int by Delegates.notNull()
    private var mControlsPaddingEnd: Int by Delegates.notNull()

    //now playing
    private lateinit var mNowPlayingDialog: MaterialDialog
    private lateinit var mFixedMusicBar: FixedMusicBar
    private lateinit var mSongTextNP: TextView
    private lateinit var mArtistAlbumTextNP: TextView
    private lateinit var mSongSeekTextNP: TextView
    private lateinit var mSongDurationTextNP: TextView
    private lateinit var mSkipPrevButtonNP: ImageView
    private lateinit var mPlayPauseButtonNP: ImageView
    private lateinit var mSkipNextButtonNP: ImageView
    private lateinit var mRepeatNP: ImageView
    private lateinit var mVolumeSeekBarNP: SeekBar
    private lateinit var mLoveButtonNP: ImageView
    private lateinit var mVolumeNP: ImageView

    //music player things

    //view model
    private val mViewModel: MusicViewModel by lazy {
        ViewModelProviders.of(this).get(MusicViewModel::class.java)
    }

    private lateinit var mAllDeviceSongs: MutableList<Music>

    //booleans
    private var sUserIsSeeking = false

    //music
    private lateinit var mMusic: Map<String, List<Album>>

    private lateinit var mQueueDialog: Pair<MaterialDialog, QueueAdapter>

    //the player
    private lateinit var mMediaPlayerHolder: MediaPlayerHolder
    private val isMediaPlayerHolder get() = ::mMediaPlayerHolder.isInitialized

    //our PlayerService shit
    private lateinit var mPlayerService: PlayerService
    private var sBound: Boolean = false
    private lateinit var mBindingIntent: Intent

    private fun isNowPlaying(): Boolean {
        return ::mNowPlayingDialog.isInitialized && mNowPlayingDialog.isShowing
    }

    private fun checkIsPlayer(showError: Boolean): Boolean {
        return mMediaPlayerHolder.apply {
            if (!isMediaPlayer && !mMediaPlayerHolder.isSongRestoredFromPrefs && showError) EqualizerUtils.notifyNoSessionId(
                this@MainActivity
            )
        }.isMediaPlayer
    }

    //Defines callbacks for service binding, passed to bindService()
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            val binder = service as PlayerService.LocalBinder
            mPlayerService = binder.getService()
            sBound = true
            mMediaPlayerHolder = mPlayerService.mediaPlayerHolder
            mMediaPlayerHolder.mediaPlayerInterface = mMediaPlayerInterface

            loadMusicAndSetupUI()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            sBound = false
        }
    }

    private fun doBindService() {
        // Bind to LocalService
        mBindingIntent = Intent(this, PlayerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onBackPressed() {
        if (::mDetailsFragment.isInitialized && mDetailsFragment.isAdded) {
            mDetailsFragment.onHandleBackPressed().doOnEnd {
                super.onBackPressed()
            }
        } else {
            if (mViewPager.currentItem != 0) mViewPager.currentItem = 0 else
                if (isMediaPlayerHolder && mMediaPlayerHolder.isPlaying) Utils.stopPlaybackDialog(
                    this,
                    mMediaPlayerHolder
                ) else super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (sBound) unbindService(connection)
        if (isMediaPlayerHolder && !mMediaPlayerHolder.isPlaying && ::mPlayerService.isInitialized && mPlayerService.isRunning) {
            mPlayerService.stopForeground(true)
            stopService(mBindingIntent)
        }
    }

    //restore recycler views state
    override fun onResume() {
        super.onResume()
        if (isMediaPlayerHolder && mMediaPlayerHolder.isMediaPlayer) mMediaPlayerHolder.onResumeActivity()
    }

    //save recycler views state
    override fun onPause() {
        super.onPause()
        if (isMediaPlayerHolder && mMediaPlayerHolder.isMediaPlayer) mMediaPlayerHolder.onPauseActivity()
    }

    //manage request permission result, continue loading ui if permissions was granted
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) Utils.showPermissionRationale(
            this
        ) else doBindService()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (goPreferences.isEdgeToEdge && window != null) ThemeHelper.handleEdgeToEdge(
            window,
            mainView
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //set ui theme
        setTheme(ThemeHelper.getAccentedTheme().first)
        ThemeHelper.applyTheme(this, goPreferences.theme)

        mResolvedAccentColor = ThemeHelper.resolveThemeAccent(this)
        mResolvedAlphaAccentColor = ThemeHelper.getAlphaAccent(this, 50)
        mResolvedIconsColor = ThemeHelper.resolveColorAttr(this, android.R.attr.textColorPrimary)
        mResolvedDisabledIconsColor =
            ThemeHelper.resolveColorAttr(this, android.R.attr.colorButtonNormal)

        resources.apply {
            mControlsPaddingNormal = getDimensionPixelSize(R.dimen.player_controls_padding_normal)
            mControlsPaddingEnd = getDimensionPixelSize(R.dimen.player_controls_padding_end)
            mControlsPaddingNoTabs = getDimensionPixelSize(R.dimen.player_controls_padding_no_tabs)
        }

        setContentView(R.layout.main_activity)

        //init views
        getViews()

        mPlayPauseButton.setOnClickListener { resumeOrPause() }

        mLovedSongsButton.setOnLongClickListener {
            if (!goPreferences.lovedSongs.isNullOrEmpty()) Utils.showClearLovedSongDialog(
                this,
                this
            )
            return@setOnLongClickListener true
        }

        mQueueButton.setOnLongClickListener {
            if (checkIsPlayer(true) && mMediaPlayerHolder.isQueue) Utils.showClearQueueDialog(
                this,
                mMediaPlayerHolder
            )
            return@setOnLongClickListener true
        }

        onLovedSongsUpdate(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) checkPermission() else doBindService()
    }

    private fun loadMusicAndSetupUI() {

        mViewModel.loadMusic(this).observe(this, Observer { hasLoaded ->

            //setup all the views if there's something
            if (hasLoaded && musicLibrary.allAlbumsForArtist.isNotEmpty()) {

                mArtistsFragment = ArtistsFragment.newInstance()
                mAllMusicFragment = AllMusicFragment.newInstance()
                mFoldersFragment = FoldersFragment.newInstance()
                mSettingsFragment = SettingsFragment.newInstance()

                mActiveFragments =
                    goPreferences.activeFragments?.toMutableList()!!

                if (goPreferences.isTabsEnabled) {
                    mTabLayout.setupWithViewPager(mViewPager)
                    mPlayerControlsContainer.setPadding(
                        mControlsPaddingNoTabs,
                        mControlsPaddingNormal,
                        mControlsPaddingEnd,
                        mControlsPaddingNormal
                    )

                    mTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

                        override fun onTabSelected(tab: TabLayout.Tab) {
                            tab.icon?.setTint(mResolvedAccentColor)
                        }

                        override fun onTabUnselected(tab: TabLayout.Tab) {
                            tab.icon?.setTint(mResolvedAlphaAccentColor)
                        }

                        override fun onTabReselected(tab: TabLayout.Tab) {
                        }
                    })

                } else {
                    mTabLayout.visibility = View.GONE
                    mPlayerControlsContainer.setPadding(
                        mControlsPaddingNoTabs,
                        mControlsPaddingNoTabs,
                        mControlsPaddingEnd,
                        mControlsPaddingNoTabs
                    )
                }

                val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
                mViewPager.adapter = pagerAdapter

                if (goPreferences.isTabsEnabled)
                    mActiveFragments.iterator().forEach {
                        setupTabLayoutTabs(mActiveFragments.indexOf(it), it.toInt())
                    }

                mAllDeviceSongs = musicLibrary.allSongsUnfiltered
                mMusic = musicLibrary.allAlbumsForArtist

                mPlayerControlsContainer.setOnLongClickListener {
                    openPlayingArtistAlbum(it)
                    return@setOnLongClickListener true
                }

                //let's get intent from external app and open the song,
                //else restore the player (normal usage)
                if (intent != null && Intent.ACTION_VIEW == intent.action && intent.data != null)
                    handleIntent(intent)
                else
                    restorePlayerStatus()

            } else {
                Utils.makeToast(
                    this@MainActivity,
                    getString(R.string.error_no_music)
                )
                finish()
            }
        })
    }

    private fun getViews() {

        mViewPager = pager
        mTabLayout = tab_layout
        mPlayerControlsContainer = playing_songs_container

        //controls panel
        mPlayingSong = playing_song
        mPlayingArtist = playing_artist
        mSeekProgressBar = song_progress
        mPlayPauseButton = play_pause_button

        mLovedSongsButton = loved_songs_button
        mLoveSongsNumber = loved_songs_number
        mQueueButton = queue_button
    }

    private fun handleOnNavigationItemSelected(itemId: Int): Fragment {
        return when (itemId) {
            0 -> getFragmentForIndex(mActiveFragments[0].toInt())
            1 -> getFragmentForIndex(mActiveFragments[1].toInt())
            2 -> getFragmentForIndex(mActiveFragments[2].toInt())
            else -> getFragmentForIndex(mActiveFragments[3].toInt())
        }
    }

    private fun getFragmentForIndex(index: Int): Fragment {
        return when (index) {
            0 -> mArtistsFragment
            1 -> mAllMusicFragment
            2 -> mFoldersFragment
            else -> mSettingsFragment
        }
    }

    private fun setupTabLayoutTabs(tabIndex: Int, iconIndex: Int) {
        mTabLayout.apply {
            getTabAt(tabIndex)?.setIcon(ThemeHelper.getTabIcon(iconIndex))
            if (tabIndex != 0) getTabAt(tabIndex)?.icon?.setTint(mResolvedAlphaAccentColor)
            else getTabAt(tabIndex)?.icon?.setTint(mResolvedAccentColor)
        }
    }

    private fun openDetailsFragment(
        selectedArtistOrFolder: String,
        isFolder: Boolean
    ) {

        mDetailsFragment =
            DetailsFragment.newInstance(
                selectedArtistOrFolder,
                isFolder,
                MusicUtils.getPlayingAlbumPosition(selectedArtistOrFolder, mMediaPlayerHolder)
            )
        supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .add(
                R.id.container,
                mDetailsFragment, DetailsFragment.TAG_ARTIST_FOLDER
            )
            .commit()
    }

    /**
    UI related methods
     */
    private fun setFixedMusicBarProgressListener() {

        mFixedMusicBar.setProgressChangeListener(
            object : MusicBar.OnMusicBarProgressChangeListener {

                val defaultPositionColor = mSongSeekTextNP.currentTextColor
                var userSelectedPosition = 0

                override fun onProgressChanged(
                    musicBar: MusicBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (sUserIsSeeking) {
                        userSelectedPosition = musicBar.position
                        mSongSeekTextNP.setTextColor(
                            mResolvedAccentColor
                        )
                    }
                    mSongSeekTextNP.text =
                        MusicUtils.formatSongDuration(musicBar.position.toLong(), false)
                }

                override fun onStartTrackingTouch(musicBar: MusicBar?) {
                    sUserIsSeeking = true
                }

                override fun onStopTrackingTouch(musicBar: MusicBar) {
                    if (sUserIsSeeking) {
                        mSongSeekTextNP.setTextColor(defaultPositionColor)
                    }
                    sUserIsSeeking = false
                    if (mMediaPlayerHolder.state != PLAYING) {
                        mSeekProgressBar.progress = userSelectedPosition
                        mFixedMusicBar.setProgress(userSelectedPosition)
                    }
                    mMediaPlayerHolder.seekTo(userSelectedPosition)
                }
            })
    }

    private fun setupPreciseVolumeHandler() {

        var isUserSeeking = false

        mVolumeSeekBarNP.progress = mMediaPlayerHolder.currentVolumeInPercent
        mVolumeSeekBarNP.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (isUserSeeking) {
                    mMediaPlayerHolder.setPreciseVolume(i)
                    ThemeHelper.updateIconTint(
                        mVolumeNP,
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
                    mVolumeNP,
                    mResolvedIconsColor
                )
            }
        })
    }

    fun openNowPlaying(view: View) {

        if (isMediaPlayerHolder && mMediaPlayerHolder.isCurrentSong) {

            mNowPlayingDialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

                cornerRadius(res = R.dimen.md_corner_radius)

                customView(R.layout.now_playing)

                mSongTextNP = getCustomView().findViewById(R.id.np_song)
                mSongTextNP.isSelected = true
                mArtistAlbumTextNP = getCustomView().findViewById(R.id.np_artist_album)
                mArtistAlbumTextNP.isSelected = true
                mFixedMusicBar = getCustomView().findViewById(R.id.np_fixed_music_bar)
                mFixedMusicBar.setBackgroundBarPrimeColor(
                    mResolvedAlphaAccentColor
                )

                mSongSeekTextNP = getCustomView().findViewById(R.id.np_seek)
                mSongDurationTextNP = getCustomView().findViewById(R.id.np_duration)

                mSkipPrevButtonNP = getCustomView().findViewById(R.id.np_skip_prev)
                mSkipPrevButtonNP.setOnClickListener { skip(false) }

                mPlayPauseButtonNP = getCustomView().findViewById(R.id.np_play)
                mPlayPauseButtonNP.setOnClickListener { resumeOrPause() }

                mSkipNextButtonNP = getCustomView().findViewById(R.id.np_skip_next)
                mSkipNextButtonNP.setOnClickListener { skip(true) }

                mRepeatNP = getCustomView().findViewById(R.id.np_repeat)

                ThemeHelper.updateIconTint(
                    mRepeatNP,
                    if (mMediaPlayerHolder.isReset) mResolvedAccentColor else
                        mResolvedIconsColor
                )

                mRepeatNP.setOnClickListener { setRepeat() }

                mLoveButtonNP = getCustomView().findViewById(R.id.np_love)
                mLoveButtonNP.setOnClickListener {
                    Utils.addToLovedSongs(
                        this@MainActivity,
                        mMediaPlayerHolder.currentSong.first,
                        mMediaPlayerHolder.playerPosition
                    )
                    onLovedSongsUpdate(false)
                }

                mVolumeSeekBarNP = getCustomView().findViewById(R.id.np_volume_seek)
                mVolumeNP = getCustomView().findViewById(R.id.np_volume)

                setupPreciseVolumeHandler()
                setFixedMusicBarProgressListener()

                updateNowPlayingInfo()

                onDismiss {
                    mFixedMusicBar.removeAllListener()
                }
            }

            if (goPreferences.isEdgeToEdge && mNowPlayingDialog.window != null) ThemeHelper.handleEdgeToEdge(
                mNowPlayingDialog.window,
                mNowPlayingDialog.view
            )
        }
    }

    override fun onAccentUpdated() {
        if (mMediaPlayerHolder.isPlaying) mMediaPlayerHolder.updateNotification()
    }

    private fun updatePlayingStatus(isNowPlaying: Boolean) {
        val isPlaying = mMediaPlayerHolder.state != PAUSED
        val drawable =
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        if (isNowPlaying) mPlayPauseButtonNP.setImageResource(drawable) else mPlayPauseButton.setImageResource(
            drawable
        )
    }

    override fun onLovedSongsUpdate(clear: Boolean) {

        val lovedSongs = goPreferences.lovedSongs

        if (clear) {
            lovedSongs?.clear()
            goPreferences.lovedSongs = lovedSongs
        }

        val lovedSongsButtonColor = if (lovedSongs.isNullOrEmpty())
            mResolvedDisabledIconsColor else
            ContextCompat.getColor(this, R.color.red)
        ThemeHelper.updateIconTint(mLovedSongsButton, lovedSongsButtonColor)
        val songsNumber = if (lovedSongs.isNullOrEmpty()) 0 else lovedSongs.size
        mLoveSongsNumber.text = songsNumber.toString()
    }

    private fun restorePlayerStatus() {
        if (isMediaPlayerHolder) {
            //if we are playing and the activity was restarted
            //update the controls panel

            mMediaPlayerHolder.apply {

                if (isMediaPlayer) {
                    onResumeActivity()
                    updatePlayingInfo(true)
                } else {
                    if (goPreferences.lastPlayedSong != null) {
                        val lastPlayedSong = goPreferences.lastPlayedSong
                        val song = lastPlayedSong?.first!!

                        val songs =
                            MusicUtils.getAlbumFromList(song.album, mMusic[song.artist]!!)
                                .first
                                .music

                        isSongRestoredFromPrefs = true
                        isPlay = false

                        startPlayback(song, songs!!)

                        updatePlayingInfo(false)

                        mSeekProgressBar.progress = lastPlayedSong.second
                    }
                }
            }
        }
    }

    //method to update info on controls panel
    private fun updatePlayingInfo(restore: Boolean) {

        val selectedSong = mMediaPlayerHolder.currentSong.first
        mSeekProgressBar.max = selectedSong.duration.toInt()

        mPlayingSong.text = selectedSong.title

        mPlayingArtist.text =
            getString(
                R.string.artist_and_album,
                selectedSong.artist,
                selectedSong.album
            )

        updateResetStatus(false)

        if (isNowPlaying()) updateNowPlayingInfo()

        if (restore) {

            if (mMediaPlayerHolder.currentSong.second) mMediaPlayerInterface.onQueueStartedOrEnded(
                true
            )
            else if (mMediaPlayerHolder.queueSongs.isNotEmpty()) mMediaPlayerInterface.onQueueEnabled()

            updatePlayingStatus(false)

            //stop foreground if coming from pause state
            mPlayerService.apply {
                if (isRestoredFromPause) {
                    stopForeground(false)
                    musicNotificationManager.notificationManager.notify(
                        NOTIFICATION_ID,
                        mPlayerService.musicNotificationManager.notificationBuilder.build()
                    )
                    isRestoredFromPause = false
                }
            }
        }
    }

    private fun updateResetStatus(onPlaybackCompletion: Boolean) {
        if (isNowPlaying()) {

            when {
                onPlaybackCompletion -> ThemeHelper.updateIconTint(mRepeatNP, mResolvedIconsColor)
                mMediaPlayerHolder.isReset -> ThemeHelper.updateIconTint(
                    mRepeatNP,
                    ThemeHelper.resolveThemeAccent(this)
                )
                else -> ThemeHelper.updateIconTint(mRepeatNP, mResolvedIconsColor)
            }
        }
    }

    private fun updateNowPlayingInfo() {

        val selectedSong = mMediaPlayerHolder.currentSong.first
        val selectedSongDuration = selectedSong.duration

        mSongTextNP.text = selectedSong.title

        mArtistAlbumTextNP.text =
            getString(
                R.string.artist_and_album,
                selectedSong.artist,
                selectedSong.album
            )

        mSongSeekTextNP.text =
            MusicUtils.formatSongDuration(mMediaPlayerHolder.playerPosition.toLong(), false)
        mSongDurationTextNP.text = MusicUtils.formatSongDuration(selectedSongDuration, false)

        mFixedMusicBar.loadFrom(selectedSong.path, selectedSong.duration.toInt())

        updatePlayingStatus(true)
    }

    fun openPlayingArtistAlbum(view: View) {
        if (isMediaPlayerHolder && mMediaPlayerHolder.isCurrentSong) {

            val selectedSong = mMediaPlayerHolder.currentSong.first
            val selectedArtistOrFolder = selectedSong.artist!!
            if (::mDetailsFragment.isInitialized && mDetailsFragment.isAdded && !mDetailsFragment.isFolder)
                mDetailsFragment.updateView(
                    selectedArtistOrFolder,
                    MusicUtils.getPlayingAlbumPosition(selectedArtistOrFolder, mMediaPlayerHolder)
                )
            else
                openDetailsFragment(selectedArtistOrFolder, isFolder = false)

            if (isNowPlaying()) mNowPlayingDialog.dismiss()
        }
    }

    /**
    Music player/playback related methods
     */
    override fun onCloseActivity() {
        if (isMediaPlayerHolder && mMediaPlayerHolder.isPlaying) Utils.stopPlaybackDialog(
            this,
            mMediaPlayerHolder
        ) else super.onBackPressed()
    }

    override fun onArtistOrFolderSelected(artistOrFolder: String, isFolder: Boolean) {
        openDetailsFragment(
            artistOrFolder,
            isFolder
        )
    }

    private fun startPlayback(song: Music, album: List<Music>) {
        if (isMediaPlayerHolder && !mPlayerService.isRunning) startService(mBindingIntent)
        mMediaPlayerHolder.apply {
            setCurrentSong(song, album, false)
            initMediaPlayer(song)
        }
    }

    override fun onSongSelected(song: Music, songs: List<Music>) {
        mMediaPlayerHolder.apply {
            isSongRestoredFromPrefs = false
            if (!isPlay) isPlay = true
            if (isQueue) setQueueEnabled(false)
            startPlayback(song, songs)
        }
    }

    private fun resumeOrPause() {
        if (checkIsPlayer(true)) mMediaPlayerHolder.resumeOrPause()
    }

    private fun skip(isNext: Boolean) {
        if (checkIsPlayer(true)) {
            if (!mMediaPlayerHolder.isPlay) mMediaPlayerHolder.isPlay = true
            if (mMediaPlayerHolder.isSongRestoredFromPrefs) mMediaPlayerHolder.isSongRestoredFromPrefs =
                false
            if (isNext) mMediaPlayerHolder.skip(true) else mMediaPlayerHolder.instantReset()
        }
    }

    private fun setRepeat() {
        if (checkIsPlayer(true)) {
            mMediaPlayerHolder.reset()
            updateResetStatus(false)
        }
    }

    fun openEqualizer(view: View) {
        if (checkIsPlayer(true)) mMediaPlayerHolder.openEqualizer(this)
    }

    override fun onAddToQueue(song: Music) {
        if (checkIsPlayer(true)) {
            mMediaPlayerHolder.apply {
                if (queueSongs.isEmpty()) setQueueEnabled(true)
                queueSongs.add(song)
                Utils.makeToast(
                    this@MainActivity,
                    getString(
                        R.string.queue_song_add,
                        song.title
                    )
                )
            }
        }
    }

    override fun onShuffleSongs(songs: MutableList<Music>) {
        songs.shuffle()
        val song = songs[0]
        onSongSelected(song, songs)
    }

    fun openQueueDialog(view: View) {
        if (checkIsPlayer(false) && mMediaPlayerHolder.queueSongs.isNotEmpty())
            mQueueDialog = Utils.showQueueSongsDialog(this, mMediaPlayerHolder)
        else
            Utils.makeToast(
                this, getString(R.string.error_no_queue)
            )
    }

    fun openLovedSongsDialog(view: View) {
        if (!goPreferences.lovedSongs.isNullOrEmpty())
            Utils.showLovedSongsDialog(this, this, mMediaPlayerHolder)
        else
            Utils.makeToast(
                this, getString(R.string.error_no_loved_songs)
            )
    }

    @TargetApi(23)
    private fun checkPermission() {

        if (Utils.hasToShowPermissionRationale(this)) Utils.showPermissionRationale(this) else doBindService()
    }

    //method to handle intent to play audio file from external app
    private fun handleIntent(intent: Intent) {

        val uri = intent.data

        try {
            if (uri.toString().isNotEmpty()) {

                val path = MusicUtils.getRealPathFromURI(this, uri!!)

                //if we were able to get the song play it!
                if (MusicUtils.getSongForIntent(
                        path,
                        mAllDeviceSongs
                    ) != null
                ) {

                    val song =
                        MusicUtils.getSongForIntent(path, mAllDeviceSongs)!!

                    //get album songs and sort them
                    val albumSongs =
                        MusicUtils.getAlbumFromList(song.album, mMusic[song.artist]!!).first
                            .music

                    onSongSelected(song, albumSongs!!)

                } else {
                    Utils.makeToast(
                        this@MainActivity,
                        getString(R.string.error_unknown_unsupported)
                    )
                    finishAndRemoveTask()
                }
            }
        } catch (e: Exception) {
            Utils.makeToast(
                this@MainActivity,
                getString(R.string.error_unknown_unsupported)
            )
            finishAndRemoveTask()
        }
    }

    /**
     * Interface to let MediaPlayerHolder update the UI media player controls.
     */
    private val mMediaPlayerInterface = object : MediaPlayerInterface {

        override fun onPlaybackCompleted() {
            updateResetStatus(true)
        }

        override fun onUpdateResetStatus() {
            updateResetStatus(false)
        }

        override fun onClose() {
            //finish activity if visible
            finishAndRemoveTask()
        }

        override fun onPositionChanged(position: Int) {
            if (!sUserIsSeeking) {
                mSeekProgressBar.progress = position
                if (isNowPlaying()) mFixedMusicBar.setProgress(position)
            }
        }

        override fun onStateChanged() {
            updatePlayingStatus(false)
            updatePlayingStatus(isNowPlaying())
            if (mMediaPlayerHolder.state != RESUMED && mMediaPlayerHolder.state != PAUSED) {
                updatePlayingInfo(false)
                if (::mQueueDialog.isInitialized && mQueueDialog.first.isShowing && mMediaPlayerHolder.isQueue)
                    mQueueDialog.second.swapSelectedSong(
                        mMediaPlayerHolder.currentSong.first
                    )
            }
        }

        override fun onQueueEnabled() {
            ThemeHelper.updateIconTint(
                mQueueButton,
                mResolvedIconsColor
            )
        }

        override fun onQueueCleared() {
            if (::mQueueDialog.isInitialized && mQueueDialog.first.isShowing) mQueueDialog.first.dismiss()
        }

        override fun onQueueStartedOrEnded(started: Boolean) {
            ThemeHelper.updateIconTint(
                mQueueButton,
                when {
                    started -> mResolvedAccentColor
                    mMediaPlayerHolder.isQueue -> mResolvedIconsColor
                    else -> mResolvedDisabledIconsColor
                }
            )
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int = mActiveFragments.size

        override fun getItem(position: Int): Fragment {
            return handleOnNavigationItemSelected(position)
        }
    }
}
