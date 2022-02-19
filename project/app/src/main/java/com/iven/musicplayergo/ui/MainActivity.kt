package com.iven.musicplayergo.ui

import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.*
import com.iven.musicplayergo.dialogs.NowPlaying
import com.iven.musicplayergo.dialogs.RecyclerSheet
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.fragments.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.*
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.EqualizerUtils
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.player.MediaPlayerInterface
import com.iven.musicplayergo.player.PlayerService
import com.iven.musicplayergo.preferences.SettingsFragment
import dev.chrisbanes.insetter.Insetter
import dev.chrisbanes.insetter.windowInsetTypesOf


private const val SHUFFLE_CUT_OFF = 250

class MainActivity : AppCompatActivity(), UIControlInterface, MediaControlInterface, DialogInterface.OnDismissListener {

    // View binding classes
    private lateinit var mMainActivityBinding: MainActivityBinding
    private lateinit var mPlayerControlsPanelBinding: PlayerControlsPanelBinding

    // View model
    private val mMusicViewModel: MusicViewModel by viewModels()

    // Fragments
    private val mActiveFragments: List<String> = goPreferences.activeTabs.toList()
    private var mArtistsFragment: MusicContainersFragment? = null
    private var mAllMusicFragment: AllMusicFragment? = null
    private var mFoldersFragment: MusicContainersFragment? = null
    private var mAlbumsFragment: MusicContainersFragment? = null
    private var mSettingsFragment: SettingsFragment? = null
    private var mDetailsFragment: DetailsFragment? = null
    private var mEqualizerFragment: EqFragment? = null

    private val mMusicContainersFragments = mutableListOf<MusicContainersFragment>()

    // Booleans
    private val sDetailsFragmentExpanded get() = supportFragmentManager.isFragment(GoConstants.DETAILS_FRAGMENT_TAG)
    private val sErrorFragmentExpanded get() = supportFragmentManager.isFragment(GoConstants.ERROR_FRAGMENT_TAG)
    private val sEqFragmentExpanded get() = supportFragmentManager.isFragment(GoConstants.EQ_FRAGMENT_TAG)
    private var sAllowCommit = true

    private var sCloseDetailsFragment = true

    private var sRevealAnimationRunning = false

    private var sRestoreSettingsFragment = false
    private var sAppearanceChanged = false

    // Queue dialog
    private var mQueueDialog: RecyclerSheet? = null

    // Now playing
    private var mNpDialog: NowPlaying? = null
    private var mNpBinding: NowPlayingBinding? = null
    private var mNpCoverBinding: NowPlayingCoverBinding? = null
    private var mNpControlsBinding: NowPlayingControlsBinding? = null
    private var mNpExtControlsBinding: NowPlayingExtendedControlsBinding? = null

    private var mAlbumIdNp : Long? = -1L

    // Music player things
    private val mMediaPlayerHolder get() = MediaPlayerHolder.getInstance()
    private val isNowPlaying get() = mNpDialog != null

    // Our PlayerService
    private lateinit var mPlayerService: PlayerService
    private var sBound = false
    private lateinit var mBindingIntent: Intent

    private fun checkIsPlayer(showError: Boolean): Boolean {
        if (!mMediaPlayerHolder.isMediaPlayer && !mMediaPlayerHolder.isSongFromPrefs && showError) {
            R.string.error_bad_id.toToast(this)
            return false
        }
        return true
    }

    // Defines callbacks for service binding, passed to bindService()
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            // get bound service and instantiate MediaPlayerHolder
            val binder = service as PlayerService.LocalBinder
            mPlayerService = binder.getService()
            sBound = true

            mMediaPlayerHolder.mediaPlayerInterface = mMediaPlayerInterface

