package com.iven.musicplayergo

import android.animation.Animator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.view.*
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.iven.musicplayergo.adapters.AlbumsAdapter
import com.iven.musicplayergo.adapters.ArtistsAdapter
import com.iven.musicplayergo.adapters.ColorsAdapter
import com.iven.musicplayergo.adapters.SongsAdapter
import com.iven.musicplayergo.indexbar.IndexBarRecyclerView
import com.iven.musicplayergo.indexbar.IndexBarView
import com.iven.musicplayergo.music.Album
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.music.MusicViewModel
import com.iven.musicplayergo.player.*
import com.iven.musicplayergo.uihelpers.PreferencesHelper
import com.iven.musicplayergo.uihelpers.UIUtils
import kotlinx.android.synthetic.main.artist_details.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.player_controls_panel.*
import kotlinx.android.synthetic.main.player_seek.*
import kotlinx.android.synthetic.main.player_settings.*
import kotlinx.android.synthetic.main.search_toolbar.*

// the fragment initialization parameters
private const val ARG_INVERTED = "param_inverted"
private const val ARG_ACCENT = "param_accent"

class MainFragment : Fragment() {

    //context
    private lateinit var mActivity: AppCompatActivity

    //preferences
    private lateinit var mPreferencesHelper: PreferencesHelper
    private var sThemeInverted: Boolean? = false
    private var mAccent: Int? = R.color.blue
    private var sSearchEnabled: Boolean = true

    //views

    //RecyclerViews
    private lateinit var mArtistsRecyclerView: IndexBarRecyclerView
    private lateinit var mAlbumsRecyclerView: RecyclerView
    private lateinit var mSongsRecyclerView: RecyclerView

    private lateinit var mArtistsAdapter: ArtistsAdapter
    private lateinit var mAlbumsAdapter: AlbumsAdapter
    private lateinit var mSongsAdapter: SongsAdapter

    private lateinit var mArtistsLayoutManager: LinearLayoutManager
    private lateinit var mAlbumsLayoutManager: LinearLayoutManager
    private lateinit var mSongsLayoutManager: LinearLayoutManager

    private lateinit var mSavedArtistRecyclerLayoutState: Parcelable
    private lateinit var mSavedAlbumsRecyclerLayoutState: Parcelable
    private lateinit var mSavedSongsRecyclerLayoutState: Parcelable

    //search bar
    private lateinit var mSupportActionBar: ActionBar

    //settings/controls panel
    private lateinit var mControlsContainer: LinearLayout
    private lateinit var mColorsRecyclerView: RecyclerView
    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var mPlayerInfoView: View
    private lateinit var mPlayingAlbum: TextView
    private lateinit var mPlayingSong: TextView
    private lateinit var mSeekBar: SeekBar
    private lateinit var mSongPosition: TextView
    private lateinit var mSongDuration: TextView
    private lateinit var mSkipPrevButton: ImageView
    private lateinit var mPlayPauseButton: ImageView
    private lateinit var mSkipNextButton: ImageView
    private lateinit var mSearchToggleButton: ImageView

    //artists details
    private lateinit var mArtistDetails: LinearLayout
    private lateinit var mArtistDetailsTitle: TextView
    private lateinit var mArtistsDetailsDiscCount: TextView
    private lateinit var mArtistsDetailsSelectedDisc: TextView
    private lateinit var mArtistDetailsSelectedDiscYear: TextView

    //view model
    private lateinit var mViewModel: MusicViewModel

    //booleans
    private var sBound: Boolean = false
    private var sArtistDiscographyExpanded: Boolean = false
    private var sUserIsSeeking = false

    //strings
    private lateinit var mNavigationArtist: String

    //music
    private lateinit var mMusic: Map<String, Map<String, List<Music>>>
    private lateinit var mArtists: MutableList<String>
    private lateinit var mSelectedArtistSongs: MutableList<Music>
    private lateinit var mSelectedArtistAlbums: List<Album>

    //player
    private lateinit var mPlayerService: PlayerService
    private lateinit var mMediaPlayerHolder: MediaPlayerHolder
    private lateinit var mMusicNotificationManager: MusicNotificationManager

    override fun onDestroy() {
        super.onDestroy()
        doUnbindService()
    }

