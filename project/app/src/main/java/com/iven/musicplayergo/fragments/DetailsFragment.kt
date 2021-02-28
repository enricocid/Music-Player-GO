package com.iven.musicplayergo.fragments

import android.animation.Animator
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.google.android.material.card.MaterialCardView
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.ui.ItemSwipeCallback
import com.iven.musicplayergo.databinding.FragmentDetailsBinding
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.models.Album
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.DetailsAlbumsViewHolder
import com.iven.musicplayergo.ui.GenericViewHolder
import com.iven.musicplayergo.ui.UIControlInterface
import kotlinx.coroutines.*


/**
 * A simple [Fragment] subclass.
 * Use the [DetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class DetailsFragment : Fragment(R.layout.fragment_details), SearchView.OnQueryTextListener {

    private var _detailsFragmentBinding: FragmentDetailsBinding? = null

    // View model
    private lateinit var mMusicViewModel: MusicViewModel

    private lateinit var mArtistDetailsAnimator: Animator
    private lateinit var mAlbumsRecyclerViewLayoutManager: LinearLayoutManager

    private val mSelectedAlbumsDataSource = dataSourceOf()
    private val mSongsDataSource = dataSourceOf()

    private var mLaunchedBy = GoConstants.ARTIST_VIEW

    private var mSelectedArtistAlbums: List<Album>? = null
    private var mSongsList: List<Music>? = null

    private var mSelectedArtistOrFolder: String? = null
    private var mSelectedAlbumPosition = -1

    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mMediaControlInterface: MediaControlInterface

    private var mSelectedAlbum: Album? = null

    private var mSongsSorting = getDefSortingModeForArtistView()

    private val sLaunchedByArtistView get() = mLaunchedBy == GoConstants.ARTIST_VIEW
    private val sLaunchedByFolderView get() = mLaunchedBy == GoConstants.FOLDER_VIEW
    private val sLaunchedByAlbumView get() = mLaunchedBy == GoConstants.ALBUM_VIEW

    private val sShowDisplayName get() = goPreferences.songsVisualization != GoConstants.TITLE

    private var mShuffledAlbum: String? = null
    private var sWasShuffling: Pair<Boolean, String?> = Pair(false, null)

    private var sPlayFirstSong = true

    /**
     * This is the job for all coroutines started by this ViewModel.
     * Cancelling this job will cancel all coroutines started by this ViewModel.
     */
    private val mLoadCoverJob = SupervisorJob()

    private val mLoadCoverHandler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    private val mLoadCoverIoDispatcher = Dispatchers.IO + mLoadCoverJob + mLoadCoverHandler
    private val mLoadCoverIoScope = CoroutineScope(mLoadCoverIoDispatcher)

    private val sIsCovers get() = goPreferences.isCovers
    private lateinit var mImageLoader: ImageLoader
    private var mAlbumArt: Bitmap? = null

    private var sOpenNewDetailsFragment = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_ARTIST_FOLDER)?.let { selectedArtistOrFolder ->
            mSelectedArtistOrFolder = selectedArtistOrFolder
        }

        arguments?.getString(TAG_IS_FOLDER)?.let { launchedBy ->
            mLaunchedBy = launchedBy
        }

        if (sLaunchedByArtistView) {
            arguments?.getInt(TAG_SELECTED_ALBUM_POSITION)?.let { selectedAlbumPosition ->
                mSelectedAlbumPosition = selectedAlbumPosition
            }
        }

        arguments?.getBoolean(TAG_IS_SHUFFLING)?.let { isShuffleMode ->
            if (isShuffleMode) {
                mSongsSorting = GoConstants.SHUFFLE_SORTING
            }
        }

        arguments?.getString(TAG_SHUFFLED_ALBUM)?.let { selectedAlbum ->
            mShuffledAlbum = selectedAlbum
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
            mMediaControlInterface = activity as MediaControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (sIsCovers) {
            mLoadCoverJob.cancel()
        }
    }

    fun onHandleBackPressed(): Animator {
        if (!mArtistDetailsAnimator.isRunning) {
            _detailsFragmentBinding?.root?.run {
                mArtistDetailsAnimator = createCircularReveal(
                        isErrorFragment = false,
                        show = false
                )
            }
        }
        return mArtistDetailsAnimator
    }

    private fun getSongSource(): List<Music>? {
        return when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW -> {
                mMusicViewModel.deviceAlbumsByArtist?.get(mSelectedArtistOrFolder)
                    ?.let { selectedArtistAlbums ->
                        mSelectedArtistAlbums = selectedArtistAlbums
                    }
                mMusicViewModel.deviceSongsByArtist?.get(mSelectedArtistOrFolder)
            }

            GoConstants.FOLDER_VIEW ->
                mMusicViewModel.deviceMusicByFolder?.get(mSelectedArtistOrFolder)

            else ->
                mMusicViewModel.deviceMusicByAlbum?.get(mSelectedArtistOrFolder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _detailsFragmentBinding = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _detailsFragmentBinding = FragmentDetailsBinding.inflate(inflater, container, false)
        return _detailsFragmentBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMusicViewModel =
            ViewModelProvider(requireActivity()).get(MusicViewModel::class.java).apply {
                deviceMusic.observe(viewLifecycleOwner, { returnedMusic ->
                    if (!returnedMusic.isNullOrEmpty()) {
                        mSongsList = getSongSource()

                        if (sLaunchedByArtistView) {
                            mSelectedAlbum = when {
                                mSelectedAlbumPosition != -1 -> mSelectedArtistAlbums?.get(mSelectedAlbumPosition)
                                else -> {
                                    mSelectedAlbumPosition = 0
                                    mSelectedArtistAlbums?.get(0)
                                }
                            }
                        }

                        setupToolbar()

                        setupViews(view)
                    }
                })
            }
    }

    private fun setupToolbar() {
        _detailsFragmentBinding?.detailsToolbar?.run {

            overflowIcon = ContextCompat.getDrawable(
                requireActivity(),
                if (sLaunchedByArtistView) {
                    R.drawable.ic_shuffle
                } else {
                    R.drawable.ic_more_vert
                }
            )

            if (sLaunchedByArtistView || sLaunchedByFolderView) {
                title = mSelectedArtistOrFolder
                // Make toolbar's title scrollable
                this.getTitleTextView()?.let { tV ->
                    tV.isSelected = true
                    tV.setHorizontallyScrolling(true)
                    tV.ellipsize = TextUtils.TruncateAt.MARQUEE
                    tV.marqueeRepeatLimit = -1
                }
            }

            setupToolbarSpecs()

            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }

            setupMenu()
        }
    }

    private fun setupToolbarSpecs() {
        _detailsFragmentBinding?.detailsToolbar?.run {

            elevation = if (sLaunchedByFolderView) {
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

    private fun setupViews(view: View) {

        if (sIsCovers && sLaunchedByArtistView || sIsCovers && sLaunchedByAlbumView) {
            mImageLoader = ImageLoader.Builder(requireActivity())
                    .bitmapPoolingEnabled(false)
                    .crossfade(true)
                    .build()
            mAlbumArt = ContextCompat.getDrawable(requireActivity(), R.drawable.album_art)?.toBitmap()
        }

        if (sLaunchedByArtistView) {

            _detailsFragmentBinding?.albumViewCoverContainer?.handleViewVisibility(false)
            if (mSongsSorting == GoConstants.SHUFFLE_SORTING) {
                sWasShuffling = Pair(true, mShuffledAlbum)
            }

            setupAlbumsContainer()

            _detailsFragmentBinding?.sortButton?.run {
                setImageResource(getDefSortingIconForArtistView(mSongsSorting))
                setOnClickListener {
                    mSongsSorting = if (sShowDisplayName) {
                        ListsHelper.getSongsDisplayNameSorting(mSongsSorting)
                    } else {
                        ListsHelper.getSongsSorting(mSongsSorting)
                    }
                    setSongsDataSource(mSelectedAlbum?.music, true)
                }
            }

            _detailsFragmentBinding?.queueAddButton?.setOnClickListener {
                val queuedMusicSorted = ListsHelper.getSortedMusicList(mSongsSorting, mSelectedAlbum?.music)
                mMediaControlInterface.onAddAlbumToQueue(
                    queuedMusicSorted,
                    Pair(true, queuedMusicSorted?.get(0)),
                    isLovedSongs = false,
                    isShuffleMode = sWasShuffling.first,
                    clearShuffleMode = false,
                    mLaunchedBy
                )
            }

        } else {

            _detailsFragmentBinding?.run {

                albumsRv.handleViewVisibility(false)
                selectedAlbumContainer.handleViewVisibility(false)

                if (sLaunchedByFolderView) {

                    albumViewCoverContainer.handleViewVisibility(false)
                    detailsToolbar.subtitle = getString(
                            R.string.folder_info,
                            mSongsList?.size
                    )
                } else {

                    val firstSong = mSongsList?.get(0)
                    selectedAlbumViewTitle.text = mSelectedArtistOrFolder
                    selectedAlbumViewTitle.isSelected = true
                    selectedAlbumViewArtist.text = firstSong?.artist
                    selectedAlbumViewArtist.isSelected = true
                    selectedAlbumViewSize.text = getString(
                            R.string.folder_info,
                            mSongsList?.size
                    )
                    selectedAlbumViewSize.isSelected = true
                    loadCoverIntoTarget(firstSong, albumViewArt)
                }

                val searchView =
                        detailsToolbar.menu.findItem(R.id.action_search).actionView as SearchView
                searchView.run {
                    setOnQueryTextListener(this@DetailsFragment)
                    setOnQueryTextFocusChangeListener { _, hasFocus ->
                        detailsToolbar.menu.setGroupVisible(
                                R.id.more_options_folder,
                                !hasFocus
                        )
                        if (sLaunchedByAlbumView) {
                            albumViewCoverContainer.afterMeasured {
                                animate()?.let { anim ->
                                    anim.duration = 500
                                    var newY = 0F
                                    if (hasFocus) {
                                        newY = -(height.toFloat() + detailsToolbar.height)
                                        anim.withEndAction { albumViewCoverContainer.handleViewVisibility(false) }
                                    } else {
                                        anim.withStartAction { albumViewCoverContainer.handleViewVisibility(true) }
                                    }
                                    anim.translationY(newY)
                                }
                            }
                        }
                    }
                }
            }
        }

        setSongsDataSource(
                if (sLaunchedByArtistView) {
                    mSelectedAlbum?.music
                } else {
                    mSongsList
                },
                true
        )

        _detailsFragmentBinding?.songsRv?.let { rv ->

            // setup{} is an extension method on RecyclerView
            rv.setup {
                // item is a `val` in `this` here
                withDataSource(mSongsDataSource)

                withItem<Music, GenericViewHolder>(R.layout.generic_item) {
                    onBind(::GenericViewHolder) { _, item ->

                        val displayedTitle =
                            if (sShowDisplayName || sLaunchedByFolderView) {
                                item.displayName?.toFilenameWithoutExtension()
                            } else {
                                getString(
                                    R.string.track_song,
                                    item.track.toFormattedTrack(),
                                    item.title
                                ).parseAsHtml()
                            }

                        // GenericViewHolder is `this` here
                        title.text = displayedTitle

                        val duration = item.duration.toFormattedDuration(
                                isAlbum = false,
                                isSeekBar = false
                        )

                        subtitle.text = if (sLaunchedByFolderView) {
                            getString(R.string.duration_artist_date_added, duration, item.artist, item.dateAdded.toFormattedDate())
                        } else {
                            duration
                        }
                    }

                    onClick {

                        val selectedPlaylist = if (sLaunchedByArtistView) {
                            mSelectedAlbum?.music
                        } else {
                            if (sWasShuffling.first && item.album == mSelectedArtistOrFolder) {
                                mSongsList = getSongSource()
                            }
                            mSongsList
                        }

                        mMediaControlInterface.onSongSelected(
                            item,
                            selectedPlaylist?.toMutableList(),
                            mLaunchedBy
                        )
                    }

                    onLongClick { index ->
                        DialogHelper.showPopupForSongs(
                            requireActivity(),
                            rv.findViewHolderForAdapterPosition(index)?.itemView,
                            item,
                            mLaunchedBy
                        )
                    }
                }
            }

            ItemTouchHelper(ItemSwipeCallback(requireActivity(), false) { viewHolder: RecyclerView.ViewHolder,
                                                                          direction: Int ->
                val song = mSelectedAlbum?.music?.get(viewHolder.adapterPosition)
                if (direction == ItemTouchHelper.RIGHT) {
                    mMediaControlInterface.onAddToQueue(
                            song,
                            mLaunchedBy
                    )
                } else {
                    DialogHelper.addToLovedSongs(requireActivity(), song, mLaunchedBy)
                }
                rv.adapter?.notifyDataSetChanged()
            }).attachToRecyclerView(rv)
        }

        if (goPreferences.isAnimations) {
            view.afterMeasured {
                _detailsFragmentBinding?.root?.run {
                    mArtistDetailsAnimator = createCircularReveal(
                            isErrorFragment = false,
                            show = true
                    )
                }
            }
        }
    }

    private fun setAlbumsDataSource(albumsList: List<Album>?) {
        albumsList?.let { albums ->
            mSelectedAlbumsDataSource.set(albums)
        }
    }

    private fun setSongsDataSource(musicList: List<Music>?, isApplySorting: Boolean) {

        val songs = if (isApplySorting) {
            if (sLaunchedByFolderView) {
                ListsHelper.getSortedMusicListForFolder(mSongsSorting, musicList?.toMutableList())
            } else {
                ListsHelper.getSortedMusicList(mSongsSorting, musicList?.toMutableList())
            }
        } else {
            musicList
        }

        if (sLaunchedByArtistView) {
            _detailsFragmentBinding?.let { _binding ->
                _binding.sortButton.run {
                    if (sWasShuffling.first && sWasShuffling.second == mSelectedAlbum?.title) {
                        setImageResource(getDefSortingIconForArtistView(mSongsSorting))
                    } else {
                        setImageResource(getDefSortingIconForArtistView(mSongsSorting))
                        isEnabled = mSelectedAlbum?.music?.size!! >= 2
                        ThemeHelper.updateIconTint(
                                this,
                                if (isEnabled) {
                                    ContextCompat.getColor(requireActivity(), R.color.widgetsColor)
                                } else {
                                    ThemeHelper.resolveColorAttr(
                                            requireActivity(),
                                            android.R.attr.colorButtonNormal
                                    )
                                }
                        )
                    }
                }
                _binding.detailsToolbar.menu.findItem(R.id.action_shuffle_sa).isEnabled =
                        mSelectedAlbum?.music?.size!! >= 2
            }
        }

        songs?.let { newSongsList ->
            mSongsDataSource.set(newSongsList)
        }
    }

    private fun getDefSortingIconForArtistView(sorting: Int) = if (sShowDisplayName) {
        ThemeHelper.getSortIconForSongsDisplayName(
                sorting
        )
    } else {
        ThemeHelper.getSortIconForSongs(
                sorting
        )
    }

    private fun getDefSortingModeForArtistView() = if (sShowDisplayName) {
        GoConstants.ASCENDING_SORTING
    } else {
        GoConstants.TRACK_SORTING
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setSongsDataSource(
            ListsHelper.processQueryForMusic(newText, mSongsList)
                ?: mSongsList,
            true
        )
        return false
    }

    override fun onQueryTextSubmit(query: String?) = false

    private fun setupMenu() {

        _detailsFragmentBinding?.detailsToolbar?.let { tb ->

            val menuToInflate = when {
                sLaunchedByArtistView -> R.menu.menu_artist_details
                sLaunchedByFolderView -> R.menu.menu_folder_details
                else -> R.menu.menu_album_details
            }

            tb.inflateMenu(menuToInflate)

            with (tb.menu) {
                when {
                    sLaunchedByArtistView -> {
                        findItem(R.id.action_shuffle_am).isEnabled = mSelectedArtistAlbums?.size!! >= 2
                        findItem(R.id.action_shuffle_sa).isEnabled = mSelectedAlbum?.music?.size!! >= 2
                    }
                    sLaunchedByFolderView -> {
                        findItem(R.id.action_shuffle_am).isEnabled = mSongsList?.size!! >= 2
                        findItem(R.id.action_shuffle_sa).isEnabled = false
                        findItem(R.id.sorting).isEnabled =
                                mSongsList?.size!! >= 2
                    }
                    else -> {
                        findItem(R.id.action_shuffle_am).isEnabled = mSongsList?.size!! >= 2
                        findItem(R.id.action_shuffle_sa).isEnabled = false
                        findItem(R.id.track_sorting).isEnabled = !sShowDisplayName
                        findItem(R.id.track_sorting_inv).isEnabled = !sShowDisplayName
                        findItem(R.id.sorting).isEnabled =
                                mSongsList?.size!! >= 2
                    }
                }
            }

            tb.setOnMenuItemClickListener {

                when (it.itemId) {
                    R.id.action_add_queue -> mMediaControlInterface.onAddAlbumToQueue(
                        mSongsList?.toMutableList(),
                        Pair(true, mSongsList?.get(0)),
                        isLovedSongs = false,
                        isShuffleMode = sWasShuffling.first,
                        clearShuffleMode = false,
                        mLaunchedBy
                    )
                    R.id.action_shuffle_am -> {
                        onDisableShuffle(isShuffleMode = false, isMusicListOutputRequired = false)
                        mMediaControlInterface.onShuffleSongs(
                                null,
                                null,
                                mSongsList?.toMutableList(),
                                true,
                                mLaunchedBy
                        )
                    }
                    R.id.action_shuffle_sa -> {
                        sWasShuffling = Pair(true, mSelectedAlbum?.title)
                        val music = mMediaControlInterface.onShuffleSongs(
                            mSelectedAlbum?.title,
                            mSelectedArtistAlbums,
                            mSelectedAlbum?.music,
                            true,
                            mLaunchedBy
                        )
                        mSongsSorting = GoConstants.SHUFFLE_SORTING

                        if (sLaunchedByArtistView) {
                            _detailsFragmentBinding?.sortButton?.setImageResource(getDefSortingIconForArtistView(mSongsSorting))
                        }
                        setSongsDataSource(music, true)
                    }
                    R.id.default_sorting -> applySortingToMusic(GoConstants.DEFAULT_SORTING)
                    R.id.ascending_sorting -> applySortingToMusic(GoConstants.ASCENDING_SORTING)
                    R.id.descending_sorting -> applySortingToMusic(GoConstants.DESCENDING_SORTING)
                    R.id.track_sorting -> applySortingToMusic(GoConstants.TRACK_SORTING)
                    R.id.track_sorting_inv -> applySortingToMusic(GoConstants.TRACK_SORTING_INVERTED)
                    R.id.date_added_sorting -> applySortingToMusic(GoConstants.DATE_ADDED_SORTING)
                    R.id.date_added_sorting_inv -> applySortingToMusic(GoConstants.DATE_ADDED_SORTING_INV)
                    R.id.artist_sorting -> applySortingToMusic(GoConstants.ARTIST_SORTING)
                    R.id.artist_sorting_inv-> applySortingToMusic(GoConstants.ARTIST_SORTING_INV)
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun applySortingToMusic(order: Int) {
        mSongsSorting = order
        val selectedList = if (sLaunchedByFolderView) {
            mMusicViewModel.deviceMusicByFolder?.get(mSelectedArtistOrFolder)
        } else {
            mMusicViewModel.deviceMusicByAlbum?.get(mSelectedArtistOrFolder)
        }
        if (!sWasShuffling.first) {
            setSongsDataSource(selectedList, true)
        }
    }

    private fun setupAlbumsContainer() {

        _detailsFragmentBinding?.selectedAlbumContainer?.setOnClickListener {
            mAlbumsRecyclerViewLayoutManager.scrollToPositionWithOffset(
                mSelectedAlbumPosition,
                0
            )
        }

        _detailsFragmentBinding?.selectedAlbum?.isSelected = true

        updateSelectedAlbumTitle()

        setAlbumsDataSource(mSelectedArtistAlbums)

        _detailsFragmentBinding?.detailsToolbar?.subtitle = getString(
            R.string.artist_info,
            mSelectedArtistAlbums?.size,
            mSongsList?.size
        )

        _detailsFragmentBinding?.albumsRv?.run {

            setHasFixedSize(true)

            setup {

                withDataSource(mSelectedAlbumsDataSource)

                mAlbumsRecyclerViewLayoutManager = layoutManager as LinearLayoutManager

                withItem<Album, DetailsAlbumsViewHolder>(R.layout.album_item) {

                    onBind(::DetailsAlbumsViewHolder) { _, item ->
                        // AlbumsViewHolder is `this` here

                        val cardView = itemView as MaterialCardView

                        album.text = item.title

                        year.text = item.year
                        totalDuration.text = item.totalDuration.toFormattedDuration(
                            isAlbum = true,
                            isSeekBar = false
                        )

                        cardView.strokeWidth = if (mSelectedAlbum?.title == item.title) {
                            resources.getDimensionPixelSize(R.dimen.album_stroke)
                        } else {
                            0
                        }

                        if (sIsCovers) {
                            loadCoverIntoTarget(item.music?.get(0), albumCover)
                        }
                    }

                    onClick { index ->

                        if (index != mSelectedAlbumPosition) {

                            adapter?.let { albumsAdapter ->

                                albumsAdapter.notifyItemChanged(
                                    mSelectedAlbumPosition
                                )

                                albumsAdapter.notifyItemChanged(index)

                                mSelectedAlbum = item
                                mSelectedAlbumPosition = index

                                updateSelectedAlbumTitle()

                                swapAlbum(item.title, item.music)
                            }
                        } else {
                            if (sPlayFirstSong) {
                                mMediaControlInterface.onSongSelected(
                                    ListsHelper.getSortedMusicList(mSongsSorting, item.music?.toMutableList())?.get(0),
                                    item.music,
                                    mLaunchedBy
                                )
                            } else {
                                sPlayFirstSong = true
                            }
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

    override fun onDetach() {
        super.onDetach()
        if (sOpenNewDetailsFragment) {
            mUIControlInterface.onOpenNewDetailsFragment()
            sOpenNewDetailsFragment = false
        }
    }

    private fun loadCoverIntoTarget(song: Music?, target: ImageView) {
        val request = ImageRequest.Builder(requireActivity())
                .data(song?.albumId?.getCoverFromURI())
                .target(
                        onSuccess = { result ->
                            // Handle the successful result.
                            target.load(result)
                        },
                        onError = {
                            target.load(mAlbumArt)
                        }
                )
                .build()

        mLoadCoverIoScope.launch {
            withContext(mLoadCoverIoDispatcher) {
                mImageLoader.enqueue(request)
            }
        }
    }

    fun hasToUpdate(selectedArtistOrFolder: String?) : Boolean {
       sOpenNewDetailsFragment = selectedArtistOrFolder != mSelectedArtistOrFolder
       return sOpenNewDetailsFragment
    }

    fun tryToSnapToAlbumPosition(snapPosition: Int) {
        sPlayFirstSong = false
        if (sLaunchedByArtistView && snapPosition != -1) {
            _detailsFragmentBinding?.albumsRv?.smoothSnapToPosition(
                snapPosition
            )
        }
    }

    fun onDisableShuffle(isShuffleMode: Boolean, isMusicListOutputRequired: Boolean) : List<Music>? {
        val songsSource = if (sLaunchedByArtistView) {
            mSelectedAlbum?.music
        } else {
            mSongsList
        }
        val selectedSongs = ListsHelper.getSortedMusicList(mSongsSorting, songsSource?.toMutableList())
        if (sWasShuffling.first && !isShuffleMode) {
            mSongsSorting = getDefSortingModeForArtistView()
            if (mSelectedAlbum?.title == sWasShuffling.second) {
                setSongsDataSource(selectedSongs, false)
            }
            sWasShuffling = Pair(false, null)
        }
        return if (isMusicListOutputRequired) {
            selectedSongs
        } else {
            null
        }
    }

    private fun updateSelectedAlbumTitle() {
        _detailsFragmentBinding?.run {
            selectedAlbum.text = mSelectedAlbum?.title
            albumYearDuration.text = getString(
                    R.string.year_and_duration,
                    mSelectedAlbum?.totalDuration?.toFormattedDuration(isAlbum = true, isSeekBar = false),
                    mSelectedAlbum?.year
            )
        }
    }

    private fun swapAlbum(title: String?, songs: MutableList<Music>?) {
        mSongsSorting = if (sWasShuffling.first && sWasShuffling.second == title) {
            GoConstants.SHUFFLE_SORTING
        } else {
            if (sShowDisplayName) {
                GoConstants.ASCENDING_SORTING
            } else {
                GoConstants.TRACK_SORTING
            }
        }
        setSongsDataSource(songs, true)
        _detailsFragmentBinding?.songsRv?.scrollToPosition(0)
    }

    companion object {

        private const val TAG_ARTIST_FOLDER = "SELECTED_ARTIST_FOLDER"
        private const val TAG_IS_FOLDER = "IS_FOLDER"
        private const val TAG_SELECTED_ALBUM_POSITION = "SELECTED_ALBUM_POSITION"
        private const val TAG_IS_SHUFFLING = "IS_SHUFFLING"
        private const val TAG_SHUFFLED_ALBUM = "SHUFFLED_ALBUM"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment DetailsFragment.
         */
        @JvmStatic
        fun newInstance(
            selectedArtistOrFolder: String?,
            launchedBy: String,
            playedAlbumPosition: Int,
            isShuffleMode: Pair<Boolean, String?>,
        ) =
            DetailsFragment().apply {
                arguments = bundleOf(
                    TAG_ARTIST_FOLDER to selectedArtistOrFolder,
                    TAG_IS_FOLDER to launchedBy,
                    TAG_SELECTED_ALBUM_POSITION to playedAlbumPosition,
                    TAG_IS_SHUFFLING to isShuffleMode.first,
                    TAG_SHUFFLED_ALBUM to isShuffleMode.second
                )
            }
    }
}
