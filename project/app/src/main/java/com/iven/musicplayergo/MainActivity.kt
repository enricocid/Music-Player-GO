package com.iven.musicplayergo

import android.Manifest
import android.animation.Animator
import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.recyclical.datasource.DataSource
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.fragments.AllMusicFragment
import com.iven.musicplayergo.fragments.ArtistsFragment
import com.iven.musicplayergo.fragments.SettingsFragment
import com.iven.musicplayergo.music.Album
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.music.MusicViewModel
import com.iven.musicplayergo.player.*
import com.iven.musicplayergo.ui.*
import kotlinx.android.synthetic.main.artist_details.*
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.player_controls_panel.*
import kotlinx.android.synthetic.main.player_seek.*
import kotlin.math.hypot

@Suppress("UNUSED_PARAMETER")
class MainActivity : AppCompatActivity(), UIControlInterface {

    //fragments
    private lateinit var mArtistsFragment: ArtistsFragment
    private lateinit var mAllMusicFragment: AllMusicFragment
    private lateinit var mSettingsFragment: SettingsFragment

    //preferences
    private var sThemeInverted: Boolean = false
    private var mAccent: Int = R.color.blue

    //views
    private lateinit var mViewPager: ViewPager

    //RecyclerViews
    private lateinit var mAlbumsRecyclerView: RecyclerView
    private lateinit var mSongsRecyclerView: RecyclerView

    //settings/controls panel
    private lateinit var mControlsContainer: LinearLayout

    private lateinit var mPlayerInfoView: View
    private lateinit var mPlayingAlbum: TextView
    private lateinit var mPlayingSong: TextView
    private lateinit var mSeekBar: SeekBar
    private lateinit var mSongPosition: TextView
    private lateinit var mSongDuration: TextView
    private lateinit var mSkipPrevButton: ImageView
    private lateinit var mPlayPauseButton: ImageView
    private lateinit var mSkipNextButton: ImageView

    //artists details
    private lateinit var mArtistDetails: LinearLayout
    private lateinit var mArtistDetailsTitle: TextView
    private lateinit var mArtistsDetailsDiscCount: TextView
    private lateinit var mArtistsDetailsSelectedDisc: TextView
    private lateinit var mArtistDetailsSelectedDiscYear: TextView

    //music player things

    //view model
    private val mViewModel: MusicViewModel by lazy {
        ViewModelProviders.of(this).get(MusicViewModel::class.java)
    }

    private lateinit var mAllDeviceSongs: MutableList<Music>

    //booleans
    private var sUserIsSeeking = false
    private var sArtistDiscographyExpanded: Boolean = false

    //music
    private lateinit var mMusic: Map<String, List<Album>>

    private var mNavigationArtist: String? = "unknown"

    private lateinit var mSelectedArtistSongs: MutableList<Music>
    private lateinit var mSelectedArtistAlbums: List<Album>
    private lateinit var mSelectedAlbumsDataSource: DataSource<Any>
    private lateinit var mAlbumSongsDataSource: DataSource<Any>
    private var mSelectedAlbum: Album? = null

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

