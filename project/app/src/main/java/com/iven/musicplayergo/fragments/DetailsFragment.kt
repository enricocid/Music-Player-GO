package com.iven.musicplayergo.fragments

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.AlbumItemBinding
import com.iven.musicplayergo.databinding.FragmentDetailsBinding
import com.iven.musicplayergo.databinding.GenericItemBinding
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.models.Album
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.ItemSwipeCallback
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface
import com.iven.musicplayergo.utils.Lists
import com.iven.musicplayergo.utils.Popups
import com.iven.musicplayergo.utils.Theming
import me.zhanghai.android.fastscroll.FastScrollerBuilder


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

    private val sShowDisplayName get() = GoPreferences.getPrefsInstance().songsVisualization == GoConstants.FN

    private var sPlayFirstSong = true
    private var sCanUpdateSongs = true
    private var sAlbumSwapped = false
    private var sOpenNewDetailsFragment = false

    private val mMediaPlayerHolder get() = MediaPlayerHolder.getInstance()

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
            if (sLaunchedByArtistView) mSelectedAlbumPosition = getInt(TAG_SELECTED_ALBUM_POSITION)
            mSelectedSongId = getLong(TAG_SELECTED_SONG_ID)
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
                mArtistDetailsAnimator = createCircularReveal(show = false)
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
        _detailsFragmentBinding = if (sLaunchedByAlbumView && Theming.isDeviceLand(resources)) {
            val view = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_details_alt_land, container, false)
            FragmentDetailsBinding.bind(view)
        } else {
            FragmentDetailsBinding.inflate(inflater, container, false)
        }
        return _detailsFragmentBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMusicViewModel =
            ViewModelProvider(requireActivity())[MusicViewModel::class.java].apply {
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
                requireContext(),
                if (sLaunchedByArtistView) R.drawable.ic_shuffle else R.drawable.ic_more_vert
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
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

            setupMenu()
        }
    }

    private fun setupToolbarSpecs() {
        _detailsFragmentBinding?.detailsToolbar?.run {
            if (sLaunchedByFolderView) {
                elevation = resources.getDimensionPixelSize(R.dimen.search_bar_elevation).toFloat()
                setBackgroundColor(Theming.resolveColorAttr(requireContext(), R.attr.toolbar_bg))
            }

            val params = layoutParams as LinearLayout.LayoutParams
            if (sLaunchedByArtistView) params.bottomMargin = resources.getDimensionPixelSize(R.dimen.player_controls_padding_normal)
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
                        Lists.getSongsDisplayNameSorting(mSongsSorting)
                    } else {
                        Lists.getSongsSorting(mSongsSorting)
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
                } else if (sLaunchedByAlbumView) {
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

                    albumViewArt.background.alpha = Theming.getAlbumCoverAlpha(requireContext())
                    albumViewArt.afterMeasured {
                        val dim = width * 2
                        albumViewArt.layoutParams = LinearLayout.LayoutParams(dim, dim)
                    }
                    firstSong?.albumId?.waitForCover(requireContext()) { bmp, error ->
                        albumViewArt.loadWithError(bmp, error, R.drawable.ic_music_note_cover)
                    }
                }

                val searchView =
                    detailsToolbar.menu.findItem(R.id.action_search).actionView as SearchView

                with(searchView) {
                    setOnQueryTextListener(this@DetailsFragment)
                    setOnQueryTextFocusChangeListener { _, hasFocus ->
                        detailsToolbar.menu.setGroupVisible(
                            R.id.more_options_folder,
                            !hasFocus
                        )
                        detailsToolbar.menu.findItem(R.id.sleeptimer).isVisible = !hasFocus
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
            if (sLaunchedByArtistView) mSelectedAlbum?.music else mSongsList,
            updateSongs = false,
            updateAdapter = false
        )

        _detailsFragmentBinding?.songsRv?.run {

            setHasFixedSize(true)
            adapter = SongsAdapter()
            FastScrollerBuilder(this).useMd2Style().build()

            ItemTouchHelper(ItemSwipeCallback(isQueueDialog = false, isFavoritesDialog = false) { viewHolder: RecyclerView.ViewHolder,
                                                                          direction: Int ->
                val song = mSongsList?.get(viewHolder.absoluteAdapterPosition)
                if (direction == ItemTouchHelper.RIGHT) {
                    mMediaControlInterface.onAddToQueue(song)
                } else {
                    Lists.addToFavorites(requireContext(), song,
                        canRemove = false, 0, mLaunchedBy
                    )
                    mUIControlInterface.onFavoriteAddedOrRemoved()
                    mMediaPlayerHolder.onUpdateFavorites()
                }
                adapter?.notifyDataSetChanged()
            }).attachToRecyclerView(this)
        }

        view.afterMeasured {
            if (GoPreferences.getPrefsInstance().isAnimations) {
                _detailsFragmentBinding?.root?.run {
                    mArtistDetailsAnimator = createCircularReveal(show = true)
                }
            }
            scrollToPlayingSong(mSelectedSongId)
        }
    }

    fun scrollToPlayingSong(songId: Long?) {
        try {
            mSongsList?.indexOfFirst { song -> song.id == songId }?.let { pos ->
                (_detailsFragmentBinding?.songsRv?.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    when {
                        pos > -1 -> pos
                        else -> 0
                    },
                    0
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setSongsDataSource(musicList: List<Music>?, updateSongs: Boolean, updateAdapter: Boolean) {

        val songs = if (sLaunchedByFolderView) {
            Lists.getSortedMusicListForFolder(mSongsSorting, musicList?.toMutableList())
        } else {
            Lists.getSortedMusicList(mSongsSorting, musicList?.toMutableList())
        }

        if (sLaunchedByArtistView) {
            _detailsFragmentBinding?.let { _binding ->
                with(_binding.sortButton) {
                    setImageResource(getDefSortingIconForArtistView())
                    isEnabled = mSelectedAlbum?.music?.size!! >= 2
                    updateIconTint(
                        if (isEnabled) {
                            ContextCompat.getColor(requireContext(), R.color.widgets_color)
                        } else {
                            Theming.resolveWidgetsColorNormal(requireContext())
                        }
                    )
                }
                _binding.detailsToolbar.menu.findItem(R.id.action_shuffle_sa).isEnabled =
                    mSelectedAlbum?.music?.size!! >= 2
            }
        }

        songs?.let { newSongsList ->
            mSongsList = newSongsList
            if (updateAdapter) _detailsFragmentBinding?.songsRv?.adapter?.notifyDataSetChanged()
            if (updateSongs) mMediaControlInterface.onUpdatePlayingAlbumSongs(mSongsList)
        }
    }

    private fun getDefSortingIconForArtistView() = if (sShowDisplayName) {
        Theming.getSortIconForSongsDisplayName(
            mSongsSorting
        )
    } else {
        Theming.getSortIconForSongs(
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
            Lists.processQueryForMusic(newText, getSongSource()) ?: mSongsList,
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
                    R.id.action_shuffle_am -> mMediaControlInterface.onSongsShuffled(
                        mSongsList?.toMutableList(),
                        mLaunchedBy
                    )
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
                    R.id.sleeptimer -> mUIControlInterface.onOpenSleepTimerDialog()
                }
                return@setOnMenuItemClickListener true
            }
        }

        tintSleepTimerIcon(enabled = mMediaPlayerHolder.isSleepTimer)
    }

    fun tintSleepTimerIcon(enabled: Boolean) {
        _detailsFragmentBinding?.detailsToolbar?.run {
            Theming.tintSleepTimerMenuItem(this, enabled)
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
            setHasFixedSize(true)
            adapter = AlbumsAdapter()

            if (mSelectedAlbumPosition != RecyclerView.NO_POSITION) {
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

    fun hasToUpdate(selectedArtistOrFolder: String?): Boolean {
        sOpenNewDetailsFragment = mSelectedArtistOrFolder != null && selectedArtistOrFolder != mSelectedArtistOrFolder
        return sOpenNewDetailsFragment
    }

    fun tryToSnapToAlbumPosition(snapPosition: Int) {
        sPlayFirstSong = false
        if (sLaunchedByArtistView && snapPosition != -1) {
            _detailsFragmentBinding?.albumsRv?.smoothSnapToPosition(snapPosition)
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
            scrollToPlayingSong(mSelectedSongId)
        }
    }

    private inner class AlbumsAdapter : RecyclerView.Adapter<AlbumsAdapter.AlbumsHolder>() {

        private val mMediaControlInterface = activity as MediaControlInterface

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumsHolder {
            val binding = AlbumItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AlbumsHolder(binding)
        }

        override fun getItemCount(): Int {
            return mSelectedArtistAlbums?.size!!
        }

        override fun onBindViewHolder(holder: AlbumsHolder, position: Int) {
            holder.bindItems(mSelectedArtistAlbums?.get(holder.absoluteAdapterPosition))
        }

        inner class AlbumsHolder(private val binding: AlbumItemBinding): RecyclerView.ViewHolder(binding.root) {

            fun bindItems(itemAlbum: Album?) {

                with(binding) {

                    album.text = itemAlbum?.title

                    year.text = itemAlbum?.year
                    totalDuration.text = itemAlbum?.totalDuration?.toFormattedDuration(
                        isAlbum = true,
                        isSeekBar = false
                    )

                    root.strokeWidth = if (mSelectedAlbum?.title == itemAlbum?.title) {
                        resources.getDimensionPixelSize(R.dimen.album_stroke)
                    } else {
                        0
                    }

                    image.background.alpha = Theming.getAlbumCoverAlpha(requireContext())

                    itemAlbum?.music?.first()?.albumId?.waitForCover(requireContext()) { bmp, error ->
                        image.loadWithError(bmp, error, R.drawable.ic_music_note_cover_alt)
                    }

                    root.setOnClickListener {
                        if (absoluteAdapterPosition != mSelectedAlbumPosition) {
                            notifyItemChanged(mSelectedAlbumPosition)
                            notifyItemChanged(absoluteAdapterPosition)
                            mSelectedAlbum = itemAlbum
                            mSelectedAlbumPosition = absoluteAdapterPosition
                            updateSelectedAlbumTitle()
                            swapAlbum(itemAlbum?.music)
                        } else {
                            if (sPlayFirstSong) {
                                with(mMediaPlayerHolder) {
                                    if (isCurrentSongFM) currentSongFM = null
                                }
                                mMediaControlInterface.onSongSelected(
                                    mSongsList?.first(),
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

        val defaultTextColor = Theming.resolveColorAttr(requireContext(), android.R.attr.textColorPrimary)
        val accentTextColor = Theming.resolveThemeColor(resources)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsHolder {
            val binding = GenericItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return SongsHolder(binding)
        }

        override fun getItemCount(): Int {
            return mSongsList?.size!!
        }

        override fun onBindViewHolder(holder: SongsHolder, position: Int) {
            holder.bindItems(mSongsList?.get(holder.absoluteAdapterPosition))
        }

        inner class SongsHolder(private val binding: GenericItemBinding): RecyclerView.ViewHolder(binding.root) {

            fun bindItems(itemSong: Music?) {

                with(binding) {

                    albumCover.handleViewVisibility(show = false)

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

                    root.setOnClickListener {

                        with(mMediaPlayerHolder) {
                            if (isCurrentSongFM) currentSongFM = null
                        }
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
                        if (!sCanUpdateSongs) sCanUpdateSongs = true
                        if (sAlbumSwapped) sAlbumSwapped = false
                    }

                    root.setOnLongClickListener {
                        Popups.showPopupForSongs(
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
        private const val TAG_SELECTED_SONG_ID = "HIGHLIGHTED_SONG_ID"
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
            selectedSongId: Long?,
            canUpdateSongs: Boolean
        ) =
            DetailsFragment().apply {
                arguments = bundleOf(
                    TAG_ARTIST_FOLDER to selectedArtistOrFolder,
                    TAG_IS_FOLDER to launchedBy,
                    TAG_SELECTED_ALBUM_POSITION to playedAlbumPosition,
                    TAG_SELECTED_SONG_ID to selectedSongId,
                    TAG_CAN_UPDATE_SONGS to canUpdateSongs
                )
            }
    }
}
