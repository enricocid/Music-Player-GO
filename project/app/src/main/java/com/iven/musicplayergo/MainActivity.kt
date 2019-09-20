package com.iven.musicplayergo

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.recyclical.datasource.DataSource
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.iven.musicplayergo.fragments.AllMusicFragment
import com.iven.musicplayergo.fragments.ArtistsFragment
import com.iven.musicplayergo.fragments.FoldersFragment
import com.iven.musicplayergo.fragments.SettingsFragment
import com.iven.musicplayergo.music.Album
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.music.MusicViewModel
import com.iven.musicplayergo.ui.*
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.songs_sheet.*

class MainActivity : AppCompatActivity(), SongsSheetInterface {

    //default
    private lateinit var mArtistsFragment: Fragment

    private lateinit var mAllMusicFragment: Fragment
    private lateinit var mFoldersFragment: Fragment
    private lateinit var mSettingsFragment: Fragment

    private lateinit var mFragmentManager: FragmentManager

    private lateinit var mActiveFragment: Fragment

    private lateinit var mPager: ViewPager
    private lateinit var mTabLayout: TabLayout

    private lateinit var mSongsSheet: View
    private lateinit var mCoverView: View
    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<View>

    private lateinit var mTheme: String
    private var mAccent = R.color.deepPurple

    //music management
    private lateinit var mSelectedAlbum: String

    // music shit related
    private val mMusicViewModel: MusicViewModel by lazy {
        ViewModelProviders.of(this).get(MusicViewModel::class.java)
    }

    private fun handleOnNavigationItemSelected(itemId: Int): Fragment {

        when (itemId) {
            0 -> mActiveFragment = mArtistsFragment
            1 -> mActiveFragment = mAllMusicFragment
            2 -> mActiveFragment = mFoldersFragment
            3 -> mActiveFragment = mSettingsFragment
        }

        return mActiveFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        mTheme = musicPlayerGoExAppPreferences.theme!!
        mAccent = musicPlayerGoExAppPreferences.accent

        ThemeHelper.applyTheme(this, mTheme)
        setTheme(ThemeHelper.getAccent(mAccent).first)

        mFragmentManager = supportFragmentManager

        setContentView(R.layout.main_activity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) checkPermission() else setupUI()
        super.onCreate(savedInstanceState)
    }