    //manage bottom panel state on back pressed
    override fun onBackPressed() {
        if (sArtistDiscographyExpanded) revealArtistDetails(false) else super.onBackPressed()
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

    override fun onSongSelected(song: Music, album: List<Music>) {
        startPlayback(song, album)
    }

    override fun onArtistSelected(artist: String) {

        if (mNavigationArtist != artist) {
            mNavigationArtist = artist
            setArtistDetails()
            revealArtistDetails(true)
        } else {
            revealArtistDetails(true)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && goPreferences.isImmersive) ThemeHelper.goImmersive(
            this,
            hasFocus
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //set ui theme
        sThemeInverted = ThemeHelper.isThemeNight()
        mAccent = goPreferences.accent

        setTheme(ThemeHelper.getAccent(mAccent).first)
        ThemeHelper.applyTheme(this, goPreferences.theme!!)

        setContentView(R.layout.main_activity)

        //init views
        getViews()
        setupViews()

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
                    R.string.perm_rationale,
                    R.drawable.ic_error,
                    R.color.red
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
                        R.string.error_unknown,
                        R.drawable.ic_error,
                        R.color.red
                    )
                    finishAndRemoveTask()
                }
            }
        } catch (e: Exception) {
            Utils.makeToast(
                this@MainActivity,
                R.string.error_unknown,
                R.drawable.ic_error,
                R.color.red
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

                setArtistDetails()

                //let's get intent from external app and open the song,
                //else restore the player (normal usage)
                if (intent != null && Intent.ACTION_VIEW == intent.action && intent.data != null)
                    handleIntent(intent)
                else
                    restorePlayerStatus()

            } else {
                Utils.makeToast(
                    this@MainActivity,
                    R.string.error_no_music,
                    R.drawable.ic_error,
                    R.color.red
                )
                finish()
            }
        })
    }

    private fun getViews() {

        mViewPager = pager

        //recycler views
        mAlbumsRecyclerView = albums_rv
        mSongsRecyclerView = songs_rv

        //controls panel
        mControlsContainer = controls_container
        //mBottomSheetBehavior = BottomSheetBehavior.from(design_bottom_sheet)
        mPlayerInfoView = player_info
        mPlayingSong = playing_song
        mPlayingAlbum = playing_album
        mSeekBar = seekTo
        mSongPosition = song_position
        mSongDuration = duration
        mSkipPrevButton = skip_prev_button
        mPlayPauseButton = play_pause_button
        mSkipNextButton = skip_next_button

        //artist details
        mArtistDetails = artist_details
        mArtistDetailsTitle = selected_discography_artist
        mArtistsDetailsDiscCount = selected_artist_album_count
        mArtistsDetailsSelectedDisc = selected_disc
        mArtistDetailsSelectedDiscYear = selected_disc_year
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

    private fun setupViews() {

        close_button.setOnClickListener { revealArtistDetails(!sArtistDiscographyExpanded) }

        setupPlayerControls()
        initializeSeekBar()
    }

    private fun setupPlayerControls() {
        mPlayerInfoView.setOnClickListener { handlePlayerInfo() }

        //this makes the text scrolling when too long
        mPlayingSong.isSelected = true
        mPlayingAlbum.isSelected = true

        mSkipPrevButton.setOnClickListener { skip(false) }
        mSkipPrevButton.setOnLongClickListener {
            setRepeat()
            return@setOnLongClickListener false
        }
        mPlayPauseButton.setOnClickListener { resumeOrPause() }
        mSkipNextButton.setOnClickListener { skip(true) }

        shuffle_button.setOnClickListener {
            if (::mMediaPlayerHolder.isInitialized) {
                if (!mSeekBar.isEnabled) mSeekBar.isEnabled = true
                val albumSongs = mSelectedAlbum?.music
                albumSongs?.shuffle()
                startPlayback(albumSongs!![0], albumSongs)
            }
        }

        volume.setOnLongClickListener {
            openEqualizer()
            return@setOnLongClickListener true
        }
    }

    private fun initializeSeekBar() {
        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            val defaultPositionColor = mSongPosition.currentTextColor
            var userSelectedPosition = 0

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                sUserIsSeeking = true
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    userSelectedPosition = progress
                    mSongPosition.setTextColor(
                        ThemeHelper.getColor(
                            this@MainActivity,
                            mAccent,
                            R.color.blue
                        )
                    )
                }
                mSongPosition.text = MusicUtils.formatSongDuration(progress.toLong())
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (sUserIsSeeking) {
                    mSongPosition.setTextColor(defaultPositionColor)
                }
                sUserIsSeeking = false
                mMediaPlayerHolder.seekTo(userSelectedPosition)
            }
        })
    }

    private fun setArtistDetails() {

        mSelectedArtistAlbums = musicLibrary.allAlbumsForArtist[mNavigationArtist]!!

        //set the titles and subtitles
        mArtistDetailsTitle.text = mNavigationArtist
        mArtistsDetailsDiscCount.text = getString(
            R.string.artist_info,
            mSelectedArtistAlbums.size,
            musicLibrary.allSongsForArtist.getValue(mNavigationArtist).size
        )

        //set the albums list
        //one-time adapter initialization

        swapAlbums(true, mSelectedArtistAlbums[0])
    }

    private fun swapAlbums(sCreateDataSource: Boolean, selectedAlbum: Album?) {

        mSelectedAlbum = selectedAlbum
        mArtistsDetailsSelectedDisc.text = mSelectedAlbum?.title
        mArtistDetailsSelectedDiscYear.text = selectedAlbum?.year

        val songs = mSelectedAlbum?.music!!
        if (songs.size > 1) songs.sortBy { it.track }

        if (sCreateDataSource) {

            mAlbumSongsDataSource = dataSourceOf(songs)
            mSelectedAlbumsDataSource = dataSourceOf(mSelectedArtistAlbums)

            mAlbumsRecyclerView.setup {
                withDataSource(mSelectedAlbumsDataSource)
                withLayoutManager(
                    LinearLayoutManager(
                        this@MainActivity,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                )

                withItem<Album, AlbumsViewHolder>(R.layout.album_item) {
                    onBind(::AlbumsViewHolder) { _, item ->
                        // AlbumsViewHolder is `this` here
                        album.text = item.title
                        year.text = item.year

                        checkbox.visibility =
                            if (mSelectedAlbum?.title != item.title) View.GONE else View.VISIBLE
                    }

                    onClick {

                        if (mSelectedAlbum?.title != item.title) {

                            mAlbumsRecyclerView.adapter?.notifyItemChanged(
                                MusicUtils.getAlbumFromList(
                                    item.title,
                                    mSelectedArtistAlbums
                                ).second
                            )

                            mAlbumsRecyclerView.adapter?.notifyItemChanged(
                                MusicUtils.getAlbumFromList(
                                    mSelectedAlbum?.title,
                                    mSelectedArtistAlbums
                                ).second
                            )
                            swapAlbums(false, item)
                        }
                    }
                }
            }


            // setup{} is an extension method on RecyclerView
            mSongsRecyclerView.setup {
                // item is a `val` in `this` here
                withDataSource(mAlbumSongsDataSource)
                withItem<Music, GenericViewHolder>(R.layout.generic_item) {
                    onBind(::GenericViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        title.text = getString(
                            R.string.track_song,
                            MusicUtils.formatSongTrack(item.track),
                            item.title
                        )
                        subtitle.text = MusicUtils.formatSongDuration(item.duration)
                    }

                    onClick {
                        onSongSelected(
                            item,
                            MusicUtils.getAlbumFromList(
                                item.album,
                                mSelectedArtistAlbums
                            ).first.music!!.toList()
                        )
                    }
                }
            }
        } else {
            mSelectedAlbumsDataSource.set(mSelectedArtistAlbums)
            mAlbumSongsDataSource.set(songs)
            mSongsRecyclerView.scrollToPosition(0)
        }
    }

    private fun startPlayback(song: Music, album: List<Music>?) {
        if (!mSeekBar.isEnabled) mSeekBar.isEnabled = true

        if (::mPlayerService.isInitialized && !mPlayerService.isRunning) startService(mBindingIntent)

        mMediaPlayerHolder.setCurrentSong(song, album!!)
        mMediaPlayerHolder.initMediaPlayer(song)
    }

    override fun onShuffleSongs() {
        if (::mMediaPlayerHolder.isInitialized) {
            val songs = if (sArtistDiscographyExpanded) mSelectedArtistSongs else mAllDeviceSongs
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
                mSeekBar.progress = position
            }
        }

        override fun onStateChanged() {
            updatePlayingStatus()
            if (mMediaPlayerHolder.state != RESUMED && mMediaPlayerHolder.state != PAUSED) {
                updatePlayingInfo(false)
            }
        }
    }

    private fun restorePlayerStatus() {
        if (::mMediaPlayerHolder.isInitialized) {
            mSeekBar.isEnabled = mMediaPlayerHolder.isMediaPlayer
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
        val duration = selectedSong!!.duration
        mSeekBar.max = duration.toInt()
        mSongDuration.text = MusicUtils.formatSongDuration(duration)
        mPlayingSong.text =
            MusicUtils.buildSpanned(
                getString(
                    R.string.playing_song,
                    selectedSong.artist,
                    selectedSong.title
                )
            )

        mSelectedAlbum =
            MusicUtils.getAlbumFromList(selectedSong.album, mSelectedArtistAlbums).first
        mPlayingAlbum.text = selectedSong.album

        updateResetStatus(false)

        if (restore) {

            mSongPosition.text =
                MusicUtils.formatSongDuration(mMediaPlayerHolder.playerPosition.toLong())
            mSeekBar.progress = mMediaPlayerHolder.playerPosition

            updatePlayingStatus()

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
        val themeColor = if (sThemeInverted) Color.WHITE else Color.BLACK
        val accent = ThemeHelper.getColor(
            this,
            mAccent,
            R.color.blue
        )
        val finalColor =
            if (onPlaybackCompletion) themeColor else if (mMediaPlayerHolder.isReset) accent else themeColor

        mSkipPrevButton.setColorFilter(
            finalColor, PorterDuff.Mode.SRC_IN
        )
    }

    private fun updatePlayingStatus() {
        val drawable =
            if (mMediaPlayerHolder.state != PAUSED) R.drawable.ic_pause else R.drawable.ic_play
        mPlayPauseButton.setImageResource(drawable)
    }

    private fun checkIsPlayer(): Boolean {
        val isPlayer = mMediaPlayerHolder.isMediaPlayer
        if (!isPlayer) EqualizerUtils.notifyNoSessionId(this)
        return isPlayer
    }

    private fun openEqualizer() {
        if (EqualizerUtils.hasEqualizer(this)) {
            if (checkIsPlayer()) mMediaPlayerHolder.openEqualizer(this)
        } else {
            Utils.makeToast(
                this@MainActivity,
                R.string.no_eq,
                R.drawable.ic_error,
                R.color.red
            )
        }
    }

    fun openVolDialog(view: View) {
        if (checkIsPlayer()) {

            MaterialDialog(this).show {
                var isUserSeeking = false
                val currentProgress = mMediaPlayerHolder.currentVolumeInPercent

                cornerRadius(res = R.dimen.md_corner_radius)
                title(text = getString(R.string.volume, currentProgress.toString()))
                customView(R.layout.precise_volume_dialog)

                val volSeekBar = getCustomView().findViewById<SeekBar>(R.id.vol_seekBar)
                volSeekBar.progress = currentProgress
                volSeekBar.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                        if (isUserSeeking) {
                            mMediaPlayerHolder.setPreciseVolume(i)
                            title(text = getString(R.string.volume, i.toString()))
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        isUserSeeking = true
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        isUserSeeking = false
                    }
                })
            }
        }
    }

    //method to reveal/hide artist details, it is a simple reveal animation
    private fun revealArtistDetails(show: Boolean) {

        val viewToRevealHeight = mViewPager.height
        val viewToRevealWidth = mAlbumsRecyclerView.width
        val viewToRevealHalfWidth = viewToRevealWidth / 2
        val radius = hypot(viewToRevealWidth.toDouble(), viewToRevealHeight.toDouble()).toFloat()
        val fromY = mViewPager.top / 2
        val startRadius = if (show) 0f else radius
        val finalRadius = if (show) radius else 0f

        val anim = ViewAnimationUtils.createCircularReveal(
            mArtistDetails,
            viewToRevealHalfWidth,
            fromY,
            startRadius,
            finalRadius
        )
        anim.duration = 500
        anim.addListener(revealAnimationListener(show))
        anim.start()
    }

    //reveal animation
    private fun revealAnimationListener(show: Boolean): Animator.AnimatorListener {

        return object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
                if (show) {
                    sArtistDiscographyExpanded = true
                    mArtistDetails.visibility = View.VISIBLE
                    mViewPager.visibility = View.INVISIBLE
                    mArtistDetails.isClickable = false
                }
            }

            override fun onAnimationEnd(animation: Animator?) {
                if (!show) {
                    sArtistDiscographyExpanded = false
                    mArtistDetails.visibility = View.INVISIBLE
                    mViewPager.visibility = View.VISIBLE
                    mArtistDetails.isClickable = true
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }
        }
    }

    //method to handle player info click
    private fun handlePlayerInfo() {
        //if we are playing a song the go to the played artist/album details
        if (::mMediaPlayerHolder.isInitialized && mMediaPlayerHolder.currentSong != null) {
            val currentSong = mMediaPlayerHolder.currentSong
            val album = currentSong!!.album
            val artist = currentSong.artist
            //do only if we are not on played artist/album details

            if (mNavigationArtist != artist) {

                onArtistSelected(artist!!)

                val playingAlbumInfo =
                    MusicUtils.getAlbumFromList(album, musicLibrary.allAlbumsForArtist[artist])

                swapAlbums(false, playingAlbumInfo.first)
                mAlbumsRecyclerView.scrollToPosition(playingAlbumInfo.second)

            } else {
                revealArtistDetails(!sArtistDiscographyExpanded)
            }
        } else {
            revealArtistDetails(!sArtistDiscographyExpanded)
        }
    }

    override fun onVisibleItemsUpdated() {
        if (::mArtistsFragment.isInitialized && mArtistsFragment.isAdded) mArtistsFragment.updateArtistsList()
    }
}
