package com.iven.musicplayergo.ui

import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Log
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
import java.util.*


private const val SHUFFLE_CUT_OFF = 250

class MainActivity : AppCompatActivity(), UIControlInterface, MediaControlInterface {

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

    // Music player things
    private lateinit var mMediaPlayerHolder: MediaPlayerHolder
    private val isMediaPlayerHolder get() = ::mMediaPlayerHolder.isInitialized

    // Our PlayerService
    private lateinit var mPlayerService: PlayerService
    private var sBound = false
    private lateinit var mBindingIntent: Intent

    private fun checkIsPlayer(showError: Boolean): Boolean {
        if (!isMediaPlayerHolder && !mMediaPlayerHolder.isMediaPlayer && !mMediaPlayerHolder.isSongFromPrefs && showError) {
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

            mMediaPlayerHolder = mPlayerService.mediaPlayerHolder.apply {
                mediaPlayerInterface = mMediaPlayerInterface
            }

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
                if (!isMediaPlayerHolder && mMediaPlayerHolder.isPlaying) {
                    DialogHelper.stopPlaybackDialog(this, mMediaPlayerHolder)
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
        if (isMediaPlayerHolder && !mMediaPlayerHolder.isPlaying && ::mPlayerService.isInitialized && mPlayerService.isRunning && !mMediaPlayerHolder.isSongFromPrefs) {
            mPlayerService.stopForeground(true)
            stopService(mBindingIntent)
        }
        if (sBound) {
            unbindService(connection)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isMediaPlayerHolder && mMediaPlayerHolder.isMediaPlayer && !mMediaPlayerHolder.isSongFromPrefs) {
            mMediaPlayerHolder.onRestartSeekBarCallback()
        }
    }

    // Pause SeekBar callback
    override fun onPause() {
        super.onPause()
        if (isMediaPlayerHolder && mMediaPlayerHolder.isMediaPlayer && !mMediaPlayerHolder.isSongFromPrefs) {
            mMediaPlayerInterface.onBackupSong()
            with(mMediaPlayerHolder) {
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
        if (VersioningHelper.isMarshmallow()) {
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
    }

    override fun onDenyPermission() {
        notifyError(GoConstants.TAG_NO_PERMISSION)
    }

    override fun attachBaseContext(newBase: Context?) {
        if (goPreferences.locale == null) {
            super.attachBaseContext(newBase)
        } else {
            val locale = Locale.forLanguageTag(goPreferences.locale!!)
            val localeUpdatedContext = ContextUtils.updateLocale(newBase!!, locale)
            super.attachBaseContext(localeUpdatedContext)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeHelper.getAccentedTheme()?.run {
            setTheme(first)
        }

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
                        mMediaPlayerHolder,
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

        mPlayerControlsPanelBinding.playPauseButton.setOnClickListener { mMediaPlayerHolder.resumeOrPause() }

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
                    DialogHelper.showClearQueueDialog(this@MainActivity, mMediaPlayerHolder)
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
            safeClickListener {
                if (checkIsPlayer(showError = true) && mMediaPlayerHolder.isCurrentSong) {
                    mNpDialog = NowPlaying.newInstance().apply {
                        show(supportFragmentManager, NowPlaying.TAG_MODAL)
                        onNowPlayingCancelled = {
                            mNpDialog = null
                        }
                    }
                }
            }
            setOnLongClickListener {
                if (!sDetailsFragmentExpanded || sDetailsFragmentExpanded and !sEqFragmentExpanded) {
                    onOpenPlayingArtistAlbum()
                }
                return@setOnLongClickListener true
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

    private fun updatePlayingStatus() {
        val isPlaying = mMediaPlayerHolder.state != GoConstants.PAUSED
        val drawable =
            if (isPlaying) {
                R.drawable.ic_pause
            } else {
                R.drawable.ic_play
            }
        mPlayerControlsPanelBinding.playPauseButton.setImageResource(
            drawable
        )
    }

    override fun onFavoriteAddedOrRemoved() {
        onFavoritesUpdated(clear = false)
        mNpDialog?.updateNpFavoritesIcon(this@MainActivity)
    }

    override fun onUpdatePositionFromNP(position: Int) {
        mPlayerControlsPanelBinding.songProgress.setProgressCompat(position, true)
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
        if (isMediaPlayerHolder) {
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

        mNpDialog?.run {
            updateRepeatStatus(onPlaybackCompletion = false)
            updateNpInfo()
        }

        if (restore) {

            if (mMediaPlayerHolder.queueSongs.isNotEmpty() && !mMediaPlayerHolder.isQueueStarted) {
                mMediaPlayerInterface.onQueueEnabled()
            } else {
                mMediaPlayerInterface.onQueueStartedOrEnded(started = mMediaPlayerHolder.isQueueStarted)
            }

            updatePlayingStatus()
            updatePlayingStatus()

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

    private fun getSongSource(): String? {
        val selectedSong = mMediaPlayerHolder.currentSong
        return when (mMediaPlayerHolder.launchedBy) {
            GoConstants.FOLDER_VIEW -> selectedSong?.relativePath
            GoConstants.ARTIST_VIEW -> selectedSong?.artist
            else -> selectedSong?.album
        }
    }

    override fun onOpenPlayingArtistAlbum() {
        if (isMediaPlayerHolder && mMediaPlayerHolder.isCurrentSong) {
            val selectedArtistOrFolder = getSongSource()
            if (sDetailsFragmentExpanded) {
                if (mDetailsFragment?.hasToUpdate(selectedArtistOrFolder)!!) {
                    closeDetailsFragment(isAnimation = false)
                } else {
                    mDetailsFragment?.tryToSnapToAlbumPosition(
                        MusicOrgHelper.getPlayingAlbumPosition(
                            mMediaPlayerHolder,
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
        if (isMediaPlayerHolder && mMediaPlayerHolder.isPlaying) {
            DialogHelper.stopPlaybackDialog(this, mMediaPlayerHolder)
        } else {
            finishAndRemoveTask()
        }
    }

    override fun onHandleCoverOptionsUpdate() {
        if (isMediaPlayerHolder) {
            mMediaPlayerHolder.updateMediaSessionMetaData()
        }
        mAlbumsFragment?.onUpdateCoverOption()
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
        if (isMediaPlayerHolder) {
            if (::mPlayerService.isInitialized && !mPlayerService.isRunning) {
                startService(mBindingIntent)
            }
            mMediaPlayerHolder.initMediaPlayer(mMediaPlayerHolder.currentSong)
        }
    }

    override fun onSongSelected(song: Music?, songs: List<Music>?, songLaunchedBy: String) {
        if (isMediaPlayerHolder) {
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
    }

    override fun onGetMediaPlayerHolder() = if (isMediaPlayerHolder) {
        mMediaPlayerHolder
    } else {
        null
    }

    override fun onOpenEqualizer() {
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
        if (isMediaPlayerHolder) {
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

                    if (isMediaPlayerHolder) {
                        // be sure to update queue, favorites and the controls panel
                        MusicOrgHelper.updateMediaPlayerHolderLists(mMediaPlayerHolder, this, mMusicViewModel.randomMusic)?.let { song ->
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
                mNpDialog?.updateRepeatStatus(onPlaybackCompletion = true)
            }
        }

        override fun onUpdateRepeatStatus() {
            mNpDialog?.updateRepeatStatus(onPlaybackCompletion = false)
        }

        override fun onClose() {
            //finish activity if visible
            finishAndRemoveTask()
        }

        override fun onPositionChanged(position: Int) {
            mPlayerControlsPanelBinding.songProgress.setProgressCompat(position, true)
            mNpDialog?.updateProgress(position)
        }

        override fun onStateChanged() {
            updatePlayingStatus()
            mNpDialog?.updatePlayingStatus()
            if (isMediaPlayerHolder && mMediaPlayerHolder.state != GoConstants.RESUMED && mMediaPlayerHolder.state != GoConstants.PAUSED) {
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