    //manage request permission result, continue loading ui if permissions was granted
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) showPermissionRationale() else setupUI()
    }

    @TargetApi(23)
    private fun checkPermission() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        )
            showPermissionRationale() else setupUI()
    }

    private fun showPermissionRationale() {

        MaterialDialog(this).show {

            cornerRadius(res = R.dimen.md_radius)
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
                Utils.makeUnknownErrorToast(this@MainActivity, R.string.perm_rationale)
                dismiss()
                finishAndRemoveTask()
            }
        }
    }

    private fun setupUI() {

        val mainActivity = main_activity
        val isModeNight =
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        val accent = ThemeHelper.getColor(this, mAccent, R.color.deepPurple)
        mainActivity.setBackgroundColor(
            if (isModeNight)
                ThemeHelper.darkenColor(accent, 0.90F) else ThemeHelper.lightenColor(accent, 0.95F)
        )

        mMusicViewModel.loadMusic(this).observe(this, Observer { hasLoaded ->

            if (hasLoaded) {

                mArtistsFragment = ArtistsFragment.newInstance()
                mActiveFragment = mArtistsFragment

                mAllMusicFragment = AllMusicFragment.newInstance()
                mFoldersFragment = FoldersFragment.newInstance()
                mSettingsFragment = SettingsFragment.newInstance()

                mPager = pager

                mSongsSheet = songs_sheet
                mCoverView = cover_view
                mBottomSheetBehavior = BottomSheetBehavior.from(mSongsSheet)
                mBottomSheetBehavior.bottomSheetCallback =
                    object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onSlide(bottomSheet: View, slideOffset: Float) {

                            if (slideOffset > 0) {
                                val baseAlpha = (Color.BLACK and -0x1000000).ushr(24)
                                val imag = (baseAlpha * slideOffset).toInt()
                                val color = imag shl 24 or (Color.BLACK and 0xffffff)
                                mCoverView.setBackgroundColor(color)
                            }
                        }

                        override fun onStateChanged(bottomSheet: View, newState: Int) {

                            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                                toggleBottomSheetVisibility(View.INVISIBLE)
                            }
                        }
                    }

                mTabLayout = tab_layout
                mTabLayout.setupWithViewPager(mPager)

                val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
                mPager.adapter = pagerAdapter
                mPager.setPageTransformer(true, ZoomOutPageTransformer())

                setupTabLayoutTabs(0)
                setupTabLayoutTabs(1)
                setupTabLayoutTabs(2)
                setupTabLayoutTabs(3)
            }
        })
    }

    private fun setupTabLayoutTabs(index: Int) {
        val icons = arrayListOf(
            R.drawable.ic_person,
            R.drawable.ic_music,
            R.drawable.ic_folder,
            R.drawable.ic_settings
        )

        mTabLayout.getTabAt(index)?.setIcon(icons[index])
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int = 4

        override fun getItem(position: Int): Fragment {
            return handleOnNavigationItemSelected(position)
        }
    }

    private fun toggleBottomSheetVisibility(visibility: Int) {
        mSongsSheet.visibility = visibility
        mCoverView.visibility = visibility
    }

    override fun onPopulateAndShowSheet(
        isFolder: Boolean,
        header: String,
        subheading: String,
        songs: List<Music>
    ) {

        toggleBottomSheetVisibility(View.VISIBLE)

        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        songs_sheet_title.text = header
        songs_sheet_subtitle.text = subheading

        val albumsRecyclerView = albums_rv
        albumsRecyclerView.visibility = if (isFolder) View.GONE else View.VISIBLE


        if (!isFolder) {

            songs.sortedBy { it.track }

            val albums = MusicUtils.buildSortedArtistAlbums(
                resources,
                musicLibrary.allCategorizedMusic.getValue(header)
            )
            val dataSource = dataSourceOf(albums)

            mSelectedAlbum = albums[0].title!!

            // setup{} is an extension method on RecyclerView
            albumsRecyclerView.setup {
                // item is a `val` in `this` here
                withDataSource(dataSource)
                withLayoutManager(
                    LinearLayoutManager(
                        this@MainActivity,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                )
                withItem<Album, AlbumsViewHolder>(R.layout.recycler_view_album_item) {
                    onBind(::AlbumsViewHolder) { _, item ->
                        // AlbumsViewHolder is `this` here
                        album.text = item.title
                        year.text = item.year
                        if (mSelectedAlbum != item.title) albumCard.strokeColor =
                            Color.TRANSPARENT else albumCard.strokeColor =
                            ThemeHelper.getColor(this@MainActivity, mAccent, R.color.deepPurple)
                    }

                    onClick { index ->

                        if (mSelectedAlbum != item.title) {
                            albumsRecyclerView.adapter?.notifyItemChanged(
                                MusicUtils.getAlbumPositionInList(
                                    item.title,
                                    albums
                                )
                            )
                            albumsRecyclerView.adapter?.notifyItemChanged(
                                MusicUtils.getAlbumPositionInList(
                                    mSelectedAlbum,
                                    albums
                                )
                            )
                            mSelectedAlbum = item.title!!
                            //  albumCard.strokeColor = accent
                            setupSongsRecyclerView(dataSourceOf(albums[index].music!!))
                        }
                    }
                }
            }
        }

        setupSongsRecyclerView(dataSourceOf(songs))
    }

    override fun onShowSheet() {
        if (mBottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            toggleBottomSheetVisibility(View.VISIBLE)
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onSongSelected(song: Music) {
        Log.d(song.title, song.title!!)
        mSelectedAlbum = song.album!!
    }

    private fun setupSongsRecyclerView(dataSource: DataSource<Any>) {

        val songsRecyclerView = songs_rv
        ViewCompat.setNestedScrollingEnabled(songsRecyclerView, false)

        // setup{} is an extension method on RecyclerView
        songsRecyclerView.setup {
            // item is a `val` in `this` here
            withDataSource(dataSource)
            withItem<Music, GenericViewHolder>(R.layout.recycler_view_item) {
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
                    onSongSelected(item)
                }
                onLongClick { index ->
                    // item is a `val` in `this` here
                    Log.d("doSomething", "Clicked $index: ${item.title}")
                }
            }
        }
    }
}
