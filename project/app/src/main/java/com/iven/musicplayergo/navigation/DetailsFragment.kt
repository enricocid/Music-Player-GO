package com.iven.musicplayergo.navigation

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicRepository
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentDetailsBinding
import com.iven.musicplayergo.enums.LaunchedBy
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.MusicOrgHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.interfaces.UIControlInterface
import com.iven.musicplayergo.models.Album
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.AlbumsViewHolder
import com.iven.musicplayergo.ui.GenericViewHolder
import kotlinx.android.synthetic.main.fragment_details.*


class DetailsFragment : Fragment(R.layout.fragment_details), SearchView.OnQueryTextListener {

    private var mUIControlInterface: UIControlInterface? = null
    private val mMediaPlayerInterface get() = MediaPlayerHolder.getInstance().mediaPlayerInterface

    private lateinit var mDetailsFragmentBinding: FragmentDetailsBinding
    private lateinit var mMusicRepository: MusicRepository

    private lateinit var mAlbumsRecyclerViewLayoutManager: LinearLayoutManager

    private val mSelectedAlbumsDataSource = dataSourceOf()
    private val mSongsDataSource = dataSourceOf()

    private var launchedBy: LaunchedBy = LaunchedBy.ArtistView

    private var mSelectedArtistAlbums: List<Album>? = null
    private var mSongsList: List<Music>? = null

    private var mSelectedArtistOrFolder: String? = null
    private var mSelectedAlbumPosition = -1

    private var mSelectedAlbum: Album? = null

    private var sLandscape = false

    private var mSongsSorting = GoConstants.TRACK_SORTING

    private val sLaunchedByArtistView: Boolean get() = launchedBy == LaunchedBy.ArtistView
    private val sLaunchedByFolderView: Boolean get() = launchedBy == LaunchedBy.FolderView

    private val mImageLoader get() = ImageLoader(requireContext())

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(GoConstants.TAG_ARTIST_FOLDER)?.let { selectedArtistOrFolder ->
            mSelectedArtistOrFolder = selectedArtistOrFolder
        }

        arguments?.getInt(GoConstants.TAG_IS_FOLDER)?.let { launchedBy ->
            this.launchedBy = LaunchedBy.values()[launchedBy]
        }

