package com.iven.musicplayergo.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.MainActivityBinding
import com.iven.musicplayergo.databinding.PlayerControlsPanelBinding
import com.iven.musicplayergo.dialogs.Dialogs
import com.iven.musicplayergo.dialogs.NowPlaying
import com.iven.musicplayergo.dialogs.RecyclerSheet
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.fragments.*
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.EqualizerUtils
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.player.MediaPlayerInterface
import com.iven.musicplayergo.player.PlayerService
import com.iven.musicplayergo.preferences.ContextUtils
import com.iven.musicplayergo.preferences.SettingsFragment
import com.iven.musicplayergo.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*


class MainActivity : AppCompatActivity(), UIControlInterface, MediaControlInterface {

    // View binding classes
    private lateinit var mMainActivityBinding: MainActivityBinding
    private lateinit var mPlayerControlsPanelBinding: PlayerControlsPanelBinding

    // View model
    private val mMusicViewModel: MusicViewModel by viewModels()

    // Preferences
    private val mGoPreference get() = GoPreferences.getPrefsInstance()

    // Fragments
    private var mArtistsFragment: MusicContainersFragment? = null
    private var mAllMusicFragment: AllMusicFragment? = null
    private var mFoldersFragment: MusicContainersFragment? = null
    private var mAlbumsFragment: MusicContainersFragment? = null
    private var mSettingsFragment: SettingsFragment? = null
    private var mDetailsFragment: DetailsFragment? = null
    private var mEqualizerFragment: EqFragment? = null

    // Booleans
    private val sDetailsFragmentExpanded get() = supportFragmentManager.isFragment(GoConstants.DETAILS_FRAGMENT_TAG)
    private val sErrorFragmentExpanded get() = supportFragmentManager.isFragment(GoConstants.ERROR_FRAGMENT_TAG)
    private val sEqFragmentExpanded get() = supportFragmentManager.isFragment(GoConstants.EQ_FRAGMENT_TAG)
    private var sAllowCommit = true

    private var sCloseDetailsFragment = true

    private var sRevealAnimationRunning = false

    private var sRestoreSettingsFragment = false
    private var sTabToRestore = -1
    private var sDialogToRestore: String? = null

    // Queue dialog
    private var mQueueDialog: RecyclerSheet? = null

    // Now playing
    private var mNpDialog: NowPlaying? = null

    // Favorites dialog
    private var mFavoritesDialog: RecyclerSheet? = null

