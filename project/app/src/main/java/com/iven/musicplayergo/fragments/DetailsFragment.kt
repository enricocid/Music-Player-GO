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
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicRepository
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentDetailsBinding
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

    private var sFolder = false

    private var mSelectedArtistAlbums: List<Album>? = null
    private var mSongsForArtistOrFolder: List<Music>? = null

    private var mSelectedArtistOrFolder: String? = null
    private var mSelectedAlbumPosition = -1

    private lateinit var mUIControlInterface: UIControlInterface
    private var mSelectedAlbum: Album? = null

    private var sLandscape = false

    private var mSongsSorting = GoConstants.TRACK_SORTING

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_ARTIST_FOLDER)?.let { selectedArtistOrFolder ->
            mSelectedArtistOrFolder = selectedArtistOrFolder
        }

        arguments?.getBoolean(TAG_IS_FOLDER)?.let { isFolder ->
            sFolder = isFolder
        }

        if (!sFolder) arguments?.getInt(TAG_SELECTED_ALBUM_POSITION)?.let { selectedAlbumPosition ->
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mDetailsFragmentBinding = FragmentDetailsBinding.bind(view)

        mMusicRepository = MusicRepository.getInstance()

        if (!sFolder) {

            mMusicRepository.deviceAlbumsByArtist?.get(mSelectedArtistOrFolder)
                ?.let { selectedArtistAlbums ->
                    mSelectedArtistAlbums = selectedArtistAlbums
                }

            mSongsForArtistOrFolder =
                mMusicRepository.deviceSongsByArtist?.get(mSelectedArtistOrFolder)

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
            mSongsForArtistOrFolder =
                mMusicRepository.deviceMusicByFolder?.get(mSelectedArtistOrFolder)
        }

        sLandscape = ThemeHelper.isDeviceLand(requireContext().resources)

        mDetailsFragmentBinding.detailsToolbar.apply {

            overflowIcon = AppCompatResources.getDrawable(
                requireContext(),
                if (sFolder) R.drawable.ic_more_vert else R.drawable.ic_shuffle
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

        if (!sFolder) {

            setupAlbumsContainer(requireContext())

        } else {

            mDetailsFragmentBinding.albumsRv.handleViewVisibility(false)
            selected_album_container.handleViewVisibility(false)

            mSongsForArtistOrFolder =
                mMusicRepository.deviceMusicByFolder?.get(mSelectedArtistOrFolder)

            mDetailsFragmentBinding.detailsToolbar.subtitle = getString(
                R.string.folder_info,
                mSongsForArtistOrFolder?.size
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

        setSongsDataSource(if (sFolder) mSongsForArtistOrFolder else mSelectedAlbum?.music)

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
                            if (sFolder) mSongsForArtistOrFolder
                            else
                                MusicOrgHelper.getAlbumSongs(
                                    item.artist,
                                    item.album,
                                    mMusicRepository.deviceAlbumsByArtist
                                )

                        mUIControlInterface.onSongSelected(
                            item,
                            selectedPlaylist,
                            sFolder
                        )
                    }

                    onLongClick { index ->
                        DialogHelper.showDoSomethingPopup(
                            requireContext(),
                            findViewHolderForAdapterPosition(index)?.itemView,
                            item,
                            sFolder,
                            mUIControlInterface
                        )
                    }
                }
            }
        }

        if (!sFolder) {
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
        if (!sFolder) {
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
            ListsHelper.processQueryForMusic(newText, mSongsForArtistOrFolder)
                ?: mSongsForArtistOrFolder
        )
        return false
    }

    override fun onQueryTextSubmit(query: String?) = false

    private fun setupMenu() {

        mDetailsFragmentBinding.detailsToolbar.apply {

            val menuToInflate =
                if (sFolder) R.menu.menu_folder_details else R.menu.menu_artist_details

            inflateMenu(menuToInflate)

            menu.apply {
                findItem(R.id.action_shuffle_am).isEnabled =
                    if (sFolder) mSongsForArtistOrFolder?.size!! >= 2 else !sFolder && mSelectedArtistAlbums?.size!! >= 2
                findItem(R.id.action_shuffle_sa).isEnabled = !sFolder
                if (sFolder) findItem(R.id.sorting).isEnabled = mSongsForArtistOrFolder?.size!! >= 2
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
        mSongsForArtistOrFolder = ListsHelper.getSortedMusicList(
            order,
            selectedList?.toMutableList()
        )
        setSongsDataSource(mSongsForArtistOrFolder)
    }

    private fun setupToolbarSpecs() {
        mDetailsFragmentBinding.detailsToolbar.apply {
            elevation = if (sFolder)
                resources.getDimensionPixelSize(R.dimen.search_bar_elevation).toFloat() else 0F

            val params = layoutParams as LinearLayout.LayoutParams
            params.bottomMargin = if (sFolder)
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

        updateSelectedAlbumContainer()

        setAlbumsDataSource(mSelectedArtistAlbums)

        mDetailsFragmentBinding.detailsToolbar.subtitle = getString(
            R.string.artist_info,
            mSelectedArtistAlbums?.size,
            mSongsForArtistOrFolder?.size
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
                                updateSelectedAlbumContainer()
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
        if (!sFolder && snapPosition != -1) mDetailsFragmentBinding.albumsRv.smoothSnapToPosition(
            snapPosition
        )
    }

    private fun updateSelectedAlbumContainer() {
        updateSelectedAlbumTitle()
        updateSelectedAlbumContainerClickListener()
    }

    private fun updateSelectedAlbumTitle() {
        mDetailsFragmentBinding.selectedAlbum.text = mSelectedAlbum?.title
        album_year_duration.text = getString(
            R.string.year_and_duration,
            mSelectedAlbum?.totalDuration?.toFormattedDuration(isAlbum = true, isSeekBar = false),
            mSelectedAlbum?.year
        )
    }

    private fun updateSelectedAlbumContainerClickListener() {
        mDetailsFragmentBinding.selectedAlbumContainer.setOnClickListener {
            playFirstSongOfAlbum()
        }
    }

    private fun playFirstSongOfAlbum() {
        val firstSongOfSelectedAlbum = mSelectedAlbum?.music?.first()
        firstSongOfSelectedAlbum?.let {
            val loadedSong =
                if (sFolder) mSongsForArtistOrFolder
                else
                    MusicOrgHelper.getAlbumSongs(
                        firstSongOfSelectedAlbum.artist,
                        firstSongOfSelectedAlbum.album,
                        mMusicRepository.deviceAlbumsByArtist
                    )

            mUIControlInterface.onSongSelected(
                firstSongOfSelectedAlbum,
                loadedSong,
                sFolder
            )
        }
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