            // load music and setup UI
            mMusicViewModel.deviceMusic.observe(this@MainActivity) { returnedMusic ->
                finishSetup(returnedMusic)
            }
            mMusicViewModel.getDeviceMusic()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            sBound = false
        }
    }

    private fun doBindService() {
        mBindingIntent = Intent(this, PlayerService::class.java).also {
            bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onBackPressed() {
        when {
            sDetailsFragmentExpanded and !sEqFragmentExpanded -> closeDetailsFragment(isAnimation = goPreferences.isAnimations)
            !sDetailsFragmentExpanded and sEqFragmentExpanded -> closeEqualizerFragment(isAnimation = goPreferences.isAnimations)
            sEqFragmentExpanded and sDetailsFragmentExpanded -> if (sCloseDetailsFragment) {
                closeDetailsFragment(isAnimation = goPreferences.isAnimations)
            } else {
                closeEqualizerFragment(isAnimation = goPreferences.isAnimations)
            }
            sErrorFragmentExpanded -> finishAndRemoveTask()
            else -> if (mMainActivityBinding.viewPager2.currentItem != 0) {
                mMainActivityBinding.viewPager2.currentItem =
                    0
            } else {
                if (mMediaPlayerHolder.isPlaying) {
                    DialogHelper.stopPlaybackDialog(this)
                } else {
                    onCloseActivity()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        sAllowCommit = false
        if (sAppearanceChanged) {
            super.onSaveInstanceState(outState)
            outState.putBoolean(
                GoConstants.RESTORE_SETTINGS_FRAGMENT,
                true
            )
        }
    }

    override fun onResumeFragments() {
        sAllowCommit = true
        super.onResumeFragments()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMusicViewModel.cancel()
        if (!mMediaPlayerHolder.isPlaying && ::mPlayerService.isInitialized && mPlayerService.isRunning && !mMediaPlayerHolder.isSongFromPrefs) {
            mPlayerService.stopForeground(true)
            stopService(mBindingIntent)
        }
        if (sBound) {
            unbindService(connection)
        }
    }

    override fun onResume() {
        super.onResume()
        if (mMediaPlayerHolder.isMediaPlayer && !mMediaPlayerHolder.isSongFromPrefs) {
            mMediaPlayerHolder.onRestartSeekBarCallback()
        }
    }

    // Pause SeekBar callback
    override fun onPause() {
        super.onPause()
        if (mMediaPlayerHolder.isMediaPlayer && !mMediaPlayerHolder.isSongFromPrefs) {
            mMediaPlayerInterface.onBackupSong()
            with(mMediaPlayerHolder) {
                onPauseSeekBarCallback()
                if (!isPlaying) {
                    giveUpAudioFocus()
                }
            }
        }
    }

    override fun onDismiss(p0: DialogInterface?) {
        TODO("Not yet implemented")
    }

    // Manage request permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GoConstants.PERMISSION_REQUEST_READ_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED)) {
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
            window?.navigationBarColor = ContextCompat.getColor(this, R.color.windowBackground)
            Insetter.builder()
                .padding(windowInsetTypesOf(navigationBars = true))
                .margin(windowInsetTypesOf(statusBars = true))
                .applyToView(mMainActivityBinding.root)
        }

        sAllowCommit = true

        sRestoreSettingsFragment =
            savedInstanceState?.getBoolean(GoConstants.RESTORE_SETTINGS_FRAGMENT)
                ?: intent.getBooleanExtra(
                    GoConstants.RESTORE_SETTINGS_FRAGMENT,
                    false
                )

        if (PermissionsHelper.hasToAskForReadStoragePermission(this)) {
            PermissionsHelper.manageAskForReadStoragePermission(this)
        } else {
            doBindService()
        }
    }

    private fun notifyError(errorType: String) {

        mMainActivityBinding.mainView.animate().apply {
            withStartAction {
                mPlayerControlsPanelBinding.playerView.handleViewVisibility(show = false)
                mMainActivityBinding.loadingProgressBar.handleViewVisibility(show = false)
                mMainActivityBinding.viewPager2.handleViewVisibility(show = false)
            }
            duration = 250
            alpha(1.0F)
            withEndAction {
                if (sAllowCommit) {
                    supportFragmentManager.addFragment(
                        ErrorFragment.newInstance(errorType),
                        GoConstants.ERROR_FRAGMENT_TAG
                    )
                }
            }
        }
    }

    private fun finishSetup(music: MutableList<Music>?) {

        if (!music.isNullOrEmpty()) {

            mMainActivityBinding.loadingProgressBar.handleViewVisibility(show = false)

            initMediaButtons()

            initViewPager()

            synchronized(handleRestore()) {
                mMainActivityBinding.mainView.animate().apply {
                    duration = 750
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

        // By default, ViewPager2's sensitivity is high enough to result in vertical
        // scroll events being registered as horizontal scroll events. Reflect into the
        // internal recyclerview and change the touch slope so that touch actions will
        // act more as a scroll than as a swipe.
        try {
            val recycler = ViewPager2::class.java.getDeclaredField("mRecyclerView").run {
                isAccessible = true
                get(mMainActivityBinding.viewPager2)
            }
            RecyclerView::class.java.getDeclaredField("mTouchSlop").apply {
                isAccessible = true

                val slop = get(recycler) as Int
                set(recycler, slop * 3) // 3x seems to be the best fit here
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Unable to reduce ViewPager sensitivity")
            Log.e("MainActivity", e.stackTraceToString())
        }

        initTabLayout()
    }

    private fun initTabLayout() {

        val resolvedAlphaAccentColor = ThemeHelper.getAlphaAccent(this)

        mPlayerControlsPanelBinding.tabLayout.run {

            tabIconTint = ColorStateList.valueOf(resolvedAlphaAccentColor)

            TabLayoutMediator(this, mMainActivityBinding.viewPager2) { tab, position ->
                tab.setIcon(ThemeHelper.getTabIcon(mActiveFragments[position]))
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

            getTabAt(
                if (sRestoreSettingsFragment) {
                    mMainActivityBinding.viewPager2.offscreenPageLimit
                } else {
                    0
                }
            )?.icon?.setTint(
                ThemeHelper.resolveThemeAccent(this@MainActivity)
            )
        }

        if (sRestoreSettingsFragment) {
            mMainActivityBinding.viewPager2.setCurrentItem(
                mMainActivityBinding.viewPager2.offscreenPageLimit,
                false
            )
        }
    }

    private fun initFragmentAt(position: Int) : Fragment {
        when (mActiveFragments[position]) {
            GoConstants.ARTISTS_TAB -> if (mArtistsFragment == null) {
                mArtistsFragment = MusicContainersFragment.newInstance(GoConstants.ARTIST_VIEW)
                mMusicContainersFragments.add(mArtistsFragment!!)
            }
            GoConstants.ALBUM_TAB -> if (mAlbumsFragment == null) {
                mAlbumsFragment = MusicContainersFragment.newInstance(GoConstants.ALBUM_VIEW)
                mMusicContainersFragments.add(mAlbumsFragment!!)
            }
            GoConstants.SONGS_TAB -> if (mAllMusicFragment == null) {
                mAllMusicFragment = AllMusicFragment.newInstance()
            }
            GoConstants.FOLDERS_TAB -> if (mFoldersFragment == null) {
                mFoldersFragment = MusicContainersFragment.newInstance(GoConstants.FOLDER_VIEW)
                mMusicContainersFragments.add(mFoldersFragment!!)
            }
            else -> if (mSettingsFragment == null) {
                mSettingsFragment = SettingsFragment.newInstance()
            }
        }
        return handleOnNavigationItemSelected(position)
    }

    private fun handleOnNavigationItemSelected(itemId: Int) = when (itemId) {
        0 -> getFragmentForIndex(0)
        1 -> getFragmentForIndex(1)
        2 -> getFragmentForIndex(2)
        3 -> getFragmentForIndex(3)
        else -> getFragmentForIndex(4)
    }

    private fun getFragmentForIndex(index: Int) = when (mActiveFragments[index]) {
        GoConstants.ARTISTS_TAB -> mArtistsFragment ?: initFragmentAt(index)
        GoConstants.ALBUM_TAB -> mAlbumsFragment ?: initFragmentAt(index)
        GoConstants.SONGS_TAB -> mAllMusicFragment ?: initFragmentAt(index)
        GoConstants.FOLDERS_TAB -> mFoldersFragment ?: initFragmentAt(index)
        else -> mSettingsFragment ?: initFragmentAt(index)
    }

    private fun closeFragments() {
        with(supportFragmentManager) {
            if (sEqFragmentExpanded) {
                goBackFromFragmentNow(mEqualizerFragment)
            }
            if (sDetailsFragmentExpanded) {
                goBackFromFragmentNow(mDetailsFragment)
            }
        }
    }

    private fun openDetailsFragment(
        selectedArtistOrFolder: String?,
        launchedBy: String,
        highlightedSongId: Long?
    ) {
        if (!sDetailsFragmentExpanded) {
            mDetailsFragment =
                DetailsFragment.newInstance(
                    selectedArtistOrFolder,
                    launchedBy,
                    MusicOrgHelper.getPlayingAlbumPosition(
                        selectedArtistOrFolder,
                        mMusicViewModel.deviceAlbumsByArtist
                    ),
                    highlightedSongId,
                    canUpdateSongs = mMediaPlayerHolder.currentSong?.artist == selectedArtistOrFolder && mMediaPlayerHolder.launchedBy == GoConstants.ARTIST_VIEW
                )
            sCloseDetailsFragment = true
            if (sAllowCommit) {
                supportFragmentManager.addFragment(
                    mDetailsFragment,
                    GoConstants.DETAILS_FRAGMENT_TAG
                )
            }
        }
    }

    private fun closeDetailsFragment(isAnimation: Boolean) {
        if (isAnimation) {
            if (!sRevealAnimationRunning) {
                mDetailsFragment?.onHandleBackPressed()?.run {
                    sRevealAnimationRunning = true
                    doOnEnd {
                        supportFragmentManager.goBackFromFragmentNow(mDetailsFragment)
                        sRevealAnimationRunning = false
                    }
                }
            }
        } else {
            supportFragmentManager.goBackFromFragmentNow(mDetailsFragment)
        }
    }

    private fun initMediaButtons() {

        mPlayerControlsPanelBinding.playPauseButton.setOnClickListener { resumeOrPause() }

        with(mPlayerControlsPanelBinding.queueButton) {
            safeClickListener {
                if (checkIsPlayer(showError = false) && mMediaPlayerHolder.queueSongs.isNotEmpty()) {
                    mQueueDialog = RecyclerSheet.newInstance(GoConstants.QUEUE_TYPE)
                    mQueueDialog?.show(supportFragmentManager, RecyclerSheet.TAG_MODAL_RV)
                    mQueueDialog?.onQueueCancelled = {
                        mQueueDialog = null
                    }
                } else {
                    R.string.error_no_queue.toToast(this@MainActivity)
                }
            }
            setOnLongClickListener {
                if (checkIsPlayer(showError = true) && mMediaPlayerHolder.queueSongs.isNotEmpty()) {
                    DialogHelper.showClearQueueDialog(this@MainActivity)
                }
                return@setOnLongClickListener true
            }
        }

        with(mPlayerControlsPanelBinding.favoritesButton) {
            safeClickListener {
                if (!goPreferences.favorites.isNullOrEmpty()) {
                    RecyclerSheet.newInstance(GoConstants.FAV_TYPE).show(supportFragmentManager, RecyclerSheet.TAG_MODAL_RV)
                } else {
                    R.string.error_no_favorites.toToast(this@MainActivity)
                }
            }
            setOnLongClickListener {
                if (!goPreferences.favorites.isNullOrEmpty()) {
                    DialogHelper.showClearFavoritesDialog(
                        this@MainActivity
                    )
                }
                return@setOnLongClickListener true
            }
        }

        onFavoritesUpdated(clear = false)

        with(mPlayerControlsPanelBinding.playingSongContainer) {
            safeClickListener { openNowPlaying() }
            setOnLongClickListener {
                if (!sDetailsFragmentExpanded || sDetailsFragmentExpanded and !sEqFragmentExpanded) {
                    openPlayingArtistAlbum()
                }
                return@setOnLongClickListener true
            }
        }
    }

    private fun setupSeekBarProgressListener() {

        mNpBinding?.run {
            npSeekBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {

                    val defaultPositionColor = npSeek.currentTextColor
                    val selectedColor = ThemeHelper.resolveThemeAccent(this@MainActivity)
                    var userSelectedPosition = 0
                    var isUserSeeking = false

                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            userSelectedPosition = progress
                        }
                        npSeek.text =
                            progress.toLong().toFormattedDuration(isAlbum = false, isSeekBar = true)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        isUserSeeking = true
                        npSeek.setTextColor(
                            selectedColor
                        )
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        if (isUserSeeking) {
                            npSeek.setTextColor(defaultPositionColor)
                            mMediaPlayerHolder.onPauseSeekBarCallback()
                            isUserSeeking = false
                        }
                        if (mMediaPlayerHolder.state != GoConstants.PLAYING) {
                            mPlayerControlsPanelBinding.songProgress.setProgressCompat(userSelectedPosition, true)
                            npSeekBar.progress = userSelectedPosition
                        }
                        mMediaPlayerHolder.seekTo(
                            userSelectedPosition,
                            updatePlaybackStatus = mMediaPlayerHolder.isPlaying,
                            restoreProgressCallBack = !isUserSeeking
                        )
                    }
                })
        }
    }

    private fun setupNPCoverLayout() {

        if (!ThemeHelper.isDeviceLand(resources)) {
            mNpBinding?.npArtistAlbum?.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
            mNpBinding?.npSong?.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
        }

        mNpCoverBinding?.run {
            if (VersioningHelper.isMarshmallow()) {
                setupNPCoverButtonsToasts(npPlaybackSpeed)
                npPlaybackSpeed.setOnClickListener { view ->
                    DialogHelper.showPopupForPlaybackSpeed(this@MainActivity, view)
                }
            } else {
                npPlaybackSpeed.visibility = View.GONE
            }

            npSaveTime.setOnClickListener { saveSongPosition() }
            npEqualizer.setOnClickListener { openEqualizer() }
            npLove.setOnClickListener {
                ListsHelper.addToFavorites(
                    this@MainActivity,
                    mMediaPlayerHolder.currentSong,
                    canRemove = true,
                    0,
                    mMediaPlayerHolder.launchedBy)
                onFavoritesUpdated(clear = false)
                updateNpFavoritesIcon(this@MainActivity)
            }

            with(npRepeat) {
                setImageResource(
                    ThemeHelper.getRepeatIcon(
                        mMediaPlayerHolder
                    )
                )
                ThemeHelper.updateIconTint(
                    this,
                    if (mMediaPlayerHolder.isRepeat1X || mMediaPlayerHolder.isLooping || mMediaPlayerHolder.isPauseOnEnd) {
                        ThemeHelper.resolveThemeAccent(this@MainActivity)
                    } else {
                        ContextCompat.getColor(this@MainActivity, R.color.widgetsColor)
                    }
                )
                setOnClickListener { setRepeat() }
                setupNPCoverButtonsToasts(npSaveTime, npLove, npEqualizer, this)
            }
        }
    }

    private fun setupNPCoverButtonsToasts(vararg imageButtons: ImageButton) {
        val iterator = imageButtons.iterator()
        while (iterator.hasNext()) {
            iterator.next().setOnLongClickListener { btn ->
                btn.contentDescription.toString().toToast(this)
                return@setOnLongClickListener true
            }
        }
    }

    private fun setupPreciseVolumeHandler() {

        mNpExtControlsBinding?.run {

            val isVolumeEnabled = goPreferences.isPreciseVolumeEnabled
            npVolumeValue.handleViewVisibility(show = isVolumeEnabled)
            npVolume.handleViewVisibility(show = isVolumeEnabled)
            npVolumeSeek.handleViewVisibility(show = isVolumeEnabled)

            if (isVolumeEnabled) {

                mMediaPlayerHolder.currentVolumeInPercent.run {
                    npVolume.setImageResource(
                        ThemeHelper.getPreciseVolumeIcon(
                            this
                        )
                    )
                    npVolumeSeek.progress = this
                    npVolumeValue.text = this.toString()
                }

                npVolumeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                    val defaultValueColor = mNpExtControlsBinding?.npVolumeValue?.currentTextColor
                    val selectedColor = ThemeHelper.resolveThemeAccent(this@MainActivity)

                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            mMediaPlayerHolder.setPreciseVolume(progress)
                            npVolumeValue.text = progress.toString()
                            npVolume.setImageResource(
                                ThemeHelper.getPreciseVolumeIcon(
                                    progress
                                )
                            )
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        npVolumeValue.setTextColor(selectedColor)
                        ThemeHelper.updateIconTint(
                            npVolume,
                            selectedColor
                        )
                    }
                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        npVolumeValue.setTextColor(defaultValueColor!!)
                        ThemeHelper.updateIconTint(
                            npVolume,
                            defaultValueColor
                        )
                    }
                })
            }
        }
    }

    private fun openNowPlaying() {

        if (checkIsPlayer(showError = true) && mMediaPlayerHolder.isCurrentSong) {

            mNpDialog = NowPlaying.newInstance().apply {

                show(supportFragmentManager, NowPlaying.TAG_MODAL)

                onNowPlayingAdded = { nowPlayingBinding ->

                    nowPlayingBinding?.run {
                        mNpBinding = this
                        mNpCoverBinding = NowPlayingCoverBinding.bind(root)
                        mNpControlsBinding = NowPlayingControlsBinding.bind(root)
                        mNpExtControlsBinding =
                            NowPlayingExtendedControlsBinding.bind(root)

                        npSong.isSelected = true
                        npArtistAlbum.isSelected = true
                        setupNPCoverLayout()
                        npPlayingSongContainer.setOnClickListener {
                            openPlayingArtistAlbum()
                            mNpDialog?.dismiss()
                        }
                    }

                    mNpControlsBinding?.run {
                        npSkipPrev.setOnClickListener { skip(isNext = false) }
                        npFastRewind.setOnClickListener { fastSeek(isForward = false) }
                        npPlay.setOnClickListener { resumeOrPause() }
                        npSkipNext.setOnClickListener { skip(isNext = true) }
                        npFastForward.setOnClickListener { fastSeek(isForward = true) }
                    }

                    setupPreciseVolumeHandler()

                    setupSeekBarProgressListener()

                    updateNpInfo()

                    mMediaPlayerHolder.currentSong?.let { song ->
                        loadNpCover(song)
                        mNpBinding?.npSeek?.text =
                            mMediaPlayerHolder.playerPosition.toLong().toFormattedDuration(isAlbum = false, isSeekBar = true)
                    }

                    mNpBinding?.npSeekBar?.progress =
                        mPlayerControlsPanelBinding.songProgress.progress

                    // to ensure full dialog's height
                    mNpBinding?.root?.afterMeasured {
                        dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bs ->
                            BottomSheetBehavior.from(bs).peekHeight = height
                        }
                    }
                    onNowPlayingCancelled = {
                        mNpDialog = null
                        mNpBinding = null
                        mNpCoverBinding = null
                        mNpControlsBinding = null
                        mNpExtControlsBinding = null
                    }
                }
            }
        }
    }

    override fun onChangePlaybackSpeed(speed: Float) {
        mMediaPlayerHolder.setPlaybackSpeed(speed)
    }

    private fun saveSongPosition() {
        val song = mMediaPlayerHolder.currentSong
        when (val position = mMediaPlayerHolder.playerPosition) {
            0 -> mNpCoverBinding?.npLove?.callOnClick()
            else -> {
                ListsHelper.addToFavorites(this, song, canRemove = false, position, mMediaPlayerHolder.launchedBy)
                onFavoriteAddedOrRemoved()
            }
        }
    }

    private fun updateNpFavoritesIcon(context: Context) {
        mNpCoverBinding?.run {
            mMediaPlayerHolder.currentSong?.let { song ->
                val favorites = goPreferences.favorites
                val isFavorite = favorites != null && favorites.contains(song.toSavedMusic(0, mMediaPlayerHolder.launchedBy))
                val favoritesButtonColor = if (isFavorite) {
                    npLove.setImageResource(R.drawable.ic_favorite)
                    ThemeHelper.resolveThemeAccent(context)
                } else {
                    npLove.setImageResource(R.drawable.ic_favorite_empty)
                    ThemeHelper.resolveColorAttr(
                        context,
                        android.R.attr.colorButtonNormal
                    )
                }
                ThemeHelper.updateIconTint(npLove, favoritesButtonColor)
            }
        }
    }

    override fun onAppearanceChanged(isThemeChanged: Boolean) {
        sAppearanceChanged = true
        synchronized(mMediaPlayerInterface.onBackupSong()) {
            if (isThemeChanged) {
                AppCompatDelegate.setDefaultNightMode(
                    ThemeHelper.getDefaultNightMode(
                        this
                    )
                )
            } else {
                ThemeHelper.applyChanges(this, restoreSettings = true)
            }
        }
    }

    override fun onPlaybackSpeedToggled() {
        //avoid having user stuck at selected playback speed
        val isPlaybackPersisted = goPreferences.playbackSpeedMode != GoConstants.PLAYBACK_SPEED_ONE_ONLY
        mMediaPlayerHolder.setPlaybackSpeed(if (isPlaybackPersisted) {
            goPreferences.latestPlaybackSpeed
        } else {
            1.0F
        })
    }

    override fun onPreciseVolumeToggled() {
        //avoid having user stuck at lowered volume without knowing why
        mMediaPlayerHolder.setPreciseVolume(if (!goPreferences.isPreciseVolumeEnabled) {
            goPreferences.latestVolume = mMediaPlayerHolder.currentVolumeInPercent
            100
        } else {
            goPreferences.latestVolume
        })
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
            mNpControlsBinding?.npPlay?.setImageResource(drawable)
        } else {
            mPlayerControlsPanelBinding.playPauseButton.setImageResource(
                drawable
            )
        }
    }

    override fun onFavoriteAddedOrRemoved() {
        onFavoritesUpdated(clear = false)
        updateNpFavoritesIcon(this@MainActivity)
    }

    override fun onFavoritesUpdated(clear: Boolean) {

        val favorites = goPreferences.favorites?.toMutableList()

        if (clear) {
            favorites?.clear()
            goPreferences.favorites = null
        }

        val favoritesButtonColor = if (favorites.isNullOrEmpty()) {
            mPlayerControlsPanelBinding.favoritesButton.setImageResource(R.drawable.ic_favorite_empty)
            ThemeHelper.resolveColorAttr(
                this,
                android.R.attr.colorButtonNormal
            )
        } else {
            mPlayerControlsPanelBinding.favoritesButton.setImageResource(R.drawable.ic_favorite)
            ThemeHelper.resolveThemeAccent(this)
        }
        ThemeHelper.updateIconTint(
            mPlayerControlsPanelBinding.favoritesButton,
            favoritesButtonColor
        )
    }

    private fun restorePlayerStatus() {
        // If we are playing and the activity was restarted
        // update the controls panel
        mMediaPlayerHolder.run {
            if (isMediaPlayer && isPlaying) {
                onRestartSeekBarCallback()
                updatePlayingInfo(restore = true)
            } else {
                isSongFromPrefs = goPreferences.latestPlayedSong != null

                var isQueueRestored = goPreferences.isQueue
                if (!goPreferences.queue.isNullOrEmpty()) {
                    queueSongs = goPreferences.queue?.toMutableList()!!
                    setQueueEnabled(enabled = true, canSkip = false)
                }

                val song = if (isSongFromPrefs) {
                    val songIsAvailable = songIsAvailable(goPreferences.latestPlayedSong)
                    if (!songIsAvailable.first && isQueueRestored != null) {
                        goPreferences.isQueue = null
                        isQueueRestored = null
                        isQueue = null
                    } else {
                        if (isQueueRestored != null) {
                            isQueue = isQueueRestored
                            isQueueStarted = true
                            mediaPlayerInterface.onQueueStartedOrEnded(started = true)
                        }
                    }
                    songIsAvailable.second
                } else {
                    mMusicViewModel.randomMusic
                }

                song?.let { restoredSong ->

                    val songs = MusicOrgHelper.getAlbumSongs(
                        (isQueueRestored ?: restoredSong).artist,
                        (isQueueRestored ?: restoredSong).album,
                        mMusicViewModel.deviceAlbumsByArtist
                    )

                    if (!songs.isNullOrEmpty()) {
                        isPlay = false
                        updateCurrentSong(restoredSong, songs, restoredSong.launchedBy)
                        preparePlayback()
                        updatePlayingInfo(restore = false)
                        mPlayerControlsPanelBinding.songProgress.setProgressCompat(restoredSong.startFrom, true)
                    } else {
                        notifyError(GoConstants.TAG_SD_NOT_READY)
                    }
                }
                if (intent != null && intent.getStringExtra(GoConstants.LAUNCHED_BY_TILE) != null) {
                    mMediaPlayerHolder.resumeMediaPlayer()
                }
            }
            onUpdateDefaultAlbumArt(
                ContextCompat.getDrawable(this@MainActivity, R.drawable.album_art)?.toBitmap()
            )
        }
    }

    // method to update info on controls panel
    private fun updatePlayingInfo(restore: Boolean) {

        val selectedSong = mMediaPlayerHolder.currentSong

        mPlayerControlsPanelBinding.songProgress.progress = 0
        mPlayerControlsPanelBinding.songProgress.max = selectedSong?.duration!!.toInt()

        mPlayerControlsPanelBinding.playingSong.text = selectedSong.title

        mPlayerControlsPanelBinding.playingArtist.text =
            getString(
                R.string.artist_and_album,
                selectedSong.artist,
                selectedSong.album
            )

        updateRepeatStatus(onPlaybackCompletion = false)

        if (isNowPlaying) {
            updateNpInfo()
        }

        if (restore) {

            if (mMediaPlayerHolder.queueSongs.isNotEmpty() && !mMediaPlayerHolder.isQueueStarted) {
                mMediaPlayerInterface.onQueueEnabled()
            } else {
                mMediaPlayerInterface.onQueueStartedOrEnded(started = mMediaPlayerHolder.isQueueStarted)
            }

            updatePlayingStatus(isNowPlaying = false)

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
        if (isNowPlaying && mNpCoverBinding != null) {
            mNpCoverBinding?.npRepeat?.setImageResource(
                ThemeHelper.getRepeatIcon(mMediaPlayerHolder)
            )
            when {
                onPlaybackCompletion -> ThemeHelper.updateIconTint(
                    mNpCoverBinding?.npRepeat!!,
                    resolvedIconsColor
                )
                mMediaPlayerHolder.isRepeat1X or mMediaPlayerHolder.isLooping or mMediaPlayerHolder.isPauseOnEnd -> {
                    ThemeHelper.updateIconTint(
                        mNpCoverBinding?.npRepeat!!, ThemeHelper.resolveThemeAccent(this)
                    )
                }
                else -> ThemeHelper.updateIconTint(
                    mNpCoverBinding?.npRepeat!!,
                    resolvedIconsColor
                )
            }
        }
    }

    // first: song is available, second: returned song
    private fun songIsAvailable(song: Music?) : Pair<Boolean, Music?> = try {
        if (mMusicViewModel.deviceMusicFiltered?.savedSongIsAvailable(song) == null) {
            Pair(first = false, second = saveRandomSongToPrefs())
        } else {
            Pair(first = true, second = song)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Pair(first = false, second = saveRandomSongToPrefs())
    }

    private fun saveRandomSongToPrefs() : Music? {
        val randomMusic = mMusicViewModel.randomMusic
        goPreferences.latestPlayedSong = randomMusic
        return randomMusic
    }

    private fun loadNpCover(selectedSong: Music) {
        if (goPreferences.isCovers) {
            mAlbumIdNp = selectedSong.albumId
            mNpCoverBinding?.npCover?.load(selectedSong.albumId?.toAlbumArtURI()) {
                error(ContextCompat.getDrawable(this@MainActivity, R.drawable.album_art))
            }
        }
    }

    private fun updateNpInfo() {

        mMediaPlayerHolder.currentSong?.let { song ->
            val selectedSongDuration = song.duration
            if (mAlbumIdNp != song.albumId && goPreferences.isCovers && isNowPlaying
            ) {
                loadNpCover(song)
            }

            mNpBinding?.npSong?.text = song.title

            mNpBinding?.npArtistAlbum?.text =
                getString(
                    R.string.artist_and_album,
                    song.artist,
                    song.album
                )

            mNpBinding?.npDuration?.text =
                selectedSongDuration.toFormattedDuration(isAlbum = false, isSeekBar = true)

            mNpBinding?.npSeekBar?.max = song.duration.toInt()

            song.id?.toContentUri()?.toBitrate(this)?.let { (first, second) ->
                mNpBinding?.npRates?.text =
                    getString(R.string.rates, first, second)
            }
        }
        updateNpFavoritesIcon(this)
        updatePlayingStatus(isNowPlaying = true)
    }

    private fun getSongSource(): String? {
        val selectedSong = mMediaPlayerHolder.currentSong
        return when (mMediaPlayerHolder.launchedBy) {
            GoConstants.FOLDER_VIEW -> selectedSong?.relativePath
            GoConstants.ARTIST_VIEW -> selectedSong?.artist
            else -> selectedSong?.album
        }
    }

    private fun openPlayingArtistAlbum() {
        if (mMediaPlayerHolder.isCurrentSong) {
            val selectedArtistOrFolder = getSongSource()
            if (sDetailsFragmentExpanded) {
                if (mDetailsFragment?.hasToUpdate(selectedArtistOrFolder)!!) {
                    closeDetailsFragment(isAnimation = false)
                } else {
                    mDetailsFragment?.tryToSnapToAlbumPosition(
                        MusicOrgHelper.getPlayingAlbumPosition(
                            selectedArtistOrFolder,
                            mMusicViewModel.deviceAlbumsByArtist,
                        )
                    )
                }
                mDetailsFragment?.highlightSong(mMediaPlayerHolder.currentSong?.id)
            } else {
                openDetailsFragment(
                    selectedArtistOrFolder,
                    mMediaPlayerHolder.launchedBy,
                    mMediaPlayerHolder.currentSong?.id
                )
            }
        }
    }

    override fun onCloseActivity() {
        if (mMediaPlayerHolder.isPlaying) {
            DialogHelper.stopPlaybackDialog(this)
        } else {
            finishAndRemoveTask()
        }
    }

    override fun onHandleFocusPref() {
        if (mMediaPlayerHolder.isMediaPlayer) {
            if (goPreferences.isFocusEnabled) {
                mMediaPlayerHolder.tryToGetAudioFocus()
            } else {
                mMediaPlayerHolder.giveUpAudioFocus()
            }
        }
    }

    override fun onHandleCoverOptionsUpdate() {
        mMediaPlayerHolder.updateMediaSessionMetaData()
        mAlbumsFragment?.onUpdateCoverOption()
    }

    override fun onHandleNotificationUpdate(isAdditionalActionsChanged: Boolean) {
        mMediaPlayerHolder.onHandleNotificationUpdate(isAdditionalActionsChanged = isAdditionalActionsChanged)
    }

    override fun onOpenNewDetailsFragment() {
        with(getSongSource()) {
            openDetailsFragment(
                this,
                mMediaPlayerHolder.launchedBy,
                mMediaPlayerHolder.currentSong?.id
            )
        }
    }

    override fun onArtistOrFolderSelected(artistOrFolder: String, launchedBy: String) {
        openDetailsFragment(
            artistOrFolder,
            launchedBy,
            mMediaPlayerHolder.currentSong?.id
        )
    }

    private fun preparePlayback() {
        if (::mPlayerService.isInitialized && !mPlayerService.isRunning) {
            startService(mBindingIntent)
        }
        mMediaPlayerHolder.initMediaPlayer(mMediaPlayerHolder.currentSong)
    }

    override fun onSongSelected(song: Music?, songs: List<Music>?, songLaunchedBy: String) {
        mMediaPlayerHolder.run {
            if (isSongFromPrefs) {
                isSongFromPrefs = false
            }
            if (!isPlay) {
                isPlay = true
            }
            if (isQueue != null) {
                setQueueEnabled(enabled = false, canSkip = false)
            }
            val albumSongs = songs ?: MusicOrgHelper.getAlbumSongs(
                song?.artist,
                song?.album,
                mMusicViewModel.deviceAlbumsByArtist
            )
            updateCurrentSong(song, albumSongs, songLaunchedBy)
            preparePlayback()
        }
    }

    private fun resumeOrPause() {
        mMediaPlayerHolder.resumeOrPause()
    }

    private fun fastSeek(isForward: Boolean) {
        if (checkIsPlayer(showError = true)) {
            mMediaPlayerHolder.fastSeek(isForward = isForward)
        }
    }

    private fun skip(isNext: Boolean) {
        if (checkIsPlayer(showError = true)) {
            if (!mMediaPlayerHolder.isPlay) { mMediaPlayerHolder.isPlay = true }
            if (mMediaPlayerHolder.isSongFromPrefs) { mMediaPlayerHolder.isSongFromPrefs = false }
            if (isNext) {
                mMediaPlayerHolder.skip(isNext = true)
            } else {
                mMediaPlayerHolder.instantReset()
            }
        }
    }

    private fun setRepeat() {
        if (checkIsPlayer(showError = true)) {
            mMediaPlayerHolder.repeat(updatePlaybackStatus = mMediaPlayerHolder.isPlaying)
            updateRepeatStatus(onPlaybackCompletion = false)
        }
    }

    override fun onGetEqualizer(): Triple<Equalizer?, BassBoost?, Virtualizer?> =
        mMediaPlayerHolder.getEqualizer()

    override fun onEnableEqualizer(isEnabled: Boolean) {
        mMediaPlayerHolder.setEqualizerEnabled(isEnabled = isEnabled)
    }

    override fun onSaveEqualizerSettings(
        selectedPreset: Int,
        bassBoost: Short,
        virtualizer: Short
    ) {
        mMediaPlayerHolder.onSaveEqualizerSettings(selectedPreset, bassBoost, virtualizer)
    }

    private fun openEqualizer() {
        if (checkIsPlayer(showError = true)) {
            if (!EqualizerUtils.hasEqualizer(this)) {
                synchronized(mMediaPlayerHolder.onOpenEqualizerCustom()) {
                    if (!sEqFragmentExpanded) {
                        mEqualizerFragment = EqFragment.newInstance()
                        sCloseDetailsFragment = !sDetailsFragmentExpanded
                        if (sAllowCommit) {
                            supportFragmentManager.addFragment(
                                mEqualizerFragment,
                                GoConstants.EQ_FRAGMENT_TAG
                            )
                        }
                        mNpDialog?.dismiss()
                    }
                }
            } else {
                mMediaPlayerHolder.openEqualizer(this)
            }
        }
    }

    private fun closeEqualizerFragment(isAnimation: Boolean) {
        if (isAnimation) {
            if (!sRevealAnimationRunning) {
                mEqualizerFragment?.onHandleBackPressed()?.run {
                    sRevealAnimationRunning = true
                    doOnEnd {
                        supportFragmentManager.goBackFromFragmentNow(mEqualizerFragment)
                        sRevealAnimationRunning = false
                    }
                }
            }
        } else {
            supportFragmentManager.goBackFromFragmentNow(mEqualizerFragment)
        }
    }

    override fun onAddToQueue(song: Music?) {
        if (checkIsPlayer(showError = true)) {

            with(mMediaPlayerHolder) {

                setCanRestoreQueue()

                song?.let { songToQueue ->

                    // don't add duplicates
                    if (restoreQueueSong != songToQueue) {
                        queueSongs.remove(songToQueue)
                        if (isQueue != null && !canRestoreQueue && isQueueStarted) {
                            val currentPosition = mMediaPlayerHolder.queueSongs.indexOf(currentSong)
                            queueSongs.add(currentPosition+1, songToQueue)
                        } else {
                            queueSongs.add(songToQueue)
                        }
                    }

                    if (canRestoreQueue && restoreQueueSong == null) {
                        restoreQueueSong = songToQueue
                    }

                    getString(
                        R.string.queue_song_add,
                        songToQueue.title
                    ).toToast(this@MainActivity)

                    if (!isPlaying || state == GoConstants.PAUSED) {
                        startSongFromQueue(songToQueue)
                    }
                }
            }
        }
    }

    override fun onAddAlbumToQueue(
        songs: List<Music>?,
        forcePlay: Pair<Boolean, Music?>
    ) {
        if (checkIsPlayer(showError = true)) {

            with(mMediaPlayerHolder) {

                setCanRestoreQueue()

                songs?.let { songsToQueue ->

                    // don't add duplicates
                    addSongsToNextQueuePosition(if (restoreQueueSong != songsToQueue.first()) {
                        queueSongs.removeAll(songsToQueue)
                        songsToQueue
                    } else {
                        songsToQueue.minus(songsToQueue.first())
                    })

                    if (canRestoreQueue && restoreQueueSong == null) {
                        restoreQueueSong = forcePlay.second ?: songsToQueue.first()
                    }

                    if (!isPlaying || forcePlay.first) {
                        startSongFromQueue(forcePlay.second ?: songsToQueue.first())
                    }
                }
            }
        }
    }

    override fun onSongsShuffled(
        songs: List<Music>?,
        songLaunchedBy: String
    ) {
        songs?.run {
            onAddAlbumToQueue(
                if (size >= SHUFFLE_CUT_OFF) {
                    this.shuffled().take(SHUFFLE_CUT_OFF)
                } else {
                    this.shuffled()
                },
                forcePlay = Pair(first = true, second = null)
            )
        }
    }

    override fun onUpdatePlayingAlbumSongs(songs: List<Music>?) {
        if (songs != null) {
            mMediaPlayerHolder.updateCurrentSongs(songs)
        } else {
            val currentAlbumSize = mMediaPlayerHolder.getCurrentAlbumSize()
            if (currentAlbumSize != 0 && currentAlbumSize > 1) {
                // update current song to reflect this change
                mMediaPlayerHolder.updateCurrentSongs(null)
            }
            if (mAllMusicFragment != null && !mAllMusicFragment?.onSongVisualizationChanged()!!) {
                ThemeHelper.applyChanges(this, restoreSettings = true)
            }
        }
    }

    override fun onAddToFilter(stringToFilter: String?) {
        if (mMusicViewModel.deviceMusicFiltered?.size == 1) {
            R.string.error_eq.toToast(this)
        } else {
            stringToFilter?.let { string ->

                ListsHelper.addToHiddenItems(string)

                if (mMusicContainersFragments.isNullOrEmpty() || !mMusicContainersFragments.isNullOrEmpty() && !mMusicContainersFragments.first().onListFiltered(string)) {
                    ThemeHelper.applyChanges(this, restoreSettings = false)
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
                    mSettingsFragment?.onFiltersChanged()

                    // be sure to update queue, favorites and the controls panel
                    MusicOrgHelper.updateMediaPlayerHolderLists(this, mMusicViewModel.randomMusic)?.let { song ->
                        val songs = MusicOrgHelper.getAlbumSongs(
                            song.artist,
                            song.album,
                            mMusicViewModel.deviceAlbumsByArtist
                        )
                        mMediaPlayerHolder.run {
                            isPlay = isPlaying
                            updateCurrentSong(song, songs, GoConstants.ARTIST_VIEW)
                            initMediaPlayer(song)
                        }
                        updatePlayingInfo(restore = false)
                    }
                }
            }
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

                onSongSelected(song, null, GoConstants.ARTIST_VIEW)

            } catch (e: Exception) {
                e.printStackTrace()
                notifyError(GoConstants.TAG_NO_MUSIC_INTENT)
            }
        }
    }

    // interface to let MediaPlayerHolder update the UI media player controls.
    private val mMediaPlayerInterface = object : MediaPlayerInterface {

        override fun onPlaybackCompleted() {
            if (!mMediaPlayerHolder.isPauseOnEnd) {
                updateRepeatStatus(onPlaybackCompletion = true)
            }
        }

        override fun onUpdateRepeatStatus() {
            updateRepeatStatus(onPlaybackCompletion = false)
        }

        override fun onClose() {
            //finish activity if visible
            finishAndRemoveTask()
        }

        override fun onPositionChanged(position: Int) {
            mPlayerControlsPanelBinding.songProgress.setProgressCompat(position, true)
            if (isNowPlaying) {
                mNpBinding?.npSeekBar?.progress = position
            }
        }

        override fun onStateChanged() {
            updatePlayingStatus(isNowPlaying = false)
            updatePlayingStatus(isNowPlaying = isNowPlaying)
            if (mMediaPlayerHolder.state != GoConstants.RESUMED && mMediaPlayerHolder.state != GoConstants.PAUSED) {
                updatePlayingInfo(restore = false)
                if (mQueueDialog != null && mMediaPlayerHolder.isQueue != null) {
                    mQueueDialog?.swapQueueSong(mMediaPlayerHolder.currentSong)
                } else if (sDetailsFragmentExpanded) {
                    mDetailsFragment?.swapSelectedSong(
                        mMediaPlayerHolder.currentSong?.id
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

        override fun onQueueStartedOrEnded(started: Boolean) {
            ThemeHelper.updateIconTint(
                mPlayerControlsPanelBinding.queueButton,
                when {
                    started -> ThemeHelper.resolveThemeAccent(this@MainActivity)
                    mMediaPlayerHolder.queueSongs.isEmpty() -> ThemeHelper.resolveColorAttr(
                        this@MainActivity,
                        android.R.attr.colorButtonNormal
                    )
                    else -> {
                        mQueueDialog?.dismiss()
                        ContextCompat.getColor(
                            this@MainActivity,
                            R.color.widgetsColor
                        )
                    }
                }
            )
        }

        override fun onBackupSong() {
            if (!mMediaPlayerHolder.isPlaying) {
                goPreferences.latestPlayedSong = mMediaPlayerHolder.currentSong?.toSavedMusic(mMediaPlayerHolder.playerPosition, mMediaPlayerHolder.launchedBy)
            }
        }

        override fun onForegroundServiceStopped() {
            DialogHelper.notifyForegroundServiceStopped(this@MainActivity)
        }
    }

    // ViewPager2 adapter class
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = mActiveFragments.size
        override fun createFragment(position: Int): Fragment = handleOnNavigationItemSelected(position)
    }
}
