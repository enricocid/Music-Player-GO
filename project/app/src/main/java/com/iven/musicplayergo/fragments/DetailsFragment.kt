package com.iven.musicplayergo.fragments

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentDetailsBinding
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.models.Album
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.ui.ItemSwipeCallback
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface
import kotlinx.coroutines.Runnable


/**
 * A simple [Fragment] subclass.
 * Use the [DetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class DetailsFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _detailsFragmentBinding: FragmentDetailsBinding? = null

    private lateinit var mMusicViewModel: MusicViewModel
    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mMediaControlInterface: MediaControlInterface

    private var mLaunchedBy = GoConstants.ARTIST_VIEW
    private val sLaunchedByArtistView get() = mLaunchedBy == GoConstants.ARTIST_VIEW
    private val sLaunchedByFolderView get() = mLaunchedBy == GoConstants.FOLDER_VIEW
    private val sLaunchedByAlbumView get() = mLaunchedBy == GoConstants.ALBUM_VIEW

    private lateinit var mArtistDetailsAnimator: Animator
    private lateinit var mAlbumsRecyclerViewLayoutManager: LinearLayoutManager

    private var mSelectedArtistOrFolder: String? = null
    private var mSelectedArtistAlbums: List<Album>? = null
    private var mSongsList: List<Music>? = null

    private var mSelectedAlbum: Album? = null
    private var mSelectedAlbumPosition = RecyclerView.NO_POSITION
    private var mSelectedSongId: Long? = null
    private var mSelectedSongPosition = RecyclerView.NO_POSITION

    private var mSongsSorting = getDefSortingModeForArtistView()

    private val sShowDisplayName get() = goPreferences.songsVisualization == GoConstants.FN

    private var sPlayFirstSong = true
    private var sCanUpdateSongs = true
    private var sAlbumSwapped = false
    private var sOpenNewDetailsFragment = false

    @SuppressLint("NotifyDataSetChanged")
    fun swapSelectedSong(songId: Long?) {
        mSelectedSongId = songId
        _detailsFragmentBinding?.songsRv?.adapter?.notifyDataSetChanged()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.run {
            getString(TAG_ARTIST_FOLDER)?.let { selectedArtistOrFolder ->
                mSelectedArtistOrFolder = selectedArtistOrFolder
            }
            getString(TAG_IS_FOLDER)?.let { launchedBy ->
                mLaunchedBy = launchedBy
            }
            if (sLaunchedByArtistView) {
                mSelectedAlbumPosition = getInt(TAG_SELECTED_ALBUM_POSITION)
            }
            mSelectedSongId = getLong(TAG_HIGHLIGHTED_SONG_ID)
            sCanUpdateSongs = getBoolean(TAG_CAN_UPDATE_SONGS)
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
                mSelectedArtistAlbums = mMusicViewModel.deviceAlbumsByArtist?.get(mSelectedArtistOrFolder)
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
                deviceMusic.observe(viewLifecycleOwner) { returnedMusic ->
                    if (!returnedMusic.isNullOrEmpty()) {

                        mSongsList = getSongSource()

                        if (sLaunchedByArtistView) {
                            mSelectedAlbum = when {
                                mSelectedAlbumPosition != RecyclerView.NO_POSITION -> mSelectedArtistAlbums?.get(
                                    mSelectedAlbumPosition
                                )
                                else -> {
                                    mSelectedAlbumPosition = 0
                                    mSelectedArtistAlbums?.first()
                                }
                            }
                        }

                        setupToolbar()
                        setupViews(view)
                    }
                }
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
                getTitleTextView()?.run {
                    isSelected = true
                    setHorizontallyScrolling(true)
                    ellipsize = TextUtils.TruncateAt.MARQUEE
                    marqueeRepeatLimit = -1
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

    @SuppressLint("NotifyDataSetChanged")
    private fun setupViews(view: View) {

        if (sLaunchedByArtistView) {

            _detailsFragmentBinding?.albumViewCoverContainer?.handleViewVisibility(show = false)

            setupAlbumsContainer()

            _detailsFragmentBinding?.sortButton?.run {
                setImageResource(getDefSortingIconForArtistView())
                setOnClickListener {
                    mSongsSorting = if (sShowDisplayName) {
                        ListsHelper.getSongsDisplayNameSorting(mSongsSorting)
                    } else {
                        ListsHelper.getSongsSorting(mSongsSorting)
                    }
                    setSongsDataSource(mSelectedAlbum?.music, updateSongs = !sAlbumSwapped && sCanUpdateSongs, updateAdapter = true)
                }
            }

            _detailsFragmentBinding?.queueAddButton?.setOnClickListener {
                mMediaControlInterface.onAddAlbumToQueue(
                    mSongsList, forcePlay = Pair(first = false, second = null)
                )
            }

        } else {

            _detailsFragmentBinding?.run {

                albumsRv.handleViewVisibility(show = false)
                selectedAlbumContainer.handleViewVisibility(show = false)

                if (sLaunchedByFolderView) {
                    albumViewCoverContainer.handleViewVisibility(show = false)
                    detailsToolbar.subtitle = getString(
                        R.string.folder_info,
                        mSongsList?.size
                    )
                } else {
                    val firstSong = mSongsList?.first()
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
                                        anim.withEndAction { albumViewCoverContainer.handleViewVisibility(show = false) }
                                    } else {
                                        anim.withStartAction { albumViewCoverContainer.handleViewVisibility(show = true) }
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
            updateSongs = false,
            updateAdapter = false
        )

        _detailsFragmentBinding?.songsRv?.run {

            adapter = SongsAdapter()

            ItemTouchHelper(ItemSwipeCallback(requireActivity(), isQueueDialog = false, isFavoritesDialog = false) { viewHolder: RecyclerView.ViewHolder,
                                                                          direction: Int ->
                val song = mSongsList?.get(viewHolder.absoluteAdapterPosition)
                if (direction == ItemTouchHelper.RIGHT) {
                    mMediaControlInterface.onAddToQueue(song)
                } else {
                    ListsHelper.addToFavorites(
                        requireActivity(),
                        song,
                        canRemove = false,
                        0,
                        mLaunchedBy
                    )
                    mUIControlInterface.onFavoriteAddedOrRemoved()
                }
                adapter?.notifyDataSetChanged()
            }).attachToRecyclerView(this)
        }

        view.afterMeasured {
            if (goPreferences.isAnimations) {
                _detailsFragmentBinding?.root?.run {
                    mArtistDetailsAnimator = createCircularReveal(
                        isErrorFragment = false,
                        show = true
                    )
                    mArtistDetailsAnimator.doOnEnd {
                        highlightSong(mSelectedSongId)
                    }
                }
            }
        }
    }

    fun highlightSong(songId: Long?) {

        if(songId == null) {
            return
        }

        mSongsList?.indexOfFirst { song -> song.id == songId }?.let { pos ->
            if (pos > -1) {
                var songView: View? = _detailsFragmentBinding?.songsRv?.layoutManager?.findViewByPosition(pos)
                if (songView == null) {
                    _detailsFragmentBinding?.songsRv?.addOnScrollListener(object :
                        RecyclerView.OnScrollListener() {
                        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                            super.onScrollStateChanged(recyclerView, newState)
                            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                                songView = _detailsFragmentBinding?.songsRv?.layoutManager?.findViewByPosition(pos)
                                animateHighlightedSong(songView)
                                _detailsFragmentBinding?.songsRv?.clearOnScrollListeners()
                            }
                        }
                    })
                    _detailsFragmentBinding?.songsRv?.smoothScrollToPosition(pos)
                } else {
                    animateHighlightedSong(songView)
                }
            }
        }
    }

    private fun animateHighlightedSong(songView: View?){
        songView?.let {
            val unpressedRunnable = Runnable {
                songView.isPressed = false
            }

            val pressRunnable = Runnable {
                songView.isPressed = true
                songView.postOnAnimationDelayed(unpressedRunnable, 1000)
            }
            pressRunnable.run()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setSongsDataSource(musicList: List<Music>?, updateSongs: Boolean, updateAdapter: Boolean) {

        val songs = if (sLaunchedByFolderView) {
            ListsHelper.getSortedMusicListForFolder(mSongsSorting, musicList?.toMutableList())
        } else {
            ListsHelper.getSortedMusicList(mSongsSorting, musicList?.toMutableList())
        }

        if (sLaunchedByArtistView) {
            _detailsFragmentBinding?.let { _binding ->
                _binding.sortButton.run {
                    setImageResource(getDefSortingIconForArtistView())
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
                _binding.detailsToolbar.menu.findItem(R.id.action_shuffle_sa).isEnabled =
                    mSelectedAlbum?.music?.size!! >= 2
            }
        }

        songs?.let { newSongsList ->
            mSongsList = newSongsList
            if (updateAdapter) {
                _detailsFragmentBinding?.songsRv?.adapter?.notifyDataSetChanged()
            }
            if (updateSongs) {
                mMediaControlInterface.onUpdatePlayingAlbumSongs(songs)
            }
        }
    }

    private fun getDefSortingIconForArtistView() = if (sShowDisplayName) {
        ThemeHelper.getSortIconForSongsDisplayName(
            mSongsSorting
        )
    } else {
        ThemeHelper.getSortIconForSongs(
            mSongsSorting
        )
    }

    private fun getDefSortingModeForArtistView() = if (sShowDisplayName) {
        GoConstants.ASCENDING_SORTING
    } else {
        GoConstants.TRACK_SORTING
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setSongsDataSource(
            ListsHelper.processQueryForMusic(newText, getSongSource())
                ?: mSongsList,
            updateSongs = false,
            updateAdapter = true
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
                        Pair(first = false, second = null)
                    )
                    R.id.action_shuffle_am -> {
                        mMediaControlInterface.onSongsShuffled(
                            mSongsList?.toMutableList(),
                            mLaunchedBy
                        )
                    }
                    R.id.action_shuffle_sa -> mMediaControlInterface.onSongsShuffled(
                        mSelectedAlbum?.music,
                        mLaunchedBy
                    )
                    R.id.default_sorting -> applySortingToMusic(GoConstants.DEFAULT_SORTING)
                    R.id.ascending_sorting -> applySortingToMusic(GoConstants.ASCENDING_SORTING)
                    R.id.descending_sorting -> applySortingToMusic(GoConstants.DESCENDING_SORTING)
                    R.id.track_sorting -> applySortingToMusic(GoConstants.TRACK_SORTING)
                    R.id.track_sorting_inv -> applySortingToMusic(GoConstants.TRACK_SORTING_INVERTED)
                    R.id.date_added_sorting -> applySortingToMusic(GoConstants.DATE_ADDED_SORTING)
                    R.id.date_added_sorting_inv -> applySortingToMusic(GoConstants.DATE_ADDED_SORTING_INV)
                    R.id.artist_sorting -> applySortingToMusic(GoConstants.ARTIST_SORTING)
                    R.id.artist_sorting_inv-> applySortingToMusic(GoConstants.ARTIST_SORTING_INV)

                    R.id.sleeptimer -> DialogHelper.showSleeptimerDialog(requireActivity(), requireContext())
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
        setSongsDataSource(selectedList, updateSongs = sCanUpdateSongs, updateAdapter = true)
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

        _detailsFragmentBinding?.detailsToolbar?.subtitle = getString(
            R.string.artist_info,
            mSelectedArtistAlbums?.size,
            mSongsList?.size
        )

        _detailsFragmentBinding?.albumsRv?.run {

            mAlbumsRecyclerViewLayoutManager = layoutManager as LinearLayoutManager
            adapter = AlbumsAdapter()

            if (mSelectedAlbumPosition != RecyclerView.NO_POSITION || mSelectedAlbumPosition != 0) {
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
        if (goPreferences.isCovers) {
            target.load(song?.albumId?.toAlbumArtURI()) {
                error(ContextCompat.getDrawable(requireActivity(), R.drawable.album_art))
            }
        } else {
            target.load(ContextCompat.getDrawable(requireActivity(), R.drawable.album_art))
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

    private fun swapAlbum(songs: MutableList<Music>?) {
        sAlbumSwapped = true
        setSongsDataSource(songs, updateSongs = false, updateAdapter = true)
        _detailsFragmentBinding?.songsRv?.afterMeasured {
            scrollToPosition(0)
            highlightSong(mSelectedSongId)
        }
    }

    private inner class AlbumsAdapter : RecyclerView.Adapter<AlbumsAdapter.AlbumsHolder>() {

        private val mMediaControlInterface = activity as MediaControlInterface

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = AlbumsHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.album_item,
                parent,
                false
            )
        )

        override fun getItemCount(): Int {
            return mSelectedArtistAlbums?.size!!
        }

        override fun onBindViewHolder(holder: AlbumsHolder, position: Int) {
            holder.bindItems(mSelectedArtistAlbums?.get(holder.absoluteAdapterPosition))
        }

        inner class AlbumsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bindItems(itemAlbum: Album?) {

                itemView.run {
                    val cardView = this as MaterialCardView

                    val albumCover = itemView.findViewById<ImageView>(R.id.image)
                    val album = itemView.findViewById<TextView>(R.id.album)
                    val year = itemView.findViewById<TextView>(R.id.year)
                    val totalDuration = itemView.findViewById<TextView>(R.id.total_duration)

                    album.text = itemAlbum?.title

                    year.text = itemAlbum?.year
                    totalDuration.text = itemAlbum?.totalDuration?.toFormattedDuration(
                        isAlbum = true,
                        isSeekBar = false
                    )

                    cardView.strokeWidth = if (mSelectedAlbum?.title == itemAlbum?.title) {
                        resources.getDimensionPixelSize(R.dimen.album_stroke)
                    } else {
                        0
                    }

                    loadCoverIntoTarget(itemAlbum?.music?.first(), albumCover)

                    setOnClickListener {
                        if (absoluteAdapterPosition != mSelectedAlbumPosition) {
                            notifyItemChanged(mSelectedAlbumPosition)
                            notifyItemChanged(absoluteAdapterPosition)
                            mSelectedAlbum = itemAlbum
                            mSelectedAlbumPosition = absoluteAdapterPosition
                            updateSelectedAlbumTitle()
                            swapAlbum(itemAlbum?.music)
                        } else {
                            if (sPlayFirstSong) {
                                mMediaControlInterface.onSongSelected(
                                    mSongsList?.get(0),
                                    itemAlbum?.music,
                                    mLaunchedBy
                                )
                            } else {
                                sPlayFirstSong = true
                            }
                        }
                    }
                }
            }
        }
    }

    private inner class SongsAdapter: RecyclerView.Adapter<SongsAdapter.SongsHolder>() {

        val defaultTextColor = ThemeHelper.resolveColorAttr(requireActivity(), android.R.attr.textColorPrimary)
        val accentTextColor = ThemeHelper.resolveThemeAccent(requireActivity())

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SongsHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.generic_item,
                parent,
                false
            )
        )

        override fun getItemCount(): Int {
            return mSongsList?.size!!
        }

        override fun onBindViewHolder(holder: SongsHolder, position: Int) {
            holder.bindItems(mSongsList?.get(holder.absoluteAdapterPosition))
        }

        inner class SongsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bindItems(itemSong: Music?) {

                itemView.run {

                    val title = itemView.findViewById<TextView>(R.id.title)
                    val subtitle = itemView.findViewById<TextView>(R.id.subtitle)

                    val displayedTitle =
                        if (sShowDisplayName || sLaunchedByFolderView) {
                            itemSong?.displayName?.toFilenameWithoutExtension()
                        } else {
                            getString(
                                R.string.track_song,
                                itemSong?.track?.toFormattedTrack(),
                                itemSong?.title
                            ).parseAsHtml()
                        }

                    title.text = displayedTitle

                    val titleColor = if (mSelectedSongId == itemSong?.id) {
                        mSelectedSongPosition = absoluteAdapterPosition
                        accentTextColor
                    } else {
                        defaultTextColor
                    }
                    title.setTextColor(titleColor)

                    val duration = itemSong?.duration?.toFormattedDuration(
                        isAlbum = false,
                        isSeekBar = false
                    )

                    subtitle.text = if (sLaunchedByFolderView) {
                        getString(R.string.duration_artist_date_added, duration,
                            itemSong?.artist, itemSong?.dateAdded?.toFormattedDate())
                    } else {
                        duration
                    }

                    setOnClickListener {
                        mMediaControlInterface.onSongSelected(
                            itemSong,
                            mSongsList,
                            mLaunchedBy
                        )

                        if (mSelectedSongId != itemSong?.id) {
                            notifyItemChanged(mSelectedSongPosition)
                            mSelectedSongId = itemSong?.id
                            notifyItemChanged(absoluteAdapterPosition)
                        }
                        if (!sCanUpdateSongs) {
                            sCanUpdateSongs = true
                        }
                        if (sAlbumSwapped) {
                            sAlbumSwapped = false
                        }
                    }

                    setOnLongClickListener {
                        DialogHelper.showPopupForSongs(
                            requireActivity(),
                            _detailsFragmentBinding?.songsRv?.findViewHolderForAdapterPosition(absoluteAdapterPosition)?.itemView,
                            itemSong,
                            mLaunchedBy
                        )
                        return@setOnLongClickListener true
                    }
                }
            }
        }
    }

    companion object {

        private const val TAG_ARTIST_FOLDER = "SELECTED_ARTIST_FOLDER"
        private const val TAG_IS_FOLDER = "IS_FOLDER"
        private const val TAG_SELECTED_ALBUM_POSITION = "SELECTED_ALBUM_POSITION"
        private const val TAG_HIGHLIGHTED_SONG_ID = "HIGHLIGHTED_SONG_ID"
        private const val TAG_CAN_UPDATE_SONGS = "CAN_UPDATE_SONGS"

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
            highlightedSongId: Long?,
            canUpdateSongs: Boolean
        ) =
            DetailsFragment().apply {
                arguments = bundleOf(
                    TAG_ARTIST_FOLDER to selectedArtistOrFolder,
                    TAG_IS_FOLDER to launchedBy,
                    TAG_SELECTED_ALBUM_POSITION to playedAlbumPosition,
                    TAG_HIGHLIGHTED_SONG_ID to highlightedSongId,
                    TAG_CAN_UPDATE_SONGS to canUpdateSongs
                )
            }
    }
}
