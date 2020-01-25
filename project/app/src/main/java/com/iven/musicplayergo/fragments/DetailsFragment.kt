package com.iven.musicplayergo.fragments

import android.animation.Animator
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.*
import com.iven.musicplayergo.music.Album
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.ui.*
import kotlinx.android.synthetic.main.fragment_details.*


/**
 * A simple [Fragment] subclass.
 * Use the [DetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class DetailsFragment : Fragment(R.layout.fragment_details), SearchView.OnQueryTextListener {

    private var sFolder = false

    private lateinit var mArtistDetailsView: View
    private lateinit var mArtistDetailsAnimator: Animator

    private lateinit var mDetailsToolbar: Toolbar

    private lateinit var mSelectedAlbumTitle: TextView
    private lateinit var mSelectedAlbumYearDuration: TextView

    private val mSelectedAlbumsDataSource = dataSourceOf()
    private val mSongsDataSource = dataSourceOf()

    private lateinit var mAlbumsRecyclerView: RecyclerView
    private lateinit var mAlbumsRecyclerViewLayoutManager: LinearLayoutManager
    private lateinit var mSongsRecyclerView: RecyclerView

    private var mSelectedArtistAlbums: List<Album>? = null
    private var mSongsForArtistOrFolder: List<Music>? = null

    private var mSelectedArtistOrFolder: String? = null
    private var mSelectedAlbumPosition = -1

    private lateinit var mUIControlInterface: UIControlInterface
    private var mSelectedAlbum: Album? = null

    private var sLandscape = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_ARTIST_FOLDER)?.let {
            mSelectedArtistOrFolder = it
        }

        arguments?.getBoolean(TAG_IS_FOLDER)?.let {
            sFolder = it
        }

        if (!sFolder) {

            arguments?.getInt(TAG_SELECTED_ALBUM_POSITION)?.let {
                mSelectedAlbumPosition = it
            }

            musicLibrary.allAlbumsByArtist?.get(mSelectedArtistOrFolder)?.let {
                mSelectedArtistAlbums = it
            }

            musicLibrary.allSongsByArtist?.get(mSelectedArtistOrFolder)?.let {
                mSongsForArtistOrFolder = it
            }

            mSelectedAlbum =
                if (mSelectedAlbumPosition != -1) {
                    mSelectedArtistAlbums?.get(mSelectedAlbumPosition)
                } else {
                    mSelectedAlbumPosition = 0
                    mSelectedArtistAlbums?.get(0)
                }
        } else {
            mSongsForArtistOrFolder =
                musicLibrary.allSongsByFolder?.get(mSelectedArtistOrFolder)
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    fun onHandleBackPressed(): Animator {
        if (!mArtistDetailsAnimator.isRunning) mArtistDetailsAnimator =
            mArtistDetailsView.createCircularReveal(isCentered = false, show = false)
        return mArtistDetailsAnimator
    }

    //https://stackoverflow.com/a/38241603
    private fun getTitleTextView(toolbar: Toolbar): TextView? {
        return try {
            val toolbarClass = Toolbar::class.java
            val titleTextViewField = toolbarClass.getDeclaredField("mTitleTextView")
            titleTextViewField.isAccessible = true
            titleTextViewField.get(toolbar) as TextView
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mArtistDetailsView = view
        mDetailsToolbar = details_toolbar
        mAlbumsRecyclerView = albums_rv
        mSelectedAlbumTitle = selected_album
        mSelectedAlbumYearDuration = album_year_duration
        mSongsRecyclerView = songs_rv

        context?.let { cxt ->

            sLandscape = ThemeHelper.isDeviceLand(cxt.resources)

            mDetailsToolbar.apply {

                overflowIcon = AppCompatResources.getDrawable(cxt, R.drawable.ic_shuffle)

                title = mSelectedArtistOrFolder

                //make toolbar's title scrollable
                getTitleTextView(this)?.let { tV ->
                    tV.isSelected = true
                    tV.setHorizontallyScrolling(true)
                    tV.ellipsize = TextUtils.TruncateAt.MARQUEE
                    tV.marqueeRepeatLimit = -1
                }

                setupToolbarSpecs(sFolder)

                setNavigationOnClickListener {
                    activity?.onBackPressed()
                }

                setupMenu()
            }

            if (!sFolder) {

                setupAlbumsContainer(cxt)

            } else {

                mAlbumsRecyclerView.visibility = View.GONE
                mSelectedAlbumTitle.visibility = View.GONE
                mSelectedAlbumYearDuration.visibility = View.GONE

                mSongsForArtistOrFolder =
                    musicLibrary.allSongsByFolder?.get(mSelectedArtistOrFolder)

                mDetailsToolbar.subtitle = getString(
                    R.string.folder_info,
                    mSongsForArtistOrFolder?.size
                )

                val searchView =
                    mDetailsToolbar.menu.findItem(R.id.action_search).actionView as SearchView
                searchView.apply {
                    setOnQueryTextListener(this@DetailsFragment)
                    setOnQueryTextFocusChangeListener { _, hasFocus ->
                        mDetailsToolbar.menu.setGroupVisible(R.id.shuffle_options_folder, !hasFocus)
                    }
                }
            }

            setSongsDataSource(if (sFolder) mSongsForArtistOrFolder else mSelectedAlbum?.music)

            mSongsRecyclerView.apply {

                // setup{} is an extension method on RecyclerView
                setup {
                    // item is a `val` in `this` here
                    withDataSource(mSongsDataSource)

                    if (sLandscape)
                        withLayoutManager(GridLayoutManager(cxt, 2))
                    else
                        addItemDecoration(ThemeHelper.getRecyclerViewDivider(cxt))

                    withItem<Music, GenericViewHolder>(R.layout.song_item) {
                        onBind(::GenericViewHolder) { _, item ->
                            // GenericViewHolder is `this` here
                            title.text = ThemeHelper.buildSpanned(
                                getString(
                                    R.string.track_song,
                                    MusicUtils.formatSongTrack(item.track),
                                    item.title
                                )
                            )
                            subtitle.text = MusicUtils.formatSongDuration(item.duration, false)
                        }

                        onClick {

                            val selectedPlaylist =
                                if (sFolder) mSongsForArtistOrFolder
                                else
                                    MusicUtils.getAlbumSongs(item.artist, item.album)

                            mUIControlInterface.onSongSelected(
                                item,
                                selectedPlaylist,
                                sFolder
                            )
                        }

                        onLongClick { index ->
                            Utils.showAddToLovedQueueSongsPopup(
                                cxt,
                                findViewHolderForAdapterPosition(index)?.itemView!!,
                                item,
                                mUIControlInterface
                            )
                        }
                    }
                }
            }

            view.afterMeasured {
                mArtistDetailsAnimator =
                    mArtistDetailsView.createCircularReveal(isCentered = false, show = true)
            }
        }
    }

    private fun setAlbumsDataSource(albumsList: List<Album>?) {
        albumsList?.apply {
            mSelectedAlbumsDataSource.set(this)
        }
    }

    private fun setSongsDataSource(musicList: List<Music>?) {
        musicList?.apply {
            mSongsDataSource.set(this)
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setSongsDataSource(
            Utils.processQueryForMusic(newText, mSongsForArtistOrFolder)
                ?: mSongsForArtistOrFolder
        )
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    private fun setupMenu() {

        mDetailsToolbar.apply {

            val menuToInflate =
                if (sFolder) R.menu.menu_folder_details else R.menu.menu_artist_details

            inflateMenu(menuToInflate)

            menu.apply {
                findItem(R.id.action_shuffle_sa).isEnabled = !sFolder
                if (!sFolder) findItem(R.id.action_shuffle_am).isEnabled =
                    mSelectedArtistAlbums?.size!! >= 2
            }

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_shuffle_am -> mUIControlInterface.onShuffleSongs(
                        mSongsForArtistOrFolder?.toMutableList(),
                        sFolder
                    )
                    R.id.action_shuffle_sa -> mUIControlInterface.onShuffleSongs(
                        mSelectedAlbum?.music,
                        sFolder
                    )
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun setupToolbarSpecs(isFolder: Boolean) {
        mDetailsToolbar.apply {
            elevation = if (isFolder)
                resources.getDimensionPixelSize(R.dimen.search_bar_elevation).toFloat() else 0F

            val params = layoutParams as LinearLayout.LayoutParams
            params.bottomMargin = if (isFolder)
                0 else resources.getDimensionPixelSize(R.dimen.player_controls_padding_normal)
        }
    }

    private fun setupAlbumsContainer(context: Context) {

        selected_album_container.setOnClickListener {
            mAlbumsRecyclerViewLayoutManager.scrollToPositionWithOffset(
                mSelectedAlbumPosition,
                0
            )
        }

        mSelectedAlbumTitle.isSelected = true

        updateSelectedAlbumTitle()

        setAlbumsDataSource(mSelectedArtistAlbums)

        mDetailsToolbar.subtitle = getString(
            R.string.artist_info,
            mSelectedArtistAlbums?.size,
            mSongsForArtistOrFolder?.size
        )

        mAlbumsRecyclerView.apply {

            setup {

                withDataSource(mSelectedAlbumsDataSource)

                mAlbumsRecyclerViewLayoutManager = LinearLayoutManager(
                    context,
                    if (sLandscape) LinearLayoutManager.VERTICAL else LinearLayoutManager.HORIZONTAL,
                    false
                )
                withLayoutManager(mAlbumsRecyclerViewLayoutManager)

                withItem<Album, AlbumsViewHolder>(R.layout.album_item) {

                    onBind(::AlbumsViewHolder) { _, item ->
                        // AlbumsViewHolder is `this` here
                        itemView.background.alpha = 20
                        album.text = item.title
                        year.text = item.year
                        totalDuration.text =
                            MusicUtils.formatSongDuration(item.totalDuration, true)
                        checkbox.visibility =
                            if (mSelectedAlbum?.title != item.title) View.GONE else View.VISIBLE
                    }

                    onClick { index ->

                        if (index != mSelectedAlbumPosition) {

                            mAlbumsRecyclerView.adapter?.apply {
                                notifyItemChanged(
                                    mSelectedAlbumPosition
                                )

                                notifyItemChanged(index)

                                mSelectedAlbum = item
                                mSelectedAlbumPosition = index
                                updateSelectedAlbumTitle()
                                swapAlbum(item.music)
                            }
                        }
                    }
                }
            }
            if (mSelectedAlbumPosition != -1 || mSelectedAlbumPosition != 0) mAlbumsRecyclerViewLayoutManager.scrollToPositionWithOffset(
                mSelectedAlbumPosition,
                0
            )
        }
    }

    fun hasToUpdate(selectedArtistOrFolder: String?): Boolean {
        return selectedArtistOrFolder != mSelectedArtistOrFolder
    }

    fun tryToSnapToAlbumPosition(snapPosition: Int) {
        if (!sFolder && snapPosition != -1) mAlbumsRecyclerView.smoothSnapToPosition(snapPosition)
    }

    private fun updateSelectedAlbumTitle() {
        mSelectedAlbumTitle.text = mSelectedAlbum?.title
        mSelectedAlbumYearDuration.text = getString(
            R.string.year_and_duration,
            mSelectedAlbum?.year,
            MusicUtils.formatSongDuration(mSelectedAlbum?.totalDuration, true)
        )
    }

    private fun swapAlbum(songs: MutableList<Music>?) {
        setSongsDataSource(songs)
        mSongsRecyclerView.scrollToPosition(0)
    }

    companion object {

        private const val TAG_ARTIST_FOLDER = "SELECTED_ARTIST_FOLDER"
        private const val TAG_IS_FOLDER = "IS_FOLDER"
        private const val TAG_SELECTED_ALBUM_POSITION = "SELECTED_ALBUM_POSITION"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment DetailsFragment.
         */
        @JvmStatic
        fun newInstance(
            selectedArtistOrFolder: String?,
            isFolder: Boolean,
            playedAlbumPosition: Int
        ) =
            DetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(TAG_ARTIST_FOLDER, selectedArtistOrFolder)
                    putBoolean(TAG_IS_FOLDER, isFolder)
                    putInt(TAG_SELECTED_ALBUM_POSITION, playedAlbumPosition)
                }
            }
    }
}
