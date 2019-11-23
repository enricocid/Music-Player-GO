package com.iven.musicplayergo

import android.Manifest
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
import androidx.core.app.ActivityCompat
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
import com.iven.musicplayergo.fragments.AllMusicFragment
import com.iven.musicplayergo.fragments.ArtistDetailsFragment
import com.iven.musicplayergo.fragments.ArtistsFragment
import com.iven.musicplayergo.fragments.SettingsFragment
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

@Suppress("UNUSED_PARAMETER")
class MainActivity : AppCompatActivity(), UIControlInterface {

    //fragments
    private lateinit var mArtistsFragment: ArtistsFragment
    private lateinit var mAllMusicFragment: AllMusicFragment
    private lateinit var mSettingsFragment: SettingsFragment

    //views
    private lateinit var mViewPager: ViewPager

    //settings/controls panel
    private lateinit var mPlayingAlbum: TextView
    private lateinit var mPlayingSong: TextView
    private lateinit var mSeekProgressBar: ProgressBar

    private lateinit var mPlayPauseButton: ImageView

    //now playing
    private lateinit var mNowPlayingDialog: MaterialDialog
    private lateinit var mFixedMusicBar: FixedMusicBar
    private lateinit var mSongTextNP: TextView
    private lateinit var mArtistTextNP: TextView
    private lateinit var mSongSeekTextNP: TextView
    private lateinit var mSongDurationTextNP: TextView
    private lateinit var mSkipPrevButtonNP: ImageView
    private lateinit var mPlayPauseButtonNP: ImageView
    private lateinit var mSkipNextButtonNP: ImageView
    private lateinit var mReplayNP: ImageView
    private lateinit var mVolumeSeekBarNP: SeekBar
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

    private var mNavigationArtist: String = "unknown"

    private lateinit var mSelectedArtistSongs: MutableList<Music>

    //the player
    private lateinit var mMediaPlayerHolder: MediaPlayerHolder

    //our PlayerService shit
    private lateinit var mPlayerService: PlayerService
    private var sBound: Boolean = false
    private lateinit var mBindingIntent: Intent

    //Defines callbacks for service binding, passed to bindService()
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            val binder = service as PlayerService.LocalBinder
            mPlayerService = binder.getService()
            sBound = true
            mMediaPlayerHolder = mPlayerService.mediaPlayerHolder!!
            mMediaPlayerHolder.mediaPlayerInterface = mediaPlayerInterface

            loadMusic()
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

    override fun onDestroy() {
        super.onDestroy()
        if (sBound) unbindService(connection)
        if (::mMediaPlayerHolder.isInitialized && !mMediaPlayerHolder.isPlaying && ::mPlayerService.isInitialized && mPlayerService.isRunning) {
            mPlayerService.stopForeground(true)
            stopService(mBindingIntent)
        }
    }

    //restore recycler views state
    override fun onResume() {
        super.onResume()
        if (::mMediaPlayerHolder.isInitialized && mMediaPlayerHolder.isMediaPlayer) mMediaPlayerHolder.onResumeActivity()
    }

    //save recycler views state
    override fun onPause() {
        super.onPause()
        if (::mMediaPlayerHolder.isInitialized && mMediaPlayerHolder.isMediaPlayer) mMediaPlayerHolder.onPauseActivity()
    }