        if (sLaunchedByArtistView) {
            arguments?.getInt(GoConstants.TAG_SELECTED_ALBUM_POSITION)
                ?.let { selectedAlbumPosition ->
                    mSelectedAlbumPosition = selectedAlbumPosition
                }
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    fun onSnapToPosition(selectedArtistOrFolder: String?): Boolean {
        val hasToUpdate =
            mSelectedArtistOrFolder != null && sLaunchedByArtistView && selectedArtistOrFolder != mSelectedArtistOrFolder
        if (!hasToUpdate && selectedArtistOrFolder == mSelectedArtistOrFolder) {
            val snapPosition = MusicOrgHelper.getPlayingAlbumPosition(
                selectedArtistOrFolder
            )
            if (snapPosition != -1) {
                mDetailsFragmentBinding.albumsRv.smoothSnapToPosition(
                    snapPosition
                )
            }
        }
        return hasToUpdate
    }

    // https://stackoverflow.com/a/38241603
    private fun getTitleTextView(toolbar: Toolbar) = try {
        val toolbarClass = Toolbar::class.java
        val titleTextViewField = toolbarClass.getDeclaredField("mTitleTextView")
        titleTextViewField.isAccessible = true
        titleTextViewField.get(toolbar) as TextView
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun getSongSource(): List<Music>? {
        return when (launchedBy) {
            LaunchedBy.ArtistView ->
                mMusicRepository.deviceSongsByArtist?.get(mSelectedArtistOrFolder)

            LaunchedBy.FolderView ->
                mMusicRepository.deviceMusicByFolder?.get(mSelectedArtistOrFolder)

            LaunchedBy.AlbumView ->
                mMusicRepository.deviceSongsByAlbum?.get(mSelectedArtistOrFolder)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mDetailsFragmentBinding = FragmentDetailsBinding.bind(view)

        mMusicRepository = MusicRepository.getInstance()

        setupViews()
    }

    private fun setupViews() {
        if (sLaunchedByArtistView) {
            mMusicRepository.deviceAlbumsByArtist?.get(mSelectedArtistOrFolder)
                ?.let { selectedArtistAlbums ->
                    mSelectedArtistAlbums = selectedArtistAlbums
                }

            mSongsList = getSongSource()

            mSelectedAlbum = when {
                mSelectedAlbumPosition != -1 -> mSelectedArtistAlbums?.get(
                    mSelectedAlbumPosition
                )
                else -> {
                    mSelectedAlbumPosition = 0
                    mSelectedArtistAlbums?.get(0)
                }
            }

        } else {
            mSongsList =
                getSongSource()
        }

        sLandscape = ThemeHelper.isDeviceLand(requireContext().resources)

        mDetailsFragmentBinding.detailsToolbar.apply {

            overflowIcon = AppCompatResources.getDrawable(
                requireContext(),
                if (sLaunchedByArtistView) {
                    R.drawable.ic_shuffle
                } else {
                    R.drawable.ic_more_vert
                }
            )

            title = mSelectedArtistOrFolder

            // Make toolbar's title scrollable
            getTitleTextView(this)?.let { tV ->
                tV.isSelected = true
                tV.setHorizontallyScrolling(true)
                tV.ellipsize = TextUtils.TruncateAt.MARQUEE
                tV.marqueeRepeatLimit = -1
            }

            setupToolbarSpecs()

            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }

            setupMenu()
        }

        if (sLaunchedByArtistView) {

            setupAlbumsContainer(requireContext())

        } else {

            mDetailsFragmentBinding.albumsRv.handleViewVisibility(false)
            selected_album_container.handleViewVisibility(false)

            mSongsList =
                getSongSource()

            mDetailsFragmentBinding.detailsToolbar.subtitle = getString(
                R.string.folder_info,
                mSongsList?.size
            )

            val searchView =
                mDetailsFragmentBinding.detailsToolbar.menu.findItem(R.id.action_search).actionView as SearchView
            searchView.apply {
                setOnQueryTextListener(this@DetailsFragment)
                setOnQueryTextFocusChangeListener { _, hasFocus ->
                    mDetailsFragmentBinding.detailsToolbar.menu.setGroupVisible(
                        R.id.more_options_folder,
                        !hasFocus
                    )
                }
            }
        }

        setSongsDataSource(
            if (sLaunchedByArtistView) {
                mSelectedAlbum?.music
            } else {
                mSongsList
            }
        )

        mDetailsFragmentBinding.songsRv.apply {

            // setup{} is an extension method on RecyclerView
            setup {
                // item is a `val` in `this` here
                withDataSource(mSongsDataSource)

                if (!sLandscape) {
                    addItemDecoration(ThemeHelper.getRecyclerViewDivider(requireContext()))
                }

                withItem<Music, GenericViewHolder>(R.layout.generic_item) {
                    onBind(::GenericViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        title.text = getString(
                            R.string.track_song,
                            item.track.toFormattedTrack(),
                            item.title
                        ).toSpanned()
                        subtitle.text = item.duration.toFormattedDuration(
                            isAlbum = false,
                            isSeekBar = false
                        )
                    }

                    onClick {

                        val selectedPlaylist =
                            if (sLaunchedByFolderView) {
                                mSongsList
                            } else {
                                MusicOrgHelper.getAlbumSongs(
                                    item.artist,
                                    item.album
                                )
                            }

                        mMediaPlayerInterface?.onSongSelected(
                            item,
                            selectedPlaylist,
                            launchedBy
                        )
                    }

                    onLongClick { index ->
                        DialogHelper.showDoSomethingPopup(
                            requireContext(),
                            findViewHolderForAdapterPosition(index)?.itemView,
                            item,
                            launchedBy
                        )
                    }
                }
            }

            addBidirectionalSwipeHandler(false) { viewHolder: RecyclerView.ViewHolder,
                                                  _: Int ->
                MediaPlayerHolder.getInstance().mediaPlayerInterface?.onAddToQueue(mSongsList!![viewHolder.adapterPosition])
                adapter?.notifyDataSetChanged()
            }
        }

        if (sLaunchedByArtistView) {
            mDetailsFragmentBinding.sortButton.apply {
                setOnClickListener {
                    mSongsSorting = ListsHelper.getSongsSorting(mSongsSorting)
                    setImageResource(ThemeHelper.resolveSortAlbumSongsIcon(mSongsSorting))
                    setSongsDataSource(
                        ListsHelper.getSortedMusicList(
                            mSongsSorting,
                            mSelectedAlbum?.music
                        )
                    )
                }
            }
        }
    }

    private fun setAlbumsDataSource(albumsList: List<Album>?) {
        albumsList?.apply {
            mSelectedAlbumsDataSource.set(this)
        }
    }

    private fun setSongsDataSource(musicList: List<Music>?) {
        if (sLaunchedByArtistView) {
            mDetailsFragmentBinding.sortButton.apply {
                isEnabled = mSelectedAlbum?.music?.size!! >= 2
                ThemeHelper.updateIconTint(
                    this,
                    if (isEnabled) {
                        R.color.widgetsColor.decodeColor(requireContext())
                    } else {
                        ThemeHelper.resolveColorAttr(
                            requireContext(),
                            android.R.attr.colorButtonNormal
                        )
                    }
                )
            }
            mDetailsFragmentBinding.detailsToolbar.menu.findItem(R.id.action_shuffle_sa).isEnabled =
                mSelectedAlbum?.music?.size!! >= 2
        }
        musicList?.apply {
            mSongsDataSource.set(this)
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setSongsDataSource(
            ListsHelper.processQueryForMusic(newText, mSongsList)
                ?: mSongsList
        )
        return false
    }

    override fun onQueryTextSubmit(query: String?) = false

    private fun setupMenu() {

        mDetailsFragmentBinding.detailsToolbar.apply {

            val menuToInflate =
                if (sLaunchedByArtistView) {
                    R.menu.menu_artist_details
                } else {
                    R.menu.menu_folder_details
                }

            inflateMenu(menuToInflate)

            menu.apply {
                findItem(R.id.action_shuffle_am).isEnabled =
                    if (sLaunchedByArtistView) {
                        mSelectedArtistAlbums?.size!! >= 2
                    } else {
                        mSongsList?.size!! >= 2
                    }

                findItem(R.id.action_shuffle_sa).isEnabled = sLaunchedByArtistView
                if (!sLaunchedByArtistView) {
                    findItem(R.id.sorting).isEnabled =
                        mSongsList?.size!! >= 2
                }
            }

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_shuffle_am -> mMediaPlayerInterface?.onShuffleSongs(
                        mSongsList?.toMutableList(),
                        launchedBy
                    )
                    R.id.action_shuffle_sa -> mMediaPlayerInterface?.onShuffleSongs(
                        mSelectedAlbum?.music,
                        launchedBy
                    )
                    R.id.default_sorting -> applySortingToMusic(GoConstants.DEFAULT_SORTING)
                    R.id.descending_sorting -> applySortingToMusic(GoConstants.DESCENDING_SORTING)
                    R.id.ascending_sorting -> applySortingToMusic(GoConstants.ASCENDING_SORTING)
                    R.id.track_sorting -> applySortingToMusic(GoConstants.TRACK_SORTING)
                    R.id.track_sorting_inv -> applySortingToMusic(GoConstants.TRACK_SORTING_INVERTED)
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun applySortingToMusic(order: Int) {
        val selectedList = mMusicRepository.deviceMusicByFolder?.get(mSelectedArtistOrFolder)
        mSongsList = ListsHelper.getSortedMusicList(
            order,
            selectedList?.toMutableList()
        )
        setSongsDataSource(mSongsList)
    }

    private fun setupToolbarSpecs() {
        mDetailsFragmentBinding.detailsToolbar.apply {
            elevation = if (!sLaunchedByArtistView) {
                resources.getDimensionPixelSize(R.dimen.search_bar_elevation).toFloat()
            } else {
                0F
            }

            val params = layoutParams as LinearLayout.LayoutParams
            params.bottomMargin = if (!sLaunchedByArtistView) {
                0
            } else {
                resources.getDimensionPixelSize(R.dimen.player_controls_padding_normal)
            }
        }
    }

    private fun setupAlbumsContainer(context: Context) {

        selected_album_container.setOnClickListener {
            mAlbumsRecyclerViewLayoutManager.scrollToPositionWithOffset(
                mSelectedAlbumPosition,
                0
            )
        }

        mDetailsFragmentBinding.selectedAlbum.isSelected = true

        updateSelectedAlbumTitle()

        setAlbumsDataSource(mSelectedArtistAlbums)

        mDetailsFragmentBinding.detailsToolbar.subtitle = getString(
            R.string.artist_info,
            mSelectedArtistAlbums?.size,
            mSongsList?.size
        )

        mDetailsFragmentBinding.albumsRv.apply {

            setup {

                withDataSource(mSelectedAlbumsDataSource)

                mAlbumsRecyclerViewLayoutManager = LinearLayoutManager(
                    context,
                    if (sLandscape) {
                        LinearLayoutManager.VERTICAL
                    } else {
                        LinearLayoutManager.HORIZONTAL
                    },
                    false
                )
                withLayoutManager(mAlbumsRecyclerViewLayoutManager)

                withItem<Album, AlbumsViewHolder>(R.layout.album_item) {

                    onBind(::AlbumsViewHolder) { _, item ->
                        // AlbumsViewHolder is `this` here

                        if (goPreferences.isCovers) {
                            imageView.loadCover(mImageLoader, item.music?.get(0))
                        }

                        album.text = item.title
                        val color =
                            ThemeHelper.resolveColorAttr(context, android.R.attr.textColorPrimary)
                        val selectedColor = if (mSelectedAlbum?.title == item.title) {
                            ThemeHelper.resolveThemeAccent(context)
                        } else {
                            color
                        }
                        album.setTextColor(selectedColor)
                    }

                    onClick { index ->

                        if (index != mSelectedAlbumPosition) {

                            mDetailsFragmentBinding.albumsRv.adapter?.apply {
                                notifyItemChanged(
                                    mSelectedAlbumPosition
                                )

                                notifyItemChanged(index)

                                mSelectedAlbum = item
                                mSelectedAlbumPosition = index
                                updateSelectedAlbumTitle()
                                swapAlbum(item.music)
                            }
                        } else {
                            mMediaPlayerInterface?.onSongSelected(
                                item.music?.get(0),
                                item.music,
                                launchedBy
                            )
                        }
                    }
                }
            }
            if (mSelectedAlbumPosition != -1 || mSelectedAlbumPosition != 0) {
                mAlbumsRecyclerViewLayoutManager.scrollToPositionWithOffset(
                    mSelectedAlbumPosition,
                    0
                )
            }
        }
    }

    private fun updateSelectedAlbumTitle() {
        mDetailsFragmentBinding.selectedAlbum.text = mSelectedAlbum?.title
        album_year_duration.text = getString(
            R.string.year_and_duration,
            mSelectedAlbum?.totalDuration?.toFormattedDuration(isAlbum = true, isSeekBar = false),
            mSelectedAlbum?.year
        )
    }

    private fun swapAlbum(songs: MutableList<Music>?) {
        mSongsSorting = GoConstants.TRACK_SORTING
        mDetailsFragmentBinding.sortButton.setImageResource(
            ThemeHelper.resolveSortAlbumSongsIcon(
                mSongsSorting
            )
        )
        setSongsDataSource(songs)
        mDetailsFragmentBinding.songsRv.scrollToPosition(0)
    }
}
