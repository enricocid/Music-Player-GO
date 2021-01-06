package com.iven.musicplayergo.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.setPeekHeight
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.callbacks.onShow
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.list.getListAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.QueueAdapter
import com.iven.musicplayergo.databinding.*
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.fragments.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.*
import com.iven.musicplayergo.models.Album
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.EqualizerUtils
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.player.MediaPlayerInterface
import com.iven.musicplayergo.player.PlayerService
import de.halfbit.edgetoedge.Edge
import de.halfbit.edgetoedge.edgeToEdge
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity(), UIControlInterface {

    // View binding classes
    private lateinit var mMainActivityBinding: MainActivityBinding
    private lateinit var mPlayerControlsPanelBinding: PlayerControlsPanelBinding

    // View model
    private val mMusicViewModel: MusicViewModel by viewModels()

    // Fragments
    private val mActiveFragments: List<String> = goPreferences.activeTabs.toList()
    private var mArtistsFragment: MusicContainersListFragment? = null
    private var mAllMusicFragment: AllMusicFragment? = null
    private var mFoldersFragment: MusicContainersListFragment? = null
    private var mAlbumsFragment: MusicContainersListFragment? = null
    private var mSettingsFragment: SettingsFragment? = null
    private lateinit var mDetailsFragment: DetailsFragment
    private lateinit var mEqualizerFragment: EqFragment

    private val mMusicContainersFragments = mutableListOf<MusicContainersListFragment>()

    // Booleans
    private val sDetailsFragmentExpanded get() = supportFragmentManager.isFragment(GoConstants.DETAILS_FRAGMENT_TAG)
    private val sErrorFragmentExpanded get() = supportFragmentManager.isFragment(GoConstants.ERROR_FRAGMENT_TAG)
    private val sEqFragmentExpanded get() = supportFragmentManager.isFragment(GoConstants.EQ_FRAGMENT_TAG)

    private var sCloseDetailsFragment = true

    private var sRevealAnimationRunning = false

    private var mFragmentToRestore = 0

    // Loved songs dialog
    private lateinit var mLovedSongsDialog: MaterialDialog

    // Now playing
    private lateinit var mNowPlayingDialog: MaterialDialog
    private lateinit var mNowPlayingBinding: NowPlayingBinding
    private lateinit var mNowPlayingControlsBinding: NowPlayingControlsBinding
    private lateinit var mNowPlayingExtendedControlsBinding: NowPlayingExtendedControlsBinding

    /**
     * This is the job for all coroutines started by this ViewModel.
     * Cancelling this job will cancel all coroutines started by this ViewModel.
     */
    private val mLoadCoverJob = SupervisorJob()

    private val mLoadCoverHandler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    private val mLoadCoverIoDispatcher = Dispatchers.IO + mLoadCoverJob + mLoadCoverHandler
    private val mLoadCoverIoScope = CoroutineScope(mLoadCoverIoDispatcher)

    private lateinit var mImageLoader: ImageLoader

    private var mSelectedArtistAlbumForNP: Pair<String?, Long?> = Pair("", -1)

    // Music player things
    private lateinit var mQueueDialog: MaterialDialog
    private lateinit var mQueueAdapter: QueueAdapter

    // The player
    private lateinit var mMediaPlayerHolder: MediaPlayerHolder
    private val isMediaPlayerHolder get() = ::mMediaPlayerHolder.isInitialized
    private val isNowPlaying get() = ::mNowPlayingDialog.isInitialized && mNowPlayingDialog.isShowing

    // Our PlayerService shit
    private lateinit var mPlayerService: PlayerService
    private var sBound = false
    private lateinit var mBindingIntent: Intent

    private fun checkIsPlayer(showError: Boolean): Boolean {
        if (!isMediaPlayerHolder && !mMediaPlayerHolder.isMediaPlayer && !mMediaPlayerHolder.isSongRestoredFromPrefs && showError) {
            getString(
                R.string.error_bad_id
            ).toToast(
                this@MainActivity
            )
            return false
        }
        return true
    }

    // Defines callbacks for service binding, passed to bindService()
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            val binder = service as PlayerService.LocalBinder
            mPlayerService = binder.getService()
            sBound = true
            mMediaPlayerHolder = mPlayerService.mediaPlayerHolder
            mMediaPlayerHolder.mediaPlayerInterface = mMediaPlayerInterface

            mMusicViewModel.deviceMusic.observe(this@MainActivity, { returnedMusic ->
                finishSetup(returnedMusic)
            })
            mMusicViewModel.getDeviceMusic()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            sBound = false
        }
    }

    private fun doBindService() {
        mMainActivityBinding.loadingProgressBar.handleViewVisibility(true)
        mBindingIntent = Intent(this, PlayerService::class.java).also {
            bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onBackPressed() {
        when {
            sDetailsFragmentExpanded and !sEqFragmentExpanded -> closeDetailsFragment()
            !sDetailsFragmentExpanded and sEqFragmentExpanded -> closeEqualizerFragment()
            sEqFragmentExpanded and sDetailsFragmentExpanded -> if (sCloseDetailsFragment) {
                closeDetailsFragment()
            } else {
                closeEqualizerFragment()
            }
            sErrorFragmentExpanded -> finishAndRemoveTask()
            else -> if (mMainActivityBinding.viewPager2.currentItem != 0) {
                mMainActivityBinding.viewPager2.currentItem =
                    0
            } else {
                if (isMediaPlayerHolder && mMediaPlayerHolder.isPlaying) {
                    DialogHelper.stopPlaybackDialog(
                        this,
                        mMediaPlayerHolder
                    )
                } else {
                    onCloseActivity()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (mFragmentToRestore != 0) {
            super.onSaveInstanceState(outState)
            outState.putInt(
                GoConstants.FRAGMENT_TO_RESTORE,
                mFragmentToRestore
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (goPreferences.isCovers) {
            mLoadCoverJob.cancel()
        }
        mMusicViewModel.cancel()
        if (isMediaPlayerHolder && !mMediaPlayerHolder.isPlaying && ::mPlayerService.isInitialized && mPlayerService.isRunning && !mMediaPlayerHolder.isSongRestoredFromPrefs) {
            mPlayerService.stopForeground(true)
            stopService(mBindingIntent)
        }
        if (sBound) {
            unbindService(connection)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isMediaPlayerHolder && mMediaPlayerHolder.isMediaPlayer && !mMediaPlayerHolder.isSongRestoredFromPrefs) {
            mMediaPlayerHolder.onRestartSeekBarCallback()
        }
    }

    // Pause SeekBar callback
    override fun onPause() {
        super.onPause()
        if (isMediaPlayerHolder && mMediaPlayerHolder.isMediaPlayer && !mMediaPlayerHolder.isSongRestoredFromPrefs) {
            saveSongToPref()
            mMediaPlayerHolder.run {
                onPauseSeekBarCallback()
                if (!isPlaying) {
                    giveUpAudioFocus()
                }
            }
        }
    }

    // Manage request permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            GoConstants.PERMISSION_REQUEST_READ_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted, yay! Do bind service
                    doBindService()
                } else {
                    // Permission denied, boo! Error!
                    notifyError(GoConstants.TAG_NO_PERMISSION)
                }
            }
        }
    }

    override fun onDenyPermission() {
        notifyError(GoConstants.TAG_NO_PERMISSION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(ThemeHelper.getAccentedTheme().first)

        mMainActivityBinding = MainActivityBinding.inflate(layoutInflater)
        mPlayerControlsPanelBinding = PlayerControlsPanelBinding.bind(mMainActivityBinding.root)

        setContentView(mMainActivityBinding.root)

        if (VersioningHelper.isOreoMR1()) {
            edgeToEdge {
                mMainActivityBinding.root.fit { Edge.Top + Edge.Bottom }
            }
        }

        initMediaButtons()

        mFragmentToRestore =
            savedInstanceState?.getInt(GoConstants.FRAGMENT_TO_RESTORE)
                ?: intent.getIntExtra(
                    GoConstants.FRAGMENT_TO_RESTORE,
                    0
                )

        if (PermissionsHelper.hasToAskForReadStoragePermission(this)) {
            PermissionsHelper.manageAskForReadStoragePermission(
                activity = this, uiControlInterface = this
            )
        } else {
            doBindService()
        }
    }

    private fun notifyError(errorType: String) {
        mPlayerControlsPanelBinding.playerView.handleViewVisibility(false)
        mMainActivityBinding.loadingProgressBar.handleViewVisibility(false)
        mMainActivityBinding.viewPager2.handleViewVisibility(false)
        supportFragmentManager.addFragment(
            ErrorFragment.newInstance(errorType),
            GoConstants.ERROR_FRAGMENT_TAG
        )
    }

    private fun finishSetup(music: MutableList<Music>?) {

        if (!music.isNullOrEmpty()) {

            mMainActivityBinding.loadingProgressBar.handleViewVisibility(false)

            synchronized(initViewPager()) {
                val fragmentsIterator = mActiveFragments.iterator().withIndex()
                while (fragmentsIterator.hasNext()) {
                    val fragmentIndex = fragmentsIterator.next().value
                    val fragment = getFragmentForIndex(fragmentIndex)
                    if (fragment is MusicContainersListFragment && !mMusicContainersFragments.contains(
                            fragment
                        )
                    ) {
                        mMusicContainersFragments.add(fragment)
                    }
                }
            }

            synchronized(handleRestore()) {
                mPlayerControlsPanelBinding.playerView.animate().apply {
                    duration = 500
                    alpha(1.0F)
                }
            }
        } else {
            notifyError(GoConstants.TAG_NO_MUSIC)
        }
    }

    // Handle restoring: handle intent data, if any, or restore playback
    private fun handleRestore() {
        if (intent != null && Intent.ACTION_VIEW == intent.action && intent.data != null) {
            handleIntent(
                intent
            )
        } else {
            restorePlayerStatus()
        }
    }

    private fun initViewPager() {

        val pagerAdapter = ScreenSlidePagerAdapter(this)
        mMainActivityBinding.viewPager2.offscreenPageLimit = mActiveFragments.size.minus(1)
        mMainActivityBinding.viewPager2.adapter = pagerAdapter

        val resolvedAlphaAccentColor = ThemeHelper.getAlphaAccent(this)

        mPlayerControlsPanelBinding.tabLayout.run {

            tabIconTint = ColorStateList.valueOf(resolvedAlphaAccentColor)

            TabLayoutMediator(this, mMainActivityBinding.viewPager2) { tab, position ->
                val fragmentIndex = mActiveFragments[position]
                tab.setIcon(ThemeHelper.getTabIcon(fragmentIndex))
                initFragmentAt(fragmentIndex)
            }.attach()

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    tab.icon?.setTint(ThemeHelper.resolveThemeAccent(this@MainActivity))
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    closeFragments()
                    tab.icon?.setTint(resolvedAlphaAccentColor)
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    closeFragments()
                }
            })

            getTabAt(mFragmentToRestore)?.icon?.setTint(
                ThemeHelper.resolveThemeAccent(this@MainActivity)
            )
        }

        if (mFragmentToRestore != 0) {
            mMainActivityBinding.viewPager2.setCurrentItem(
                mFragmentToRestore,
                false
            )
        }
    }

    private fun initFragmentAt(fragmentIndex: String) {
        when (fragmentIndex) {
            GoConstants.ARTISTS_TAB -> if (mArtistsFragment == null) {
                mArtistsFragment =
                    MusicContainersListFragment.newInstance(GoConstants.ARTIST_VIEW)
            }
            GoConstants.ALBUM_TAB -> if (mAlbumsFragment == null) {
                mAlbumsFragment =
                    MusicContainersListFragment.newInstance(GoConstants.ALBUM_VIEW)
            }
            GoConstants.SONGS_TAB -> if (mAllMusicFragment == null) {
                mAllMusicFragment =
                    AllMusicFragment.newInstance()
            }
            GoConstants.FOLDERS_TAB -> if (mFoldersFragment == null) {
                mFoldersFragment =
                    MusicContainersListFragment.newInstance(GoConstants.FOLDER_VIEW)
            }
            else -> if (mSettingsFragment == null) {
                mSettingsFragment =
                    SettingsFragment.newInstance()
            }
        }
    }

    private fun handleOnNavigationItemSelected(itemId: Int) = when (itemId) {
        0 -> getFragmentForIndex(mActiveFragments[0])
        1 -> getFragmentForIndex(mActiveFragments[1])
        2 -> getFragmentForIndex(mActiveFragments[2])
        3 -> getFragmentForIndex(mActiveFragments[3])
        else -> getFragmentForIndex(mActiveFragments[4])
    }

    private fun getFragmentForIndex(index: String) = when (index) {
        GoConstants.ARTISTS_TAB -> mArtistsFragment
        GoConstants.ALBUM_TAB -> mAlbumsFragment
        GoConstants.SONGS_TAB -> mAllMusicFragment
        GoConstants.FOLDERS_TAB -> mFoldersFragment
        else -> mSettingsFragment
    }

    private fun closeFragments() {
        supportFragmentManager.run {
            goBackFromFragment(sEqFragmentExpanded)
            goBackFromFragment(sDetailsFragmentExpanded)
        }
    }

    private fun openDetailsFragment(
        selectedArtistOrFolder: String?,
        launchedBy: String,
        isShuffleMode: Pair<Boolean, String?>
    ) {
        if (!sDetailsFragmentExpanded) {
            mDetailsFragment =
                DetailsFragment.newInstance(
                    selectedArtistOrFolder,
                    launchedBy,
                    MusicOrgHelper.getPlayingAlbumPosition(
                        selectedArtistOrFolder,
                        mMediaPlayerHolder,
                        mMusicViewModel.deviceAlbumsByArtist
                    ),
                    isShuffleMode
                )
            sCloseDetailsFragment = true
            supportFragmentManager.addFragment(
                mDetailsFragment,
                GoConstants.DETAILS_FRAGMENT_TAG
            )
        }
    }

    private fun closeDetailsFragment() {
        if (!sRevealAnimationRunning) {
            mDetailsFragment.onHandleBackPressed().apply {
                sRevealAnimationRunning = true
                doOnEnd {
                    synchronized(supportFragmentManager.goBackFromFragment(true)) {
                        sRevealAnimationRunning = false
                    }
                }
            }
        }
    }

    private fun closeNowPlayingDialog() {
        if (::mNowPlayingDialog.isInitialized && mNowPlayingDialog.isShowing) {
            mNowPlayingDialog.dismiss()
        }
    }

    private fun initMediaButtons() {

        mPlayerControlsPanelBinding.playPauseButton.setOnClickListener { resumeOrPause() }

        mPlayerControlsPanelBinding.queueButton.run {
            setOnClickListener { openQueueDialog() }
            setOnLongClickListener {
                if (checkIsPlayer(true) && mMediaPlayerHolder.isQueue) {
                    DialogHelper.showClearQueueDialog(
                        this@MainActivity,
                        mMediaPlayerHolder
                    )
                }
                return@setOnLongClickListener true
            }
        }

        mPlayerControlsPanelBinding.lovedSongsButton.run {
            setOnClickListener { openLovedSongsDialog() }
            setOnLongClickListener {
                if (!goPreferences.lovedSongs.isNullOrEmpty()) {
                    DialogHelper.showClearLovedSongDialog(
                        this@MainActivity,
                        this@MainActivity
                    )
                }
                return@setOnLongClickListener true
            }
        }

        onLovedSongsUpdate(false)

        mPlayerControlsPanelBinding.playingSongContainer.run {
            setOnClickListener { openNowPlaying() }
            setOnLongClickListener {
                if (!sDetailsFragmentExpanded || sDetailsFragmentExpanded and !sEqFragmentExpanded) {
                    openPlayingArtistAlbum()
                }
                return@setOnLongClickListener true
            }
        }
    }

    private fun setSeekBarProgressListener() {

        mNowPlayingBinding.npSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                val defaultPositionColor = mNowPlayingBinding.npSeek.currentTextColor
                val selectedColor = ThemeHelper.resolveThemeAccent(this@MainActivity)
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
                            selectedColor
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
                        mPlayerControlsPanelBinding.songProgress.progress = userSelectedPosition
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

    private fun setupNowPlayingWithCovers() {
        if (goPreferences.isCovers) {
            if (!ThemeHelper.isDeviceLand(resources)) {
                mNowPlayingBinding.npArtistAlbum.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
                mNowPlayingBinding.npSong.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
            }
        } else {
            mNowPlayingBinding.npCover.handleViewVisibility(false)
        }
    }

    private fun setupPreciseVolumeHandler() {

        mMediaPlayerHolder.currentVolumeInPercent.run {
            mNowPlayingExtendedControlsBinding.npVolume.setImageResource(
                ThemeHelper.getPreciseVolumeIcon(
                    this
                )
            )
            mNowPlayingExtendedControlsBinding.npVolumeSeek.progress = this
        }

        val resolvedIconsColor = ContextCompat.getColor(this, R.color.widgetsColor)

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
                        ThemeHelper.resolveThemeAccent(this@MainActivity)
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
                        resolvedIconsColor
                )
            }
        })
    }

    private fun openNowPlaying() {

        if (checkIsPlayer(true) && mMediaPlayerHolder.isCurrentSong) {

            mNowPlayingDialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

                customView(R.layout.now_playing)

                mNowPlayingBinding = NowPlayingBinding.bind(getCustomView())
                mNowPlayingControlsBinding = NowPlayingControlsBinding.bind(mNowPlayingBinding.root)
                mNowPlayingExtendedControlsBinding =
                    NowPlayingExtendedControlsBinding.bind(mNowPlayingBinding.root)

                mNowPlayingBinding.npSong.isSelected = true
                mNowPlayingBinding.npArtistAlbum.isSelected = true

                mNowPlayingBinding.npPlayingSongContainer.setOnClickListener { openPlayingArtistAlbum() }

                mNowPlayingControlsBinding.npActions.setOnClickListener { equalizerButton ->
                    val popup = PopupMenu(this@MainActivity, equalizerButton)
                    popup.menuInflater.inflate(R.menu.menu_now_playing_actions, popup.menu)

                    popup.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.equalizer -> openEqualizer()
                            R.id.savePlayerPosition -> {
                                savePlayerPosition()
                            }
                        }
                        true
                    }
                    popup.show()
                }

                mNowPlayingControlsBinding.npSkipPrev.run {
                    setOnClickListener { skip(false) }
                    setOnLongClickListener {
                        fastSeek(false)
                        return@setOnLongClickListener true
                    }
                }

                mNowPlayingControlsBinding.npPlay.setOnClickListener { resumeOrPause() }

                mNowPlayingControlsBinding.npSkipNext.run {
                    setOnClickListener { skip(true) }
                    setOnLongClickListener {
                        fastSeek(true)
                        return@setOnLongClickListener true
                    }
                }

                val accent = ThemeHelper.resolveThemeAccent(this@MainActivity)
                val gradientColorStart = ColorUtils.blendARGB(ContextCompat.getColor(this@MainActivity, R.color.seekBarBgColorLight), accent, 0.35F)
                val gradientColorEnd = ColorUtils.blendARGB(ContextCompat.getColor(this@MainActivity, R.color.seekBarBgColorDark), accent, 0.25F)

                mNowPlayingBinding.npSeekBar.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(gradientColorStart, gradientColorEnd))

                setupNowPlayingWithCovers()

                mNowPlayingControlsBinding.npRepeat.run {
                    setImageResource(
                            ThemeHelper.getRepeatIcon(
                                mMediaPlayerHolder
                            )
                    )
                    ThemeHelper.updateIconTint(
                        this,
                        if (mMediaPlayerHolder.isRepeat1X || mMediaPlayerHolder.isLooping) {
                            ThemeHelper.resolveThemeAccent(this@MainActivity)
                        } else {
                            ContextCompat.getColor(this@MainActivity, R.color.widgetsColor)
                        }
                    )
                    setOnClickListener { setRepeat() }
                }

                mNowPlayingExtendedControlsBinding.npLove.setOnClickListener {
                    ListsHelper.addOrRemoveFromLovedSongs(mMediaPlayerHolder.currentSong.first,
                            0, mMediaPlayerHolder.launchedBy)
                    onLovedSongsUpdate(false)
                    updateNowPlayingLovedIcon(context)
                }

                if (goPreferences.isPreciseVolumeEnabled) {
                    setupPreciseVolumeHandler()
                } else {
                    mNowPlayingExtendedControlsBinding.npVolumeSeek.isEnabled = false
                    ThemeHelper.updateIconTint(
                        mNowPlayingExtendedControlsBinding.npVolume,
                        ThemeHelper.resolveColorAttr(
                            this@MainActivity,
                            android.R.attr.colorButtonNormal
                        )
                    )
                }

                setSeekBarProgressListener()

                updateNowPlayingInfo()

                onShow {
                    mMediaPlayerHolder.currentSong.first?.let { song ->
                        if (goPreferences.isCovers) {
                            loadNowPlayingCover(song)
                        }
                        mNowPlayingBinding.npSeek.text =
                            song.startFrom.toLong()
                                .toFormattedDuration(false, isSeekBar = true)
                    }

                    mNowPlayingBinding.npSeekBar.progress =
                        mPlayerControlsPanelBinding.songProgress.progress
                }

                onDismiss {
                    mNowPlayingBinding.npSeekBar.setOnSeekBarChangeListener(null)
                }

                if (VersioningHelper.isOreoMR1() && !ThemeHelper.isDeviceLand(resources)) {
                    edgeToEdge {
                        mNowPlayingBinding.root.fit { Edge.Bottom }
                    }
                }

                // to ensure full dialog's height
                getCustomView().afterMeasured {
                    mNowPlayingDialog.setPeekHeight(height)
                }
            }
        }
    }

    private fun savePlayerPosition(){
        val song = mMediaPlayerHolder.currentSong.first
        val position = mMediaPlayerHolder.playerPosition
        if(position > 0) {
            ListsHelper.addToLovedSongs(song, mMediaPlayerHolder.playerPosition, mMediaPlayerHolder.launchedBy)
            onLovedSongAdded(song, true)
            this.getString(
                    R.string.loved_song_added,
                    song?.title,
                    mMediaPlayerHolder.playerPosition.toLong().toFormattedDuration(
                            isAlbum = false,
                            isSeekBar = false
                    )
            ).toToast(this)
        }else {
            this.getString(R.string.cannot_save_position).toToast(this)
        }
    }


    private fun updateNowPlayingLovedIcon(context: Context) {
        if (this::mNowPlayingExtendedControlsBinding.isInitialized) {
            mMediaPlayerHolder.currentSong.first.let {
                val isLoved = isInLovedSongs(it!!)
                val lovedSongsButtonColor = if (isLoved)
                    ThemeHelper.resolveThemeAccent(context) else ThemeHelper.resolveColorAttr(
                        context,
                        android.R.attr.colorButtonNormal
                )
                ThemeHelper.updateIconTint(
                        mNowPlayingExtendedControlsBinding.npLove,
                        lovedSongsButtonColor
                )
            }
        }
    }

    private fun isInLovedSongs(song: Music?): Boolean {
        val lovedSongs = goPreferences.lovedSongs
        if (lovedSongs != null && song != null) {
            val convertedSong =
                    song.toSavedMusic(0, mMediaPlayerHolder.launchedBy)
            return lovedSongs.contains(convertedSong)
        }
        return false
    }

    private fun saveSongToPref() {
        if (::mMediaPlayerHolder.isInitialized && !mMediaPlayerHolder.isPlaying || mMediaPlayerHolder.state == GoConstants.PAUSED) {
            mMediaPlayerHolder.run {
                MusicOrgHelper.saveLatestSong(currentSong.first, this, launchedBy)
            }
        }
    }

    override fun onAppearanceChanged(isThemeChanged: Boolean) {
        mFragmentToRestore = mMainActivityBinding.viewPager2.currentItem
        synchronized(saveSongToPref()) {
            if (isThemeChanged) {
                AppCompatDelegate.setDefaultNightMode(
                    ThemeHelper.getDefaultNightMode(
                        this
                    )
                )
            } else {
                ThemeHelper.applyChanges(this, mFragmentToRestore)
            }
        }
    }

    override fun onPreciseVolumeToggled() {
        //avoid having user stuck at lowered volume without knowing why
        mMediaPlayerHolder.setPreciseVolume(100)
    }

    private fun updatePlayingStatus(isNowPlaying: Boolean) {
        val isPlaying = mMediaPlayerHolder.state != GoConstants.PAUSED
        val drawable =
            if (isPlaying) {
                R.drawable.ic_pause
            } else {
                R.drawable.ic_play
            }
        if (isNowPlaying) {
            mNowPlayingControlsBinding.npPlay.setImageResource(drawable)
        } else {
            mPlayerControlsPanelBinding.playPauseButton.setImageResource(
                drawable
            )
        }
    }

    override fun onLovedSongAdded(song: Music?, isAdded: Boolean) {
        if (checkIsPlayer(true)) {
            mMediaPlayerHolder.run {
                if (queueSongs.isNotEmpty() && isLovedSongsQueued) {
                    song?.let { songToProcess ->
                        if (isAdded) {
                            queueSongs.add(songToProcess)
                        } else {
                            queueSongs.remove(songToProcess)
                        }
                    }

                }
            }
        }
        onLovedSongsUpdate(false)
        updateNowPlayingLovedIcon(this@MainActivity)
    }

    override fun onLovedSongsUpdate(clear: Boolean) {

        val lovedSongs = goPreferences.lovedSongs?.toMutableList()

        if (clear) {
            lovedSongs?.clear()
            goPreferences.lovedSongs = lovedSongs
        }

        val lovedSongsButtonColor = if (lovedSongs.isNullOrEmpty()) {
            ThemeHelper.resolveColorAttr(
                this,
                android.R.attr.colorButtonNormal
            )
        } else {
            ContextCompat.getColor(this, R.color.red)
        }
        ThemeHelper.updateIconTint(
            mPlayerControlsPanelBinding.lovedSongsButton,
            lovedSongsButtonColor
        )
    }

    private fun restorePlayerStatus() {
        if (isMediaPlayerHolder) {
            // If we are playing and the activity was restarted
            // update the controls panel
            mMediaPlayerHolder.run {

                if (isMediaPlayer && isPlaying) {

                    onRestartSeekBarCallback()
                    updatePlayingInfo(true)

                } else {

                    isSongRestoredFromPrefs = goPreferences.latestPlayedSong != null

                    val song =
                        if (isSongRestoredFromPrefs) {
                            checkIfSongIsAvailable(goPreferences.latestPlayedSong)
                        } else {
                            mMusicViewModel.randomMusic
                        }

                    val songs = MusicOrgHelper.getAlbumSongs(
                        song?.artist,
                        song?.album,
                        mMusicViewModel.deviceAlbumsByArtist
                    )

                    if (!songs.isNullOrEmpty()) {
                        isPlay = false

                        startPlayback(
                            song,
                            songs,
                            getLatestSongLaunchedBy()
                        )

                        updatePlayingInfo(false)

                        mPlayerControlsPanelBinding.songProgress.progress = song?.startFrom!!
                    } else {
                        notifyError(GoConstants.TAG_SD_NOT_READY)
                    }
                }
                onUpdateDefaultAlbumArt(
                    getDrawable(R.drawable.album_art)?.toBitmap()
                )
            }
        }
    }

    private fun getLatestSongLaunchedBy() = goPreferences.latestPlayedSong?.launchedBy
        ?: GoConstants.ARTIST_VIEW

    // method to update info on controls panel
    private fun updatePlayingInfo(restore: Boolean) {

        val selectedSong = mMediaPlayerHolder.currentSong.first

        mPlayerControlsPanelBinding.songProgress.progress = 0
        mPlayerControlsPanelBinding.songProgress.max = selectedSong?.duration!!.toInt()

        mPlayerControlsPanelBinding.playingSong.text = selectedSong.title

        mPlayerControlsPanelBinding.playingArtist.text =
            getString(
                R.string.artist_and_album,
                selectedSong.artist,
                selectedSong.album
            )

        updateRepeatStatus(false)

        if (isNowPlaying) {
            updateNowPlayingInfo()
        }

        if (restore) {

            if (!mMediaPlayerHolder.queueSongs.isNullOrEmpty() && !mMediaPlayerHolder.isQueueStarted) {
                mMediaPlayerInterface.onQueueEnabled()
            } else {
                mMediaPlayerInterface.onQueueStartedOrEnded(mMediaPlayerHolder.isQueueStarted)
            }

            updatePlayingStatus(false)

            if (::mPlayerService.isInitialized) {
                //stop foreground if coming from pause state
                mPlayerService.run {
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
        val resolvedIconsColor = ContextCompat.getColor(this, R.color.widgetsColor)
        if (isNowPlaying) {
            mNowPlayingControlsBinding.npRepeat.setImageResource(
                ThemeHelper.getRepeatIcon(
                    mMediaPlayerHolder
                )
            )
            when {
                onPlaybackCompletion -> ThemeHelper.updateIconTint(
                    mNowPlayingControlsBinding.npRepeat,
                    resolvedIconsColor
                )
                mMediaPlayerHolder.isRepeat1X or mMediaPlayerHolder.isLooping -> {
                    ThemeHelper.updateIconTint(
                        mNowPlayingControlsBinding.npRepeat,
                        ThemeHelper.resolveThemeAccent(this)
                    )
                }
                else -> ThemeHelper.updateIconTint(
                    mNowPlayingControlsBinding.npRepeat,
                    resolvedIconsColor
                )
            }
        }
    }

    private fun checkIfSongIsAvailable(song: Music?) : Music? = try {
        if (mMusicViewModel.deviceMusicFiltered?.savedSongIsAvailable(song) == null) {
            saveRandomSongToPrefs()
        } else {
            song
        }
    } catch (e: Exception) {
        e.printStackTrace()
        saveRandomSongToPrefs()
    }

    private fun saveRandomSongToPrefs() : Music? {
        val randomMusic = mMusicViewModel.randomMusic
        goPreferences.latestPlayedSong = randomMusic
        return randomMusic
    }

    private fun loadNowPlayingCover(selectedSong: Music) {
        if (!::mImageLoader.isInitialized) {
            mImageLoader = ImageLoader.Builder(this)
                    .bitmapPoolingEnabled(false)
                    .crossfade(true)
                    .build()
        }

        mSelectedArtistAlbumForNP = Pair(selectedSong.artist, selectedSong.albumId)
        val request = ImageRequest.Builder(this)
                .data(selectedSong.albumId?.getCoverFromPFD(this) ?: getDrawable(R.drawable.album_art)?.toBitmap())
                .target(
                        onSuccess = { result ->
                            // Handle the successful result.
                            mNowPlayingBinding.npCover.load(result) {
                                transformations(RoundedCornersTransformation(16.0F))
                            }
                        }
                )
                .build()

        mLoadCoverIoScope.launch {
            withContext(mLoadCoverIoDispatcher) {
                mImageLoader.enqueue(request)
            }
        }
    }

    private fun updateNowPlayingInfo() {

        mMediaPlayerHolder.currentSong.first?.let { song ->
            val selectedSongDuration = song.duration
            if (mSelectedArtistAlbumForNP != Pair(
                    song.artist,
                    song.albumId
                ) && goPreferences.isCovers && ::mNowPlayingDialog.isInitialized && mNowPlayingDialog.isShowing
            ) {
                loadNowPlayingCover(song)
            }

            mNowPlayingBinding.npSong.text = song.title

            mNowPlayingBinding.npArtistAlbum.text =
                getString(
                    R.string.artist_and_album,
                    song.artist,
                    song.album
                )

            mNowPlayingBinding.npDuration.text =
                selectedSongDuration.toFormattedDuration(false, isSeekBar = true)

            mNowPlayingBinding.npSeekBar.max = song.duration.toInt()

            song.id?.toContentUri()?.toBitrate(this)?.let { (first, second) ->
                mNowPlayingBinding.npRates.text =
                        getString(R.string.rates, first, second)
            }
        }
        updateNowPlayingLovedIcon(this)
        updatePlayingStatus(true)
    }

    private fun getSongSource(selectedSong: Music?, launchedBy: String): String? {
        return when (launchedBy) {
            GoConstants.FOLDER_VIEW -> selectedSong?.relativePath
            GoConstants.ARTIST_VIEW -> selectedSong?.artist
            else -> selectedSong?.album
        }
    }

    private fun openPlayingArtistAlbum() {
        if (isMediaPlayerHolder && mMediaPlayerHolder.isCurrentSong) {
            val isPlayingFromFolder = mMediaPlayerHolder.launchedBy
            val selectedSong = mMediaPlayerHolder.currentSong.first
            val selectedArtistOrFolder = getSongSource(selectedSong, isPlayingFromFolder)
            if (sDetailsFragmentExpanded) {
                if (mDetailsFragment.hasToUpdate(selectedArtistOrFolder)) {
                    synchronized(super.onBackPressed()) {
                        openDetailsFragment(
                            selectedArtistOrFolder,
                            mMediaPlayerHolder.launchedBy,
                            Pair(
                                checkIsPlayer(false) && mMediaPlayerHolder.isShuffledSongsQueued.first && mMediaPlayerHolder.currentSong.first?.artist == selectedArtistOrFolder && mMediaPlayerHolder.launchedBy != GoConstants.ALBUM_VIEW && mMediaPlayerHolder.launchedBy != GoConstants.FOLDER_VIEW,
                                mMediaPlayerHolder.isShuffledSongsQueued.second
                            )
                        )
                    }
                } else {
                    mDetailsFragment.tryToSnapToAlbumPosition(
                        MusicOrgHelper.getPlayingAlbumPosition(
                            selectedArtistOrFolder,
                            mMediaPlayerHolder,
                            mMusicViewModel.deviceAlbumsByArtist
                        )
                    )
                }
            } else {
                openDetailsFragment(
                    selectedArtistOrFolder,
                    mMediaPlayerHolder.launchedBy,
                    Pair(
                        checkIsPlayer(false) && mMediaPlayerHolder.isShuffledSongsQueued.first && mMediaPlayerHolder.currentSong.first?.artist == selectedArtistOrFolder && mMediaPlayerHolder.launchedBy != GoConstants.ALBUM_VIEW && mMediaPlayerHolder.launchedBy != GoConstants.FOLDER_VIEW,
                        mMediaPlayerHolder.isShuffledSongsQueued.second
                    )
                )
            }

            closeNowPlayingDialog()
        }
    }

    override fun onCloseActivity() {
        if (isMediaPlayerHolder && mMediaPlayerHolder.isPlaying) {
            DialogHelper.stopPlaybackDialog(
                this,
                mMediaPlayerHolder
            )
        } else {
            finishAndRemoveTask()
        }
    }

    override fun onHandleFocusPref() {
        if (isMediaPlayerHolder) {
            if (isMediaPlayerHolder && mMediaPlayerHolder.isMediaPlayer) {
                if (goPreferences.isFocusEnabled) {
                    mMediaPlayerHolder.tryToGetAudioFocus()
                } else {
                    mMediaPlayerHolder.giveUpAudioFocus()
                }
            }
        }
    }

    override fun onHandleNotificationUpdate(isAdditionalActionsChanged: Boolean) {
        if (isMediaPlayerHolder) {
            mMediaPlayerHolder.onHandleNotificationUpdate(isAdditionalActionsChanged)
        }
    }

    override fun onArtistOrFolderSelected(artistOrFolder: String, launchedBy: String) {
        openDetailsFragment(
            artistOrFolder,
            launchedBy,
            Pair(
                checkIsPlayer(false) && mMediaPlayerHolder.isShuffledSongsQueued.first && mMediaPlayerHolder.currentSong.first?.artist == artistOrFolder && mMediaPlayerHolder.launchedBy != GoConstants.ALBUM_VIEW && mMediaPlayerHolder.launchedBy != GoConstants.FOLDER_VIEW,
                mMediaPlayerHolder.isShuffledSongsQueued.second
            )
        )
    }

    private fun startPlayback(song: Music?, songs: List<Music>?, launchedBy: String) {
        if (isMediaPlayerHolder) {
            if (::mPlayerService.isInitialized && !mPlayerService.isRunning) {
                startService(
                    mBindingIntent
                )
            }
            mMediaPlayerHolder.run {
                setCurrentSong(song, songs, isFromQueue = false, songLaunchedBy = launchedBy)
                initMediaPlayer(song)
            }
        }
    }

    override fun onSongSelected(song: Music?, songs: List<Music>?, launchedBy: String) {
        if (isMediaPlayerHolder) {
            mMediaPlayerHolder.run {
                isSongRestoredFromPrefs = false
                isPlay = true
                if (isQueue) {
                    setQueueEnabled(false)
                }
                val selectedSongs = if (sDetailsFragmentExpanded) {
                    mDetailsFragment.onDisableShuffle(isShuffleMode = false, isMusicListOutputRequired = true)
                } else {
                    songs
                }
                startPlayback(song, selectedSongs, launchedBy)
            }
        }
    }

    private fun resumeOrPause() {
        if (checkIsPlayer(true)) {
            mMediaPlayerHolder.resumeOrPause()
        }
    }

    private fun fastSeek(isForward: Boolean) {
        if (checkIsPlayer(true)) {
            mMediaPlayerHolder.fastSeek(isForward)
        }
    }

    private fun skip(isNext: Boolean) {
        if (checkIsPlayer(true)) {
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
    }

    private fun setRepeat() {
        if (checkIsPlayer(true)) {
            mMediaPlayerHolder.repeat(mMediaPlayerHolder.isPlaying)
            updateRepeatStatus(false)
        }
    }

    override fun onGetEqualizer(): Triple<Equalizer?, BassBoost?, Virtualizer?> =
        mMediaPlayerHolder.getEqualizer()

    override fun onEnableEqualizer(isEnabled: Boolean) {
        if (::mMediaPlayerHolder.isInitialized) {
            mMediaPlayerHolder.setEqualizerEnabled(isEnabled)
        }
    }

    override fun onSaveEqualizerSettings(
        selectedPreset: Int,
        bassBoost: Short,
        virtualizer: Short
    ) {
        mMediaPlayerHolder.onSaveEqualizerSettings(selectedPreset, bassBoost, virtualizer)
    }

    private fun openEqualizer() {
        if (checkIsPlayer(true)) {
            if (!EqualizerUtils.hasEqualizer(this)) {
                synchronized(mMediaPlayerHolder.onOpenEqualizerCustom()) {
                    if (!sEqFragmentExpanded) {
                        mEqualizerFragment = EqFragment.newInstance()
                        synchronized(closeNowPlayingDialog()) {
                            sCloseDetailsFragment = !sDetailsFragmentExpanded
                            supportFragmentManager.addFragment(
                                mEqualizerFragment,
                                GoConstants.EQ_FRAGMENT_TAG
                            )
                        }
                    }
                }
            } else {
                mMediaPlayerHolder.openEqualizer(this)
            }
        }
    }

    private fun closeEqualizerFragment() {
        if (!sRevealAnimationRunning) {
            mEqualizerFragment.onHandleBackPressed().run {
                sRevealAnimationRunning = true
                doOnEnd {
                    synchronized(supportFragmentManager.goBackFromFragment(true)) {
                        sRevealAnimationRunning = false
                    }
                }
            }
        }
    }

    override fun onAddToQueue(song: Music?, launchedBy: String) {
        if (checkIsPlayer(true)) {
            mMediaPlayerHolder.run {
                if (queueSongs.isEmpty()) {
                    setQueueEnabled(true)
                }
                song?.let { songToQueue ->
                    if (!queueSongs.contains(songToQueue)) {
                        queueSongs.add(songToQueue)

                        if (!isPlaying || state == GoConstants.PAUSED) {
                            startSongFromQueue(song, launchedBy)
                        }

                        getString(
                                R.string.queue_song_add,
                                songToQueue.title
                        ).toToast(this@MainActivity)
                    }
                }
            }
        }
    }

    override fun onAddAlbumToQueue(
        songs: MutableList<Music>?,
        isAlbumOrFolder: Pair<Boolean, Music?>,
        isLovedSongs: Boolean,
        isShuffleMode: Boolean,
        clearShuffleMode: Boolean,
        launchedBy: String
    ) {
        if (checkIsPlayer(true)) {
            if (isShuffleMode) {
                mMediaPlayerHolder.isLovedSongsQueued = false
            }
            val wasShuffleMode = mMediaPlayerHolder.isShuffledSongsQueued

            mMediaPlayerHolder.run {
                if (queueSongs.isEmpty()) {
                    setQueueEnabled(true)
                }
                if (isShuffleMode && clearShuffleMode || wasShuffleMode.first && clearShuffleMode || !isAlbumOrFolder.first && clearShuffleMode) {
                    queueSongs.clear()
                }
                songs?.let { songsToQueue ->
                    if (queueSongs.isEmpty()) {
                        queueSongs.addAll(songsToQueue)
                    } else {
                        // don't add duplicates
                        val filteredSongs = songsToQueue.minus(queueSongs)
                        addFilteredSongsToQueue(this, filteredSongs)
                    }
                }
                if (isLovedSongs && ::mLovedSongsDialog.isInitialized && mLovedSongsDialog.isShowing) {
                    mLovedSongsDialog.dismiss()
                    mLovedSongsDialog.onDismiss {
                        openQueueDialog()
                    }
                }
                isLovedSongsQueued = isLovedSongs
                if (clearShuffleMode && !wasShuffleMode.first || isLovedSongsQueued) {
                    if (isLovedSongsQueued) {
                        restoreShuffledSongs()
                    }
                    if (sDetailsFragmentExpanded) {
                        mDetailsFragment.onDisableShuffle(isShuffleMode = false, isMusicListOutputRequired = false)
                    }
                }

                if (!isPlaying || isLovedSongs || clearShuffleMode && !isAlbumOrFolder.first) {
                    isAlbumOrFolder.second?.let { song ->
                        startSongFromQueue(song, launchedBy)
                    }
                }
            }
        }
    }

    private fun addFilteredSongsToQueue(
        mediaPlayerHolder: MediaPlayerHolder,
        filteredSongs: List<Music>
    ) {
        // don't add duplicates
        if (filteredSongs.isNotEmpty()) {
            mediaPlayerHolder.queueSongs.addAll(filteredSongs)
        }
    }

    override fun onShuffleSongs(
        albumTitle: String?,
        artistAlbums: List<Album>?,
        songs: MutableList<Music>?,
        toBeQueued: Boolean,
        launchedBy: String
    ): MutableList<Music>? {
        return songs?.apply {
            if (checkIsPlayer(true)) {
                shuffle()
                val song = get(0)
                mMediaPlayerHolder.run {
                    if (isLovedSongsQueued) {
                        isLovedSongsQueued = false
                    }
                    isShuffledSongsQueued = Pair(true, albumTitle)
                    albumsForShuffleMode = artistAlbums
                    if (toBeQueued) {
                        onAddAlbumToQueue(
                            songs,
                            Pair(false, song),
                            isLovedSongs = false,
                            isShuffleMode = isShuffledSongsQueued.first,
                            clearShuffleMode = true,
                            launchedBy
                        )
                    } else {
                        onSongSelected(song, songs, launchedBy)
                    }
                }
            }
        }
    }

    override fun onSongVisualizationChanged() {
        if (mAllMusicFragment != null && !mAllMusicFragment?.onSongVisualizationChanged()!!) {
            ThemeHelper.applyChanges(this, mMainActivityBinding.viewPager2.currentItem)
        }
    }

    override fun onAddToFilter(stringToFilter: String?) {
        if (mMusicViewModel.deviceMusicFiltered?.size == 1) {
            getString(R.string.error_eq).toToast(this)
        } else {
            stringToFilter?.let { string ->

                ListsHelper.addToHiddenItems(string)

                if (!mMusicContainersFragments.isNullOrEmpty() && !mMusicContainersFragments[0].onListFiltered(string)) {
                ThemeHelper.applyChanges(this, mMainActivityBinding.viewPager2.currentItem)
                } else {
                    if (!mMusicContainersFragments.isNullOrEmpty() && mMusicContainersFragments.size >= 1) {
                        val musicContainersIterator = mMusicContainersFragments.iterator().withIndex()
                        while (musicContainersIterator.hasNext()) {
                            val item = musicContainersIterator.next()
                            if (item.index != 0) {
                                item.value.onListFiltered(string)
                            }
                        }
                    }

                    mMusicViewModel.deviceMusicFiltered = mMusicViewModel.deviceMusicFiltered?.filter { !it.artist.equals(string) and !it.album.equals(string) and !it.relativePath.equals(string) }?.toMutableList()

                    mAllMusicFragment?.onListFiltered(mMusicViewModel.deviceMusicFiltered)
                    mSettingsFragment?.onFiltersChanged(mMusicViewModel.deviceMusicFiltered?.size!!)

                    // be sure to update queue, loved songs and the controls panel
                    if (isMediaPlayerHolder) {
                        MusicOrgHelper.updateMediaPlayerHolderLists(mMediaPlayerHolder, this, mMusicViewModel.randomMusic)?.run {
                            val songs = MusicOrgHelper.getAlbumSongs(
                                artist,
                                album,
                                mMusicViewModel.deviceAlbumsByArtist
                            )
                            mMediaPlayerHolder.isPlay = mMediaPlayerHolder.isPlaying
                            mMediaPlayerHolder.setCurrentSong(this, songs, isFromQueue = false, songLaunchedBy = launchedBy)
                            mMediaPlayerHolder.initMediaPlayer(this)
                            updatePlayingInfo(false)
                        }
                    }
                }
            }
        }
    }

    private fun openQueueDialog() {
        if (checkIsPlayer(false) && mMediaPlayerHolder.queueSongs.isNotEmpty()) {
            mQueueDialog = DialogHelper.showQueueSongsDialog(this, mMediaPlayerHolder)
            mQueueAdapter = mQueueDialog.getListAdapter() as QueueAdapter
        } else {
            getString(R.string.error_no_queue).toToast(this)
        }
    }

    private fun openLovedSongsDialog() {
        if (!goPreferences.lovedSongs.isNullOrEmpty()) {
            mLovedSongsDialog = DialogHelper.showLovedSongsDialog(
                this,
                this,
                mMediaPlayerHolder
            )
        } else {
            getString(R.string.error_no_loved_songs).toToast(this)
        }
    }

    //method to handle intent to play audio file from external app
    private fun handleIntent(intent: Intent) {

        intent.data?.let { returnUri ->
            contentResolver.query(returnUri, null, null, null, null)
        }?.use { cursor ->
            try {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                val song = mMusicViewModel.getSongFromIntent(cursor.getString(displayNameIndex))
                //get album songs and sort them
                val albumSongs = MusicOrgHelper.getAlbumSongs(
                    song?.artist,
                    song?.album,
                    mMusicViewModel.deviceAlbumsByArtist
                )

                onSongSelected(song, albumSongs, GoConstants.ARTIST_VIEW)

            } catch (e: Exception) {
                e.printStackTrace()
                notifyError(GoConstants.TAG_NO_MUSIC_INTENT)
            }
        }
    }

    // interface to let MediaPlayerHolder update the UI media player controls.
    private val mMediaPlayerInterface = object : MediaPlayerInterface {

        override fun onPlaybackCompleted() {
            updateRepeatStatus(true)
        }

        override fun onUpdateRepeatStatus() {
            updateRepeatStatus(false)
        }

        override fun onClose() {
            //finish activity if visible
            finishAndRemoveTask()
        }

        override fun onPositionChanged(position: Int) {
            mPlayerControlsPanelBinding.songProgress.progress = position
            if (isNowPlaying) {
                mNowPlayingBinding.npSeekBar.progress = position
            }
        }

        override fun onStateChanged() {
            updatePlayingStatus(false)
            updatePlayingStatus(isNowPlaying)
            if (mMediaPlayerHolder.state != GoConstants.RESUMED && mMediaPlayerHolder.state != GoConstants.PAUSED) {
                updatePlayingInfo(false)
                if (::mQueueDialog.isInitialized && mQueueDialog.isShowing && mMediaPlayerHolder.isQueue) {
                    mQueueAdapter.swapSelectedSong(
                        mMediaPlayerHolder.currentSong.first
                    )
                }
            }
        }

        override fun onQueueEnabled() {
            ThemeHelper.updateIconTint(
                mPlayerControlsPanelBinding.queueButton,
                ContextCompat.getColor(this@MainActivity, R.color.widgetsColor)
            )
        }

        override fun onQueueCleared() {
            if (::mQueueDialog.isInitialized && mQueueDialog.isShowing) {
                mQueueDialog.dismiss()
            }
            if (checkIsPlayer(false) && sDetailsFragmentExpanded && !mMediaPlayerHolder.isShuffledSongsQueued.first) {
                mDetailsFragment.onDisableShuffle(isShuffleMode = false, isMusicListOutputRequired = false)
            }
        }

        override fun onQueueStartedOrEnded(started: Boolean) {
            ThemeHelper.updateIconTint(
                mPlayerControlsPanelBinding.queueButton,
                when {
                    started -> ThemeHelper.resolveThemeAccent(this@MainActivity)
                    mMediaPlayerHolder.isQueue -> ContextCompat.getColor(
                        this@MainActivity,
                        R.color.widgetsColor
                    )
                    else -> {
                        ThemeHelper.resolveColorAttr(
                            this@MainActivity,
                            android.R.attr.colorButtonNormal
                        )
                    }
                }
            )
        }

        override fun onFocusLoss() {
            saveSongToPref()
        }

        override fun onSaveSong() {
            saveSongToPref()
        }

        override fun onPlaylistEnded() {
            getString(R.string.error_list_ended).toToast(this@MainActivity)
        }
    }

    // ViewPager2 adapter class
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = mActiveFragments.size

        override fun createFragment(position: Int): Fragment =
            handleOnNavigationItemSelected(position)!!
    }
}