    //method to handle the navigation bar back feedback
    fun onBackPressed(): Boolean {
        //if the bottom sheet is expanded collapse it
        return when (mBottomSheetBehavior.state) {
            BottomSheetBehavior.STATE_EXPANDED -> {
                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                false
            }
            else -> {
                return when (sArtistDiscographyExpanded) {
                    true -> {
                        revealArtistDetails(false)
                        false
                    }
                    else -> true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mSavedArtistRecyclerLayoutState.isInitialized && ::mSavedAlbumsRecyclerLayoutState.isInitialized && ::mSavedSongsRecyclerLayoutState.isInitialized) {
            mArtistsLayoutManager.onRestoreInstanceState(mSavedArtistRecyclerLayoutState)
            mAlbumsLayoutManager.onRestoreInstanceState(mSavedAlbumsRecyclerLayoutState)
            mSongsLayoutManager.onRestoreInstanceState(mSavedSongsRecyclerLayoutState)
        }
        if (::mMediaPlayerHolder.isInitialized && mMediaPlayerHolder.isMediaPlayer) {
            mMediaPlayerHolder.onResumeActivity()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mMediaPlayerHolder.isInitialized && mMediaPlayerHolder.isMediaPlayer) {
            mMediaPlayerHolder.onPauseActivity()
        }
        if (::mArtistsLayoutManager.isInitialized && ::mAlbumsLayoutManager.isInitialized && ::mSongsLayoutManager.isInitialized) {
            mSavedArtistRecyclerLayoutState = mArtistsLayoutManager.onSaveInstanceState()!!
            mSavedAlbumsRecyclerLayoutState = mAlbumsLayoutManager.onSaveInstanceState()!!
            mSavedSongsRecyclerLayoutState = mSongsLayoutManager.onSaveInstanceState()!!
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        mActivity.menuInflater.inflate(R.menu.search_menu, menu)

        val search = menu!!.findItem(R.id.search)
        val searchView = search.actionView as SearchView

        searchView.setIconifiedByDefault(false)
        UIUtils.setupSearch(searchView, mArtistsAdapter, mArtists, mArtistsRecyclerView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            sThemeInverted = it.getBoolean(ARG_INVERTED)
            mAccent = it.getInt(ARG_ACCENT)
        }
        mPreferencesHelper = PreferencesHelper(activity!!)
        sSearchEnabled = mPreferencesHelper.isSearchBarEnabled()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mActivity = activity as AppCompatActivity
        setViews()
        mControlsContainer.afterMeasured {
            container.setPadding(0, 0, 0, height)
            mBottomSheetBehavior.peekHeight = height
        }
        setupPlayerControls()
        setupSettings()
        initializeSeekBar()
        doBindService()
    }

    private fun loadMusic() {
        mViewModel = ViewModelProviders.of(this).get(MusicViewModel::class.java)

        mViewModel.getMusic(MusicUtils.getMusicCursor(mActivity.contentResolver)!!)
            .observe(this, Observer<Map<String, Map<String, List<Music>>>> { music ->
                mMusic = music
                if (mMusic.isNotEmpty()) {
                    setArtistsRecyclerView()
                    mNavigationArtist = mArtists[0]
                    restorePlayerStatus()
                } else {
                    Toast.makeText(mActivity, getString(R.string.error_no_music), Toast.LENGTH_SHORT).show()
                    mActivity.finish()
                }
            })
    }

    private fun setViews() {

        //main
        main.setBackgroundColor(
            ColorUtils.setAlphaComponent(
                ContextCompat.getColor(mActivity, mAccent!!),
                if (sThemeInverted!!) 10 else 40
            )
        )

        //controls panel
        mControlsContainer = controls_container
        mBottomSheetBehavior = BottomSheetBehavior.from(design_bottom_sheet)
        mPlayerInfoView = player_info
        mPlayingSong = playing_song
        mPlayingAlbum = playing_album
        mSeekBar = seekTo
        mSongPosition = song_position
        mSongDuration = duration
        mSkipPrevButton = skip_prev_button
        mPlayPauseButton = play_pause_button
        mSkipNextButton = skip_next_button
        mSearchToggleButton = search_option

        //setup horizontal scrolling text
        UIUtils.setHorizontalScrollBehavior(mPlayerInfoView, playing_song, playing_album)

        //recycler views
        mArtistsRecyclerView = artists_rv
        mAlbumsRecyclerView = albums_rv
        mSongsRecyclerView = songs_rv
        mColorsRecyclerView = colors_rv

        //search view
        mActivity.setSupportActionBar(search_toolbar)
        mSupportActionBar = mActivity.supportActionBar!!

        if (sSearchEnabled) {
            mSupportActionBar.show()
        } else {
            mSupportActionBar.hide()
        }

        //artist details
        mArtistDetails = artist_details
        mArtistDetailsTitle = selected_discography_artist
        mArtistsDetailsDiscCount = selected_artist_album_count
        mArtistsDetailsSelectedDisc = selected_disc

        //setup horizontal scrolling text for artist details title and album title
        UIUtils.setHorizontalScrollBehavior(discs_artist_container, mArtistDetailsTitle)
        UIUtils.setHorizontalScrollBehavior(disc_title_container, mArtistsDetailsSelectedDisc)

        mArtistDetailsSelectedDiscYear = selected_disc_year
        close_button.setOnClickListener { revealArtistDetails(!sArtistDiscographyExpanded) }
    }

    private fun setupPlayerControls() {
        mPlayerInfoView.setOnClickListener { handlePlayerInfo() }
        mSkipPrevButton.setOnClickListener { skipPrev() }
        mSkipPrevButton.setOnLongClickListener {
            setRepeat()
            return@setOnLongClickListener false
        }
        mPlayPauseButton.setOnClickListener { resumeOrPause() }
        mSkipNextButton.setOnClickListener { skipNext() }
        shuffle_button.setOnClickListener {
            if (::mMediaPlayerHolder.isInitialized) {
                if (!mSeekBar.isEnabled) mSeekBar.isEnabled = true
                mSongsAdapter.randomPlaySelectedAlbum(mMediaPlayerHolder)
            }
        }
    }

    private fun setupSettings() {

        shuffle_option.setOnClickListener { shuffleSongs() }
        eq_option.setOnClickListener { openEqualizer() }
        mSearchToggleButton.setOnClickListener { handleSearchBarVisibility() }
        invert_option.setOnClickListener {
            invertTheme()
        }

        if (!sSearchEnabled) search_option.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)

        mColorsRecyclerView.layoutManager = LinearLayoutManager(mActivity, RecyclerView.HORIZONTAL, false)
        val colorsAdapter = ColorsAdapter(mActivity, mAccent!!)
        mColorsRecyclerView.adapter = colorsAdapter

        colorsAdapter.onColorClick = { accent ->
            mMusicNotificationManager.accent = ContextCompat.getColor(mActivity, accent)
            if (mMediaPlayerHolder.isMediaPlayer) {
                mMusicNotificationManager.notificationManager.notify(
                    NOTIFICATION_ID,
                    mMusicNotificationManager.createNotification()
                )
            }
            PreferencesHelper(mActivity).setThemeAccent(accent)
        }
    }

    private fun continueLoadingOnArtistsConfigured() {
        //set indexes if artists rv is scrollable
        mArtistsRecyclerView.afterMeasured {
            if (mArtistsRecyclerView.computeVerticalScrollRange() > height) {
                val indexBarView = IndexBarView(
                    mActivity,
                    mArtistsRecyclerView,
                    mArtistsAdapter,
                    mArtistsLayoutManager,
                    sThemeInverted!!,
                    ContextCompat.getColor(mActivity, mAccent!!)
                )
                mArtistsRecyclerView.setFastScroller(indexBarView)
            }
            //set artist details on artists rv loaded
            setArtistDetails()
        }
    }

    private fun setArtistsRecyclerView() {

        mArtists = MusicUtils.getArtists(mMusic)
        mNavigationArtist = mArtists[0]

        //set the search menu
        setHasOptionsMenu(sSearchEnabled)

        //set the artists list
        mArtistsRecyclerView.setHasFixedSize(true)
        mArtistsLayoutManager = LinearLayoutManager(mActivity)
        mArtistsRecyclerView.layoutManager = mArtistsLayoutManager
        mArtistsAdapter = ArtistsAdapter(resources, mArtists, mMusic)
        mArtistsRecyclerView.adapter = mArtistsAdapter

        mArtistsAdapter.onArtistClick = { artist ->
            if (mNavigationArtist != artist) {
                mNavigationArtist = artist
                setArtistDetails()
                revealArtistDetails(true)
            } else {
                revealArtistDetails(true)
            }
        }

        continueLoadingOnArtistsConfigured()
    }

    private fun setArtistDetails() {

        val notSortedArtistDiscs = mMusic[mNavigationArtist]!!
        mSelectedArtistAlbums = MusicUtils.buildSortedArtistAlbums(resources, mMusic[mNavigationArtist]!!)

        //set the titles and subtitles
        mArtistDetailsTitle.text = mNavigationArtist
        mArtistsDetailsDiscCount.text = getString(R.string.albums, MusicUtils.getArtistDiscsCount(notSortedArtistDiscs))

        //set the albums list
        //one-time adapter initialization
        mAlbumsRecyclerView.setHasFixedSize(true)
        mAlbumsLayoutManager = LinearLayoutManager(mActivity, RecyclerView.HORIZONTAL, false)
        mAlbumsRecyclerView.layoutManager = mAlbumsLayoutManager
        mAlbumsAdapter = AlbumsAdapter(
            mSelectedArtistAlbums,
            ContextCompat.getColor(mActivity, mAccent!!)
        )
        mAlbumsRecyclerView.adapter = mAlbumsAdapter

        mAlbumsAdapter.onAlbumClick = { album ->
            setAlbumSongs(album)
        }

        mSelectedArtistSongs = MusicUtils.getArtistSongs(notSortedArtistDiscs)

        val placeholderAlbum = mSelectedArtistAlbums[0]
        setAlbumSongs(placeholderAlbum.title)
    }

    private fun setAlbumSongs(selectedAlbum: String) {
        val album = mMusic[mNavigationArtist]!![selectedAlbum]
        mArtistsDetailsSelectedDisc.text = selectedAlbum
        mArtistDetailsSelectedDiscYear.text = MusicUtils.getYearForAlbum(resources, album!![0].year)

        //set the songs list
        if (!::mSongsAdapter.isInitialized) {
            //one-time adapter initialization
            mSongsRecyclerView.setHasFixedSize(true)
            mSongsLayoutManager = LinearLayoutManager(mActivity)
            mSongsRecyclerView.layoutManager = mSongsLayoutManager
            mSongsAdapter = SongsAdapter(album.toMutableList())
            mSongsRecyclerView.adapter = mSongsAdapter
        } else {
            mSongsAdapter.swapSongs(album.toMutableList())
        }
        mSongsAdapter.onSongClick = { music ->
            if (!mSeekBar.isEnabled) mSeekBar.isEnabled = true
            mMediaPlayerHolder.setCurrentSong(music, album)
            mMediaPlayerHolder.initMediaPlayer(music)
        }
    }

    private fun initializeSeekBar() {
        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            val currentPositionColor = mSongPosition.currentTextColor
            var userSelectedPosition = 0

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                sUserIsSeeking = true
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    userSelectedPosition = progress
                    mSongPosition.setTextColor(ContextCompat.getColor(mActivity, mAccent!!))
                }
                mSongPosition.text = MusicUtils.formatSongDuration(progress.toLong())
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (sUserIsSeeking) {
                    mSongPosition.setTextColor(currentPositionColor)
                }
                sUserIsSeeking = false
                mMediaPlayerHolder.seekTo(userSelectedPosition)
            }
        })
    }

    private fun shuffleSongs() {
        if (::mMediaPlayerHolder.isInitialized) {
            val songs = if (sArtistDiscographyExpanded) mSelectedArtistSongs else mViewModel.allDeviceSongs
            songs.shuffle()
            val song = songs[0]
            mMediaPlayerHolder.setCurrentSong(song, songs)
            mMediaPlayerHolder.initMediaPlayer(song)
        }
    }

    private fun setRepeat() {
        if (checkIsPlayer()) {
            mMediaPlayerHolder.reset()
            updateResetStatus(false)
        }
    }

    private fun skipPrev() {
        if (checkIsPlayer()) {
            mMediaPlayerHolder.instantReset()
            if (mMediaPlayerHolder.isReset) {
                mMediaPlayerHolder.reset()
                updateResetStatus(false)
            }
        }
    }

    private fun resumeOrPause() {
        if (checkIsPlayer()) {
            mMediaPlayerHolder.resumeOrPause()
        }
    }

    private fun skipNext() {
        if (checkIsPlayer()) {
            mMediaPlayerHolder.skip(true)
        }
    }

    fun onPositionChanged(position: Int) {
        if (!sUserIsSeeking) {
            mSeekBar.progress = position
        }
    }

    fun onStateChanged() {
        updatePlayingStatus()
        if (mMediaPlayerHolder.state != RESUMED && mMediaPlayerHolder.state != PAUSED) {
            updatePlayingInfo(false, true)
        }
    }

    fun onPlaybackCompleted() {
        updateResetStatus(true)
    }

    private fun restorePlayerStatus() {
        if (::mMediaPlayerHolder.isInitialized) {
            mSeekBar.isEnabled = mMediaPlayerHolder.isMediaPlayer
            //if we are playing and the activity was restarted
            //update the controls panel
            if (mMediaPlayerHolder.isMediaPlayer) {
                mMediaPlayerHolder.onResumeActivity()
                updatePlayingInfo(true, false)
            }
        }
    }

    //method to update info on controls panel
    private fun updatePlayingInfo(restore: Boolean, startPlay: Boolean) {
        if (startPlay) {
            mMediaPlayerHolder.mediaPlayer!!.start()
            mPlayerService.startForeground(NOTIFICATION_ID, mMusicNotificationManager.createNotification())
        }

        val selectedSong = mMediaPlayerHolder.currentSong
        val duration = selectedSong!!.duration
        mSeekBar.max = duration.toInt()
        mSongDuration.text = MusicUtils.formatSongDuration(duration)
        mPlayingSong.text =
                MusicUtils.buildSpanned(getString(R.string.playing_song, selectedSong.artist, selectedSong.title))
        mPlayingAlbum.text = selectedSong.album

        if (restore) {
            mSongPosition.text = MusicUtils.formatSongDuration(mMediaPlayerHolder.playerPosition.toLong())
            mSeekBar.progress = mMediaPlayerHolder.playerPosition

            updatePlayingStatus()
            updateResetStatus(false)

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
        val themeColor = if (sThemeInverted!!) R.color.white else R.color.black
        val color = if (onPlaybackCompletion) themeColor else if (mMediaPlayerHolder.isReset) mAccent else themeColor
        mSkipPrevButton.setColorFilter(ContextCompat.getColor(mActivity, color!!), PorterDuff.Mode.SRC_IN)
    }

    private fun updatePlayingStatus() {
        val drawable = if (mMediaPlayerHolder.state != PAUSED) R.drawable.ic_pause else R.drawable.ic_play
        mPlayPauseButton.setImageResource(drawable)
    }

    private fun invertTheme() {
        //avoid service killing when the player is in paused state
        if (::mMediaPlayerHolder.isInitialized && mMediaPlayerHolder.isPlaying) {
            if (mMediaPlayerHolder.state == PAUSED) {
                mPlayerService.startForeground(
                    NOTIFICATION_ID,
                    mPlayerService.musicNotificationManager.createNotification()
                )
                mPlayerService.isRestoredFromPause = true
            }
        }
        mPreferencesHelper.invertTheme()
    }

    private fun checkIsPlayer(): Boolean {
        val isPlayer = mMediaPlayerHolder.isMediaPlayer
        if (!isPlayer) {
            EqualizerUtils.notifyNoSessionId(mActivity)
        }
        return isPlayer
    }

    private fun openEqualizer() {
        if (EqualizerUtils.hasEqualizer(mActivity)) {
            if (checkIsPlayer()) {
                mMediaPlayerHolder.openEqualizer(mActivity)
            }
        } else {
            Toast.makeText(mActivity, getString(R.string.no_eq), Toast.LENGTH_SHORT).show()
        }
    }

    //method to reveal/hide artist details, it is a simple reveal animation
    private fun revealArtistDetails(show: Boolean) {

        val viewToRevealHeight = mArtistsRecyclerView.height
        val viewToRevealWidth = mAlbumsRecyclerView.width
        val viewToRevealHalfWidth = viewToRevealWidth / 2
        val radius = Math.hypot(viewToRevealWidth.toDouble(), viewToRevealHeight.toDouble()).toFloat()
        val fromY = mArtistsRecyclerView.top / 2
        val animationDuration: Long = 500

        if (show) {
            val anim = ViewAnimationUtils.createCircularReveal(mArtistDetails, viewToRevealHalfWidth, fromY, 0f, radius)
            anim.duration = animationDuration
            anim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {
                    mArtistDetails.visibility = View.VISIBLE
                    mArtistsRecyclerView.visibility = View.INVISIBLE
                    mArtistDetails.isClickable = false
                    if (sSearchEnabled && ::mSupportActionBar.isInitialized && mSupportActionBar.isShowing) mSupportActionBar.hide()
                }

                override fun onAnimationEnd(animator: Animator) {
                    sArtistDiscographyExpanded = true
                }

                override fun onAnimationCancel(animator: Animator) {}

                override fun onAnimationRepeat(animator: Animator) {}
            })
            anim.start()

        } else {

            val anim = ViewAnimationUtils.createCircularReveal(mArtistDetails, viewToRevealHalfWidth, fromY, radius, 0f)
            anim.duration = animationDuration
            anim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {
                    sArtistDiscographyExpanded = false
                }

                override fun onAnimationEnd(animator: Animator) {
                    mArtistDetails.visibility = View.INVISIBLE
                    mArtistsRecyclerView.visibility = View.VISIBLE
                    mArtistDetails.isClickable = true
                    sArtistDiscographyExpanded = false
                    if (sSearchEnabled && ::mSupportActionBar.isInitialized && !mSupportActionBar.isShowing) mSupportActionBar.show()
                }

                override fun onAnimationCancel(animator: Animator) {}

                override fun onAnimationRepeat(animator: Animator) {}
            })
            anim.start()
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
                mArtistsAdapter.onArtistClick?.invoke(artist)
                val playingAlbumPosition = MusicUtils.getAlbumPositionInList(album, mSelectedArtistAlbums)
                mAlbumsAdapter.swapSelectedAlbum(playingAlbumPosition)
                mAlbumsRecyclerView.scrollToPosition(playingAlbumPosition)
                mAlbumsAdapter.onAlbumClick?.invoke(album)
            } else {
                revealArtistDetails(!sArtistDiscographyExpanded)
            }
        } else {
            revealArtistDetails(!sArtistDiscographyExpanded)
        }
    }

    //hide/show search bar dynamically
    private fun handleSearchBarVisibility() {
        if (::mSupportActionBar.isInitialized) {
            val newVisibility = !mSupportActionBar.isShowing
            mPreferencesHelper.setSearchToolbarVisibility(newVisibility)
            setHasOptionsMenu(newVisibility)
            val searchToggleButtonColor = when (newVisibility) {
                false -> Color.GRAY
                true -> if (sThemeInverted!!) Color.WHITE else Color.BLACK
            }
            mSearchToggleButton.setColorFilter(searchToggleButtonColor, PorterDuff.Mode.SRC_IN)
            if (mSupportActionBar.isShowing) {
                mSupportActionBar.hide()
            } else {
                mSupportActionBar.show()
            }
            sSearchEnabled = newVisibility
        }
    }

    //viewTreeObserver extension to measure layout params
    //https://antonioleiva.com/kotlin-ongloballayoutlistener/
    private inline fun <T : View> T.afterMeasured(crossinline f: T.() -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    f()
                }
            }
        })
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            mPlayerService = (iBinder as PlayerService.LocalBinder).instance
            mMediaPlayerHolder = mPlayerService.mediaPlayerHolder!!
            mMediaPlayerHolder.mainFragment = this@MainFragment
            mMusicNotificationManager = mPlayerService.musicNotificationManager
            mMusicNotificationManager.accent = ContextCompat.getColor(mActivity, mAccent!!)
            loadMusic()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
        }
    }

    private fun doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        val startNotStickyIntent = Intent(mActivity, PlayerService::class.java)
        mActivity.bindService(startNotStickyIntent, mConnection, Context.BIND_AUTO_CREATE)
        sBound = true
        mActivity.startService(startNotStickyIntent)
    }

    private fun doUnbindService() {
        if (sBound) {
            // Detach our existing connection.
            mActivity.unbindService(mConnection)
            sBound = false
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(isThemeInverted: Boolean, accent: Int) =
            MainFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_INVERTED, isThemeInverted)
                    putInt(ARG_ACCENT, accent)
                }
            }
    }
}