    //manage request permission result, continue loading ui if permissions was granted
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) showPermissionRationale() else doBindService()
    }

    override fun onSongSelected(song: Music, songs: List<Music>) {
        startPlayback(song, songs)
    }

    override fun onArtistSelected(artist: String) {
        openArtistDetailsFragment(artist)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //set ui theme
        setTheme(ThemeHelper.getAccentedTheme().first)
        ThemeHelper.applyTheme(this, goPreferences.theme!!)

        setContentView(R.layout.main_activity)

        //init views
        getViews()
        setupPlayerControls()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) checkPermission() else doBindService()
    }

    @TargetApi(23)
    private fun checkPermission() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        )
            showPermissionRationale() else doBindService()
    }

    private fun showPermissionRationale() {

        MaterialDialog(this).show {

            cancelOnTouchOutside(false)
            cornerRadius(res = R.dimen.md_corner_radius)
            title(R.string.app_name)
            message(R.string.perm_rationale)
            positiveButton {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    2588
                )
            }
            negativeButton {
                Utils.makeToast(
                    this@MainActivity,
                    R.string.perm_rationale
                )
                dismiss()
                finishAndRemoveTask()
            }
        }
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
                        mSelectedArtistSongs,
                        mAllDeviceSongs
                    ) != null
                ) {

                    val song =
                        MusicUtils.getSongForIntent(path, mSelectedArtistSongs, mAllDeviceSongs)!!

                    //get album songs and sort them
                    val albumSongs =
                        MusicUtils.getAlbumFromList(song.album, mMusic[song.artist]!!).first
                            .music?.sortedBy { albumSong -> albumSong.track }

                    startPlayback(song, albumSongs)
                } else {
                    Utils.makeToast(
                        this@MainActivity,
                        R.string.error_unknown
                    )
                    finishAndRemoveTask()
                }
            }
        } catch (e: Exception) {
            Utils.makeToast(
                this@MainActivity,
                R.string.error_unknown
            )
            finishAndRemoveTask()
        }
    }

    private fun loadMusic() {

        mViewModel.loadMusic(this).observe(this, Observer { hasLoaded ->

            //setup all the views if there's something
            if (hasLoaded && musicLibrary.allAlbumsForArtist.isNotEmpty()) {

                mArtistsFragment = ArtistsFragment.newInstance()
                mAllMusicFragment = AllMusicFragment.newInstance()
                mSettingsFragment = SettingsFragment.newInstance()

                val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
                mViewPager.adapter = pagerAdapter

                mAllDeviceSongs = musicLibrary.allSongsUnfiltered
                mMusic = musicLibrary.allAlbumsForArtist

                mNavigationArtist = Utils.getSortedList(
                    goPreferences.artistsSorting,
                    musicLibrary.allAlbumsForArtist.keys.toMutableList(),
                    musicLibrary.allAlbumsForArtist.keys.toMutableList()
                )[0]

                //let's get intent from external app and open the song,
                //else restore the player (normal usage)
                if (intent != null && Intent.ACTION_VIEW == intent.action && intent.data != null)
                    handleIntent(intent)
                else
                    restorePlayerStatus()

            } else {
                Utils.makeToast(
                    this@MainActivity,
                    R.string.error_no_music
                )
                finish()
            }
        })
    }

    private fun getViews() {

        mViewPager = pager

        //controls panel
        mPlayingSong = playing_song
        mPlayingAlbum = playing_album
        mSeekProgressBar = song_progress
        mPlayPauseButton = play_pause_button
    }

    private fun handleOnNavigationItemSelected(itemId: Int): Fragment {

        return when (itemId) {
            0 -> mArtistsFragment
            1 -> mAllMusicFragment
            else -> mSettingsFragment
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int = 3

        override fun getItem(position: Int): Fragment {
            return handleOnNavigationItemSelected(position)
        }
    }

    private fun setupPlayerControls() {

        //this makes the text scrolling when too long
        mPlayingSong.isSelected = true
        mPlayingAlbum.isSelected = true

        mPlayPauseButton.setOnClickListener { resumeOrPause() }
    }

    private fun openArtistDetailsFragment(selectedArtist: String) {

        mNavigationArtist = selectedArtist

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.container,
                ArtistDetailsFragment.newInstance(mNavigationArtist), ArtistDetailsFragment.TAG
            )
            .addToBackStack(null)
            .commit()
    }

    private fun startPlayback(song: Music, album: List<Music>?) {
        if (::mPlayerService.isInitialized && !mPlayerService.isRunning) startService(mBindingIntent)

        mMediaPlayerHolder.setCurrentSong(song, album!!)
        mMediaPlayerHolder.initMediaPlayer(song)
    }

    override fun onShuffleSongs(songs: MutableList<Music>) {
        if (::mMediaPlayerHolder.isInitialized) {
            songs.shuffle()
            val song = songs[0]
            startPlayback(song, songs)
        }
    }

    private fun setRepeat() {
        if (checkIsPlayer()) {
            mMediaPlayerHolder.reset()
            updateResetStatus(false)
        }
    }

    private fun resumeOrPause() {
        if (checkIsPlayer()) {
            mMediaPlayerHolder.resumeOrPause()
        }
    }

    private fun skip(isNext: Boolean) {
        if (checkIsPlayer()) {
            if (isNext) mMediaPlayerHolder.skip(true) else mMediaPlayerHolder.instantReset()
        }
    }

    //interface to let MediaPlayerHolder update the UI media player controls
    val mediaPlayerInterface = object : MediaPlayerInterface {

        override fun onClose() {
            //stop service
            mPlayerService.stopForeground(true)
            stopService(mBindingIntent)

            //finish activity if visible
            finishAndRemoveTask()
        }

        override fun onPlaybackCompleted() {
            updateResetStatus(true)
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
            }
        }
    }

    private fun restorePlayerStatus() {
        if (::mMediaPlayerHolder.isInitialized) {
            //if we are playing and the activity was restarted
            //update the controls panel
            if (mMediaPlayerHolder.isMediaPlayer) {
                mMediaPlayerHolder.onResumeActivity()
                updatePlayingInfo(true)
            }
        }
    }

    //method to update info on controls panel
    private fun updatePlayingInfo(restore: Boolean) {

        val selectedSong = mMediaPlayerHolder.currentSong
        mSeekProgressBar.max = selectedSong!!.duration.toInt()

        mPlayingSong.text =
            MusicUtils.buildSpanned(
                getString(
                    R.string.playing_song,
                    selectedSong.artist,
                    selectedSong.title
                )
            )

        mPlayingAlbum.text = selectedSong.album

        updateResetStatus(false)

        if (isNowPlaying()) updateNowPlayingInfo()

        if (restore) {

            updatePlayingStatus(false)

            //stop foreground if coming from pause state
            if (mPlayerService.isRestoredFromPause) {
                mPlayerService.stopForeground(false)
                mPlayerService.musicNotificationManager.notificationManager.notify(
                    NOTIFICATION_ID,
                    mPlayerService.musicNotificationManager.notificationBuilder!!.build()
                )
                mPlayerService.isRestoredFromPause = false
            }
        }
    }

    private fun updateResetStatus(onPlaybackCompletion: Boolean) {
        if (isNowPlaying()) {

            val defaultIconsColor =
                ThemeHelper.resolveColorAttr(this, android.R.attr.textColorPrimary)
            when {
                onPlaybackCompletion -> ThemeHelper.updateIconTint(mReplayNP, defaultIconsColor)
                mMediaPlayerHolder.isReset -> ThemeHelper.updateIconTint(
                    mReplayNP,
                    ThemeHelper.resolveThemeAccent(this)
                )

                else -> ThemeHelper.updateIconTint(mReplayNP, defaultIconsColor)
            }
        }
    }

    private fun updatePlayingStatus(isNowPlaying: Boolean) {
        val isPlaying = mMediaPlayerHolder.state != PAUSED
        val drawable =
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        if (isNowPlaying) mPlayPauseButtonNP.setImageResource(drawable) else mPlayPauseButton.setImageResource(
            drawable
        )
    }

    private fun checkIsPlayer(): Boolean {
        val isPlayer = mMediaPlayerHolder.isMediaPlayer
        if (!isPlayer) EqualizerUtils.notifyNoSessionId(this)
        return isPlayer
    }

    fun openNowPlaying(view: View) {

        if (checkIsPlayer()) {

            mNowPlayingDialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

                cornerRadius(res = R.dimen.md_corner_radius)

                customView(R.layout.now_playing)

                mSongTextNP = getCustomView().findViewById(R.id.np_song)
                mSongTextNP.isSelected = true
                mArtistTextNP = getCustomView().findViewById(R.id.np_artist_album)
                mArtistTextNP.isSelected = true
                mFixedMusicBar = getCustomView().findViewById(R.id.np_fixed_music_bar)
                mSongSeekTextNP = getCustomView().findViewById(R.id.np_seek)
                mSongDurationTextNP = getCustomView().findViewById(R.id.np_duration)

                mSkipPrevButtonNP = getCustomView().findViewById(R.id.np_skip_prev)
                mSkipPrevButtonNP.setOnClickListener { skip(false) }

                mPlayPauseButtonNP = getCustomView().findViewById(R.id.np_play)
                mPlayPauseButtonNP.setOnClickListener { resumeOrPause() }

                mSkipNextButtonNP = getCustomView().findViewById(R.id.np_skip_next)
                mSkipNextButtonNP.setOnClickListener { skip(true) }

                mReplayNP = getCustomView().findViewById(R.id.np_replay)
                mReplayNP.setOnClickListener { setRepeat() }

                mVolumeSeekBarNP = getCustomView().findViewById(R.id.np_volume_seek)
                mVolumeNP = getCustomView().findViewById(R.id.np_volume)

                setupPreciseVolumeHandler()
                setFixedMusicBarProgressListener()

                updateNowPlayingInfo()

                onDismiss {
                    mFixedMusicBar.removeAllListener()
                }
            }
        }
    }

    private fun updateNowPlayingInfo() {
        val selectedSong = mMediaPlayerHolder.currentSong
        val selectedSongDuration = selectedSong?.duration!!

        mSongTextNP.text = selectedSong.title
        mArtistTextNP.text =
            MusicUtils.buildSpanned(
                getString(
                    R.string.artist_and_album_np,
                    selectedSong.artist,
                    selectedSong.album
                )
            )

        mSongDurationTextNP.text = MusicUtils.formatSongDuration(selectedSongDuration)

        mFixedMusicBar.loadFrom(selectedSong.path, selectedSong.duration.toInt())

        updatePlayingStatus(true)
    }

    private fun isNowPlaying(): Boolean {
        return ::mNowPlayingDialog.isInitialized && mNowPlayingDialog.isShowing
    }

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
                            ThemeHelper.resolveThemeAccent(this@MainActivity)
                        )
                    }
                    mSongSeekTextNP.text =
                        MusicUtils.formatSongDuration(musicBar.position.toLong())
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

    fun openEqualizer(view: View) {
        if (EqualizerUtils.hasEqualizer(this)) {
            if (checkIsPlayer()) mMediaPlayerHolder.openEqualizer(this)
        } else {
            Utils.makeToast(
                this@MainActivity,
                R.string.no_eq
            )
        }
    }

    private fun setupPreciseVolumeHandler() {
        if (checkIsPlayer()) {

            var isUserSeeking = false
            val currentProgress = mMediaPlayerHolder.currentVolumeInPercent

            mVolumeSeekBarNP.progress = currentProgress
            mVolumeSeekBarNP.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    if (isUserSeeking) {

                        mMediaPlayerHolder.setPreciseVolume(i)
                        ThemeHelper.updateIconTint(
                            mVolumeNP,
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
                        mVolumeNP,
                        ThemeHelper.resolveColorAttr(
                            this@MainActivity,
                            android.R.attr.textColorPrimary
                        )
                    )
                }
            })
        }
    }
}