    // Sleep timer dialog
    private var mSleepTimerDialog: RecyclerSheet? = null

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
                onHandleNotificationColorUpdate(Theming.resolveColorAttr(this@MainActivity, R.attr.main_bg))
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
            sDetailsFragmentExpanded and !sEqFragmentExpanded -> closeDetailsFragment(isAnimation = mGoPreference.isAnimations)
            !sDetailsFragmentExpanded and sEqFragmentExpanded -> closeEqualizerFragment(isAnimation = mGoPreference.isAnimations)
            sEqFragmentExpanded and sDetailsFragmentExpanded -> if (sCloseDetailsFragment) {
                closeDetailsFragment(isAnimation = mGoPreference.isAnimations)
            } else {
                closeEqualizerFragment(isAnimation = mGoPreference.isAnimations)
            }
            sErrorFragmentExpanded -> super.onBackPressed()
            else -> if (mMainActivityBinding.viewPager2.currentItem != 0) {
                mMainActivityBinding.viewPager2.currentItem = 0
            } else {
                onCloseActivity()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        sAllowCommit = false
        val bundle = bundleOf(
            GoConstants.RESTORE_FRAGMENT to mMainActivityBinding.viewPager2.currentItem,
            GoConstants.RESTORE_DIALOG_TYPE to sDialogToRestore
        )
        outState.putAll(bundle)
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
            if (sBound) {
                unbindService(connection)
            }
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
        sDialogToRestore = when {
            mNpDialog != null -> NowPlaying.TAG_MODAL
            mQueueDialog != null -> RecyclerSheet.QUEUE_TYPE
            else -> null
        }

        mNpDialog?.dismiss()
        mQueueDialog?.dismiss()
        mFavoritesDialog?.dismiss()
        mSleepTimerDialog?.dismiss()

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
        if (Versioning.isMarshmallow()) {
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
        newBase?.let { ctx ->
            // Be sure that prefs are initialized
            GoPreferences.initPrefs(newBase)
            if (mGoPreference.locale == null) {
                val localeUpdatedContext = ContextUtils.updateLocale(ctx, Locale.getDefault())
                super.attachBaseContext(localeUpdatedContext)
            } else {
                val locale = Locale.forLanguageTag(mGoPreference.locale!!)
                val localeUpdatedContext = ContextUtils.updateLocale(ctx, locale)
                super.attachBaseContext(localeUpdatedContext)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(Theming.resolveTheme(this))

        mMainActivityBinding = MainActivityBinding.inflate(layoutInflater)
        mPlayerControlsPanelBinding = PlayerControlsPanelBinding.bind(mMainActivityBinding.root)

        setContentView(mMainActivityBinding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window?.navigationBarColor = Theming.resolveColorAttr(this, R.attr.main_bg)
            WindowCompat.setDecorFitsSystemWindows(window, true)
            mMainActivityBinding.root.applyEdgeToEdge()
        }

        sAllowCommit = true

        sRestoreSettingsFragment = intent.getBooleanExtra(
            GoConstants.RESTORE_SETTINGS_FRAGMENT,
            false
        )

        savedInstanceState?.run {
            sTabToRestore = getInt(GoConstants.RESTORE_FRAGMENT, -1)
            sDialogToRestore = getString(GoConstants.RESTORE_DIALOG_TYPE, null)
        }

        if (Permissions.hasToAskForReadStoragePermission(this)) {
            Permissions.manageAskForReadStoragePermission(this)
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

            // Be sure that prefs are initialized
            GoPreferences.initPrefs(this)

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
        mMainActivityBinding.viewPager2.offscreenPageLimit =
            mGoPreference.activeTabs.toList().size.minus(1)
        mMainActivityBinding.viewPager2.adapter = pagerAdapter
        mMainActivityBinding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                mArtistsFragment?.stopActionMode()
                mFoldersFragment?.stopActionMode()
            }
        })
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

        val accent = Theming.resolveThemeColor(resources)
        val alphaAccentColor = ColorUtils.setAlphaComponent(accent, 200)

        with(mPlayerControlsPanelBinding.tabLayout) {

            tabIconTint = ColorStateList.valueOf(alphaAccentColor)

            TabLayoutMediator(this, mMainActivityBinding.viewPager2) { tab, position ->
                tab.setIcon(Theming.getTabIcon(mGoPreference.activeTabs.toList()[position]))
            }.attach()

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    tab.icon?.setTint(accent)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    closeRestoreFragments()
                    tab.icon?.setTint(alphaAccentColor)
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    closeRestoreFragments()
                }
            })
        }

        Log.d("stab", sRestoreSettingsFragment.toString())
        mMainActivityBinding.viewPager2.setCurrentItem(
            when {
                sTabToRestore != -1 && !sRestoreSettingsFragment -> sTabToRestore
                sRestoreSettingsFragment -> mMainActivityBinding.viewPager2.offscreenPageLimit
                else -> 0
            },
            false
        )
        mPlayerControlsPanelBinding.tabLayout.getTabAt(
            mMainActivityBinding.viewPager2.currentItem
        )?.run {
            select()
            icon?.setTint(Theming.resolveThemeColor(resources))
        }
    }

    private fun initFragmentAt(position: Int) : Fragment {
        when (mGoPreference.activeTabs.toList()[position]) {
            GoConstants.ARTISTS_TAB -> mArtistsFragment = MusicContainersFragment.newInstance(GoConstants.ARTIST_VIEW)
            GoConstants.ALBUM_TAB -> mAlbumsFragment = MusicContainersFragment.newInstance(GoConstants.ALBUM_VIEW)
            GoConstants.SONGS_TAB -> mAllMusicFragment = AllMusicFragment.newInstance()
            GoConstants.FOLDERS_TAB -> mFoldersFragment = MusicContainersFragment.newInstance(GoConstants.FOLDER_VIEW)
            else -> mSettingsFragment = SettingsFragment.newInstance()
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

    private fun getFragmentForIndex(index: Int) = when (mGoPreference.activeTabs.toList()[index]) {
        GoConstants.ARTISTS_TAB -> mArtistsFragment ?: initFragmentAt(index)
        GoConstants.ALBUM_TAB -> mAlbumsFragment ?: initFragmentAt(index)
        GoConstants.SONGS_TAB -> mAllMusicFragment ?: initFragmentAt(index)
        GoConstants.FOLDERS_TAB -> mFoldersFragment ?: initFragmentAt(index)
        else -> mSettingsFragment ?: initFragmentAt(index)
    }

    private fun closeRestoreFragments() {
        if (sAllowCommit) {
            with(supportFragmentManager) {
                if (sEqFragmentExpanded) {
                    goBackFromFragmentNow(mEqualizerFragment)
                    mEqualizerFragment = null
                    if (sTabToRestore != -1) {
                        onOpenEqualizer()
                    }
                }
                if (sDetailsFragmentExpanded) {
                    goBackFromFragmentNow(mDetailsFragment)
                    mDetailsFragment = null
                    if (sTabToRestore != -1) {
                        onOpenPlayingArtistAlbum()
                    }
                }
            }
            sDialogToRestore?.let { dialogType ->
               when (dialogType) {
                   NowPlaying.TAG_MODAL -> openNowPlayingFragment()
                   else -> openQueueFragment()
               }
                sDialogToRestore = null
            }
        }
    }

    private fun openNowPlayingFragment() {
        if (checkIsPlayer(showError = true) && mMediaPlayerHolder.isCurrentSong && mNpDialog == null) {
            mNpDialog = NowPlaying.newInstance().apply {
                show(supportFragmentManager, NowPlaying.TAG_MODAL)
                onNowPlayingCancelled = {
                    mNpDialog = null
                }
            }
        }
    }

    private fun openQueueFragment() {
        if (checkIsPlayer(showError = false) && mMediaPlayerHolder.queueSongs.isNotEmpty() && mQueueDialog == null) {
            mQueueDialog = RecyclerSheet.newInstance(RecyclerSheet.QUEUE_TYPE).apply {
                show(supportFragmentManager, RecyclerSheet.TAG_MODAL_RV)
                onQueueCancelled = {
                    mQueueDialog = null
                }
            }
        } else {
            R.string.error_no_queue.toToast(this@MainActivity)
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
                    MusicUtils.getPlayingAlbumPosition(
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
                        if (sAllowCommit) {
                            supportFragmentManager.goBackFromFragmentNow(mDetailsFragment)
                        }
                        sRevealAnimationRunning = false
                    }
                }
            }
        } else {
            if (sAllowCommit) {
                supportFragmentManager.goBackFromFragmentNow(mDetailsFragment)
            }
        }
    }

    private fun initMediaButtons() {

        mPlayerControlsPanelBinding.playPauseButton.setOnClickListener { mMediaPlayerHolder.resumeOrPause() }

        with(mPlayerControlsPanelBinding.queueButton) {
            safeClickListener {
                openQueueFragment()
            }
            setOnLongClickListener {
                if (checkIsPlayer(showError = true) && mMediaPlayerHolder.queueSongs.isNotEmpty()) {
                    Dialogs.showClearQueueDialog(this@MainActivity, mMediaPlayerHolder)
                }
                return@setOnLongClickListener true
            }
        }

        with(mPlayerControlsPanelBinding.favoritesButton) {
            safeClickListener {
                if (!mGoPreference.favorites.isNullOrEmpty() && mFavoritesDialog == null) {
                    mFavoritesDialog = RecyclerSheet.newInstance(RecyclerSheet.FAV_TYPE).apply {
                        show(supportFragmentManager, RecyclerSheet.TAG_MODAL_RV)
                        onFavoritesDialogCancelled = {
                            mFavoritesDialog = null
                        }
                    }
                } else {
                    R.string.error_no_favorites.toToast(this@MainActivity)
                }
            }
            setOnLongClickListener {
                if (!mGoPreference.favorites.isNullOrEmpty()) {
                    Dialogs.showClearFavoritesDialog(
                        this@MainActivity
                    )
                }
                return@setOnLongClickListener true
            }
        }

        onFavoritesUpdated(clear = false)

        with(mPlayerControlsPanelBinding.playingSongContainer) {
            safeClickListener {
                openNowPlayingFragment()
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
        synchronized(mMediaPlayerInterface.onBackupSong()) {
            if (isThemeChanged) {
                AppCompatDelegate.setDefaultNightMode(
                    Theming.getDefaultNightMode(
                        this
                    )
                )
            } else {
                Theming.applyChanges(this, restoreSettings = true)
            }
        }
    }

    override fun onPlaybackSpeedToggled() {
        //avoid having user stuck at selected playback speed
        val isPlaybackPersisted = mGoPreference.playbackSpeedMode != GoConstants.PLAYBACK_SPEED_ONE_ONLY
        mMediaPlayerHolder.setPlaybackSpeed(if (isPlaybackPersisted) {
            mGoPreference.latestPlaybackSpeed
        } else {
            1.0F
        })
    }

    private fun updatePlayingStatus() {
        if (isMediaPlayerHolder) {
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
    }

    override fun onFavoriteAddedOrRemoved() {
        if (isMediaPlayerHolder) {
            onFavoritesUpdated(clear = false)
            mNpDialog?.updateNpFavoritesIcon(mMediaPlayerHolder)
        }
    }

    override fun onUpdatePositionFromNP(position: Int) {
        mPlayerControlsPanelBinding.songProgress.setProgressCompat(position, true)
    }

    override fun onFavoritesUpdated(clear: Boolean) {

        val favorites = mGoPreference.favorites?.toMutableList()

        if (clear) {
            favorites?.clear()
            mGoPreference.favorites = null
            mFavoritesDialog?.dismissAllowingStateLoss()
        }

        val favoritesButtonColor = if (favorites.isNullOrEmpty()) {
            mPlayerControlsPanelBinding.favoritesButton.setImageResource(R.drawable.ic_favorite_empty)
            Theming.resolveWidgetsColorNormal(this)
        } else {
            mPlayerControlsPanelBinding.favoritesButton.setImageResource(R.drawable.ic_favorite)
            Theming.resolveThemeColor(resources)
        }
        mPlayerControlsPanelBinding.favoritesButton.updateIconTint(
            favoritesButtonColor
        )
    }

    private fun restorePlayerStatus() {
        // If we are playing and the activity was restarted
        // update the controls panel
        if (isMediaPlayerHolder) {
            with(mMediaPlayerHolder) {
                if (isMediaPlayer && isPlaying) {
                    onRestartSeekBarCallback()
                    updatePlayingInfo(restore = true)
                } else {
                    isSongFromPrefs = mGoPreference.latestPlayedSong != null

                    var isQueueRestored = mGoPreference.isQueue
                    if (!mGoPreference.queue.isNullOrEmpty()) {
                        queueSongs = mGoPreference.queue?.toMutableList()!!
                        setQueueEnabled(enabled = true, canSkip = false)
                    }

                    val song = if (isSongFromPrefs) {
                        val songIsAvailable = songIsAvailable(mGoPreference.latestPlayedSong)
                        if (!songIsAvailable.first && isQueueRestored != null) {
                            mGoPreference.isQueue = null
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
                        mMusicViewModel.getRandomMusic()
                    }

                    song?.let { restoredSong ->

                        val songs = MusicUtils.getAlbumSongs(
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

            if (::mPlayerService.isInitialized) {
                //stop foreground if coming from pause state
                with(mPlayerService) {
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
        val randomMusic = mMusicViewModel.getRandomMusic()
        mGoPreference.latestPlayedSong = randomMusic
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
                        MusicUtils.getPlayingAlbumPosition(
                            mMediaPlayerHolder,
                            selectedArtistOrFolder,
                            mMusicViewModel.deviceAlbumsByArtist,
                        )
                    )
                }
                mDetailsFragment?.scrollToPlayingSong(mMediaPlayerHolder.currentSong?.id)
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
            Dialogs.stopPlaybackDialog(this, mMediaPlayerHolder)
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
        if (isMediaPlayerHolder) {
            with(getSongSource()) {
                openDetailsFragment(
                    this,
                    mMediaPlayerHolder.launchedBy,
                    mMediaPlayerHolder.currentSong?.id
                )
            }
        }
    }

    override fun onArtistOrFolderSelected(artistOrFolder: String, launchedBy: String) {
        if (isMediaPlayerHolder) {
            openDetailsFragment(
                artistOrFolder,
                launchedBy,
                mMediaPlayerHolder.currentSong?.id
            )
        }
    }

    private fun preparePlayback() {
        if (isMediaPlayerHolder) {
            runBlocking {
                launch {
                    println("${Thread.currentThread()} has run.")
                    if (::mPlayerService.isInitialized && !mPlayerService.isRunning) {
                        startService(mBindingIntent)
                    }
                }
            }
            mMediaPlayerHolder.initMediaPlayer(mMediaPlayerHolder.currentSong)
        }
    }

    override fun onSongSelected(song: Music?, songs: List<Music>?, songLaunchedBy: String) {
        if (isMediaPlayerHolder) {
            with(mMediaPlayerHolder) {
                if (isSongFromPrefs) {
                    isSongFromPrefs = false
                }
                if (!isPlay) {
                    isPlay = true
                }
                if (isQueue != null) {
                    setQueueEnabled(enabled = false, canSkip = false)
                }
                val albumSongs = songs ?: MusicUtils.getAlbumSongs(
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
                mMediaPlayerHolder.onOpenEqualizerCustom()
                if (!sEqFragmentExpanded && mEqualizerFragment == null) {
                    mEqualizerFragment = EqFragment.newInstance()
                    sCloseDetailsFragment = !sDetailsFragmentExpanded
                    if (sAllowCommit) {
                        supportFragmentManager.addFragment(
                            mEqualizerFragment,
                            GoConstants.EQ_FRAGMENT_TAG
                        )
                    }
                    mNpDialog?.dismissAllowingStateLoss()
                }
            } else {
                mMediaPlayerHolder.openEqualizer(this)
            }
        }
    }

    override fun onOpenSleepTimerDialog() {
        if (isMediaPlayerHolder) {
            if (mSleepTimerDialog == null) {
                mSleepTimerDialog = RecyclerSheet.newInstance(if (mMediaPlayerHolder.isSleepTimer) {
                    RecyclerSheet.SLEEPTIMER_ELAPSED_TYPE
                } else {
                    RecyclerSheet.SLEEPTIMER_TYPE
                }).apply {
                    show(supportFragmentManager, RecyclerSheet.TAG_MODAL_RV)
                    onSleepTimerEnabled = { enabled ->
                        updateSleepTimerIcon(isEnabled = enabled)
                    }
                    onSleepTimerDialogCancelled = {
                        mSleepTimerDialog = null
                    }
                }
            }
        }
    }

    private fun updateSleepTimerIcon(isEnabled: Boolean) {
        mDetailsFragment?.tintSleepTimerIcon(enabled = isEnabled)
        mArtistsFragment?.tintSleepTimerIcon(enabled = isEnabled)
        mAlbumsFragment?.tintSleepTimerIcon(enabled = isEnabled)
        mAllMusicFragment?.tintSleepTimerIcon(enabled = isEnabled)
        mFoldersFragment?.tintSleepTimerIcon(enabled = isEnabled)
    }

    private fun closeEqualizerFragment(isAnimation: Boolean) {
        if (isAnimation) {
            if (!sRevealAnimationRunning) {
                mEqualizerFragment?.onHandleBackPressed()?.run {
                    sRevealAnimationRunning = true
                    doOnEnd {
                        if (sAllowCommit) {
                            supportFragmentManager.goBackFromFragmentNow(mEqualizerFragment)
                        }
                        sRevealAnimationRunning = false
                        mEqualizerFragment = null
                    }
                }
            }
        } else {
            if (sAllowCommit) {
                supportFragmentManager.goBackFromFragmentNow(mEqualizerFragment)
            }
        }
    }

    override fun onAddToQueue(song: Music?) {
        if (isMediaPlayerHolder) {

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
        if (checkIsPlayer(showError = true) && !songs?.isEmpty()!!) {

            with(mMediaPlayerHolder) {

                setCanRestoreQueue()

                // don't add duplicates
                addSongsToNextQueuePosition(if (restoreQueueSong != songs.first()) {
                    queueSongs.removeAll(songs.toSet())
                    songs
                } else {
                    songs.minus(songs.first())
                })

                if (canRestoreQueue && restoreQueueSong == null) {
                    restoreQueueSong = forcePlay.second ?: songs.first()
                }

                    if (!isPlaying || forcePlay.first) {
                        startSongFromQueue(forcePlay.second ?: songs.first())
                    }
                }
        }
    }

    override fun onSongsShuffled(
        songs: List<Music>?,
        songLaunchedBy: String
    ) {
        val cutoff = 250
        songs?.run {
            onAddAlbumToQueue(
                if (size >= cutoff) {
                    this.shuffled().take(cutoff)
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
                    Theming.applyChanges(this, restoreSettings = true)
                }
            }
        }
    }

    override fun onAddToFilter(stringsToFilter: List<String>?) {

        stringsToFilter?.run {
            Lists.hideItems(this)
        }

        if (isMediaPlayerHolder) {
            // be sure to update queue, favorites and the controls panel
            MusicUtils.updateMediaPlayerHolderLists(mMediaPlayerHolder, this, mMusicViewModel.getRandomMusic())?.let { song ->
                val songs = MusicUtils.getAlbumSongs(
                    song.artist,
                    song.album,
                    mMusicViewModel.deviceAlbumsByArtist
                )
                with(mMediaPlayerHolder) {
                    isPlay = isPlaying
                    updateCurrentSong(song, songs, GoConstants.ARTIST_VIEW)
                    initMediaPlayer(song)
                }
                updatePlayingInfo(restore = false)
            }
        }
        Theming.applyChanges(this, restoreSettings = false)
    }

    override fun onFiltersCleared() {
        GoPreferences.getPrefsInstance().filters = null
        Theming.applyChanges(this, restoreSettings = false)
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
            if (isMediaPlayerHolder) {
                updatePlayingStatus()
                mNpDialog?.updatePlayingStatus(mMediaPlayerHolder)
                if (mMediaPlayerHolder.state != GoConstants.RESUMED && mMediaPlayerHolder.state != GoConstants.PAUSED) {
                    updatePlayingInfo(restore = false)
                    if (mMediaPlayerHolder.isQueue != null) {
                        mQueueDialog?.swapQueueSong(mMediaPlayerHolder.currentSong)
                    } else if (sDetailsFragmentExpanded) {
                        mDetailsFragment?.swapSelectedSong(
                            mMediaPlayerHolder.currentSong?.id
                        )
                    }
                }
            }
        }

        override fun onQueueEnabled() {
            mPlayerControlsPanelBinding.queueButton.updateIconTint(
                ContextCompat.getColor(this@MainActivity, R.color.widgetsColor)
            )
        }

        override fun onQueueStartedOrEnded(started: Boolean) {
            mPlayerControlsPanelBinding.queueButton.updateIconTint(
                when {
                    started -> Theming.resolveThemeColor(resources)
                    mMediaPlayerHolder.queueSongs.isEmpty() -> {
                        mQueueDialog?.dismissAllowingStateLoss()
                        Theming.resolveWidgetsColorNormal(this@MainActivity)
                    }
                    else -> {
                        mQueueDialog?.dismissAllowingStateLoss()
                        ContextCompat.getColor(
                            this@MainActivity,
                            R.color.widgetsColor
                        )
                    }
                }
            )
        }

        override fun onBackupSong() {
            if (checkIsPlayer(showError = false) && !mMediaPlayerHolder.isPlaying) {
                mGoPreference.latestPlayedSong = mMediaPlayerHolder.currentSong?.toSavedMusic(mMediaPlayerHolder.playerPosition, mMediaPlayerHolder.launchedBy)
            }
        }

        override fun onUpdateSleepTimerCountdown(value: Long) {
            mSleepTimerDialog?.run {
                if (sheetType == RecyclerSheet.SLEEPTIMER_ELAPSED_TYPE) {
                    val newValue = value.toFormattedDuration(isAlbum = false, isSeekBar = true)
                    updateCountdown(newValue)
                }
            }
        }

        override fun onStopSleepTimer() {
            mSleepTimerDialog?.dismissAllowingStateLoss()
            updateSleepTimerIcon(isEnabled = false)
        }

        override fun onUpdateFavorites() {
            onFavoriteAddedOrRemoved()
        }
    }

    // ViewPager2 adapter class
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount() = mGoPreference.activeTabs.toList().size
        override fun createFragment(position: Int): Fragment = handleOnNavigationItemSelected(position)
    }
}
