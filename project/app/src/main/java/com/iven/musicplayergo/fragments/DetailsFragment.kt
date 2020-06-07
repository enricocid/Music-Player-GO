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
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicRepository
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentDetailsBinding
import com.iven.musicplayergo.enums.LaunchedBy
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.MusicOrgHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.models.Album
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.ui.AlbumsViewHolder
import com.iven.musicplayergo.ui.GenericViewHolder
import com.iven.musicplayergo.ui.UIControlInterface
import kotlinx.android.synthetic.main.fragment_details.*


/**
 * A simple [Fragment] subclass.
 * Use the [DetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class DetailsFragment : Fragment(R.layout.fragment_details), SearchView.OnQueryTextListener {

    private lateinit var mDetailsFragmentBinding: FragmentDetailsBinding
    private lateinit var mMusicRepository: MusicRepository

    private lateinit var mArtistDetailsAnimator: Animator
    private lateinit var mAlbumsRecyclerViewLayoutManager: LinearLayoutManager

    private val mSelectedAlbumsDataSource = dataSourceOf()
    private val mSongsDataSource = dataSourceOf()

    private var launchedBy: LaunchedBy = LaunchedBy.ArtistView

    private var mSelectedArtistAlbums: List<Album>? = null
    private var mSongsList: List<Music>? = null

    private var mSelectedArtistOrFolder: String? = null
    private var mSelectedAlbumPosition = -1

    private lateinit var mUIControlInterface: UIControlInterface
    private var mSelectedAlbum: Album? = null

    private var sLandscape = false

    private var mSongsSorting = GoConstants.TRACK_SORTING

    private fun isLaunchedByArtistView(): Boolean {
        return launchedBy == LaunchedBy.ArtistView
    }

    private fun isLaunchedByFolderView(): Boolean {
        return launchedBy == LaunchedBy.FolderView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_ARTIST_FOLDER)?.let { selectedArtistOrFolder ->
            mSelectedArtistOrFolder = selectedArtistOrFolder
        }

        arguments?.getInt(TAG_IS_FOLDER)?.let { launchedBy ->
            this.launchedBy = LaunchedBy.values()[launchedBy]
        }

        if (isLaunchedByArtistView())
            arguments?.getInt(TAG_SELECTED_ALBUM_POSITION)?.let { selectedAlbumPosition ->
                mSelectedAlbumPosition = selectedAlbumPosition
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
            mDetailsFragmentBinding.root.createCircularReveal(isErrorFragment = false, show = false)
        return mArtistDetailsAnimator
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
        if (isLaunchedByArtistView()) {

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
                if (isLaunchedByArtistView())
                    R.drawable.ic_shuffle
                else R.drawable.ic_more_vert
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

        if (isLaunchedByArtistView()) {

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
            if (isLaunchedByArtistView())
                mSelectedAlbum?.music
            else mSongsList
        )

        mDetailsFragmentBinding.songsRv.apply {

            // setup{} is an extension method on RecyclerView
            setup {
                // item is a `val` in `this` here
                withDataSource(mSongsDataSource)

                if (sLandscape)
                    withLayoutManager(GridLayoutManager(requireContext(), 2))
                else
                    addItemDecoration(ThemeHelper.getRecyclerViewDivider(requireContext()))

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
                            if (isLaunchedByFolderView()) mSongsList
                            else
                                MusicOrgHelper.getAlbumSongs(
                                    item.artist,
                                    item.album,
                                    mMusicRepository.deviceAlbumsByArtist
                                )

                        mUIControlInterface.onSongSelected(
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
                            launchedBy,
                            mUIControlInterface
                        )
                    }
                }
            }

            addBidirectionalSwipeHandler(false) { viewHolder: RecyclerView.ViewHolder,
                                                  _: Int ->
                mUIControlInterface.onAddToQueue(mSongsForArtistOrFolder!![viewHolder.adapterPosition])
                adapter?.notifyDataSetChanged()
            }
        }

        if (isLaunchedByArtistView()) {
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

        view.afterMeasured {
            mArtistDetailsAnimator =
                mDetailsFragmentBinding.root.createCircularReveal(
                    isErrorFragment = false,
                    show = true
                )
        }
    }

    private fun setAlbumsDataSource(albumsList: List<Album>?) {
        albumsList?.apply {
            mSelectedAlbumsDataSource.set(this)
        }
    }

    private fun setSongsDataSource(musicList: List<Music>?) {
        if (isLaunchedByArtistView()) {
            mDetailsFragmentBinding.sortButton.apply {
                isEnabled = mSelectedAlbum?.music?.size!! >= 2
                ThemeHelper.updateIconTint(
                    this,
                    if (isEnabled) R.color.widgetsColor.decodeColor(requireContext()) else ThemeHelper.resolveColorAttr(
                        requireContext(),
                        android.R.attr.colorButtonNormal
                    )
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
                if (isLaunchedByArtistView())
                    R.menu.menu_artist_details
                else
                    R.menu.menu_folder_details

            inflateMenu(menuToInflate)

            menu.apply {
                findItem(R.id.action_shuffle_am).isEnabled =
                    if (isLaunchedByArtistView()) mSelectedArtistAlbums?.size!! >= 2
                    else mSongsList?.size!! >= 2

                findItem(R.id.action_shuffle_sa).isEnabled = isLaunchedByArtistView()
                if (!isLaunchedByArtistView()) findItem(R.id.sorting).isEnabled =
                    mSongsList?.size!! >= 2
            }

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_shuffle_am -> mUIControlInterface.onShuffleSongs(
                        mSongsList?.toMutableList(),
                        launchedBy
                    )
                    R.id.action_shuffle_sa -> mUIControlInterface.onShuffleSongs(
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
        mSongsList = ListsHelper.getSortedMusicList(
            order,
            mSongsList?.toMutableList()
        )
        setSongsDataSource(mSongsList)
    }

    private fun setupToolbarSpecs() {
        mDetailsFragmentBinding.detailsToolbar.apply {
            elevation = if (!isLaunchedByArtistView())
                resources.getDimensionPixelSize(R.dimen.search_bar_elevation).toFloat() else 0F

            val params = layoutParams as LinearLayout.LayoutParams
            params.bottomMargin = if (!isLaunchedByArtistView())
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
                        totalDuration.text = item.totalDuration.toFormattedDuration(
                            isAlbum = true,
                            isSeekBar = false
                        )
                        checkbox.handleViewVisibility(mSelectedAlbum?.title == item.title)
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

    fun hasToUpdate(selectedArtistOrFolder: String?) =
        selectedArtistOrFolder != mSelectedArtistOrFolder

    fun tryToSnapToAlbumPosition(snapPosition: Int) {
        if (isLaunchedByArtistView() && snapPosition != -1)
            mDetailsFragmentBinding.albumsRv.smoothSnapToPosition(
                snapPosition
            )
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
            launchedBy: LaunchedBy,
            playedAlbumPosition: Int
        ) =
            DetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(TAG_ARTIST_FOLDER, selectedArtistOrFolder)
                    putInt(TAG_IS_FOLDER, launchedBy.ordinal)
                    putInt(TAG_SELECTED_ALBUM_POSITION, playedAlbumPosition)
                }
            }
    }
}
