package com.iven.musicplayergo.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentMusicContainersBinding
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.ui.MainActivity
import com.iven.musicplayergo.ui.UIControlInterface
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.reddit.indicatorfastscroll.FastScrollerView

/**
 * A simple [Fragment] subclass.
 * Use the [MusicContainersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MusicContainersFragment : Fragment(),
    SearchView.OnQueryTextListener {

    private var _musicContainerListBinding: FragmentMusicContainersBinding? = null

    // View model
    private lateinit var mMusicViewModel: MusicViewModel

    private var mLaunchedBy = GoConstants.ARTIST_VIEW

    private var mList: MutableList<String>? = null

    private lateinit var mListAdapter: MusicContainersAdapter

    private lateinit var mUiControlInterface: UIControlInterface

    private lateinit var mSortMenuItem: MenuItem
    private var mSorting = GoConstants.DESCENDING_SORTING

    private var sIsFastScroller = false
    private val sLaunchedByAlbumView get() = mLaunchedBy == GoConstants.ALBUM_VIEW

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_LAUNCHED_BY)?.let { launchedBy ->
            mLaunchedBy = launchedBy
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUiControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _musicContainerListBinding = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _musicContainerListBinding = FragmentMusicContainersBinding.inflate(inflater, container, false)
        return _musicContainerListBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMusicViewModel =
            ViewModelProvider(requireActivity()).get(MusicViewModel::class.java).apply {
                deviceMusic.observe(viewLifecycleOwner) { returnedMusic ->
                    if (!returnedMusic.isNullOrEmpty()) {
                        mSorting = getSortingMethodFromPrefs()
                        mList = getSortedList()
                        finishSetup()
                    }
                }
            }
    }

    private fun finishSetup() {

        _musicContainerListBinding?.artistsFoldersRv?.run {
            itemAnimator = null
            mListAdapter = MusicContainersAdapter()
            adapter = mListAdapter
        }

        setupIndicatorFastScrollerView()

        _musicContainerListBinding?.searchToolbar?.let { stb ->

            stb.inflateMenu(R.menu.menu_search)

            stb.title = getFragmentTitle()

            stb.overflowIcon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_sort)

            stb.setNavigationOnClickListener {
                mUiControlInterface.onCloseActivity()
            }

            with (stb.menu) {

                mSortMenuItem = ListsHelper.getSelectedSorting(mSorting, this).apply {
                    setTitleColor(ThemeHelper.resolveThemeAccent(requireActivity()))
                }

                with(findItem(R.id.action_search).actionView as SearchView) {
                    setOnQueryTextListener(this@MusicContainersFragment)
                    setOnQueryTextFocusChangeListener { _, hasFocus ->
                        _musicContainerListBinding?.fastscroller?.handleViewVisibility(show = !hasFocus)
                        _musicContainerListBinding?.fastscrollerThumb?.handleViewVisibility(
                            show = !hasFocus
                        )
                        _musicContainerListBinding?.artistsFoldersRv?.setupFastScrollerPadding(forceNoPadding = hasFocus,
                            resources)
                        stb.menu.setGroupVisible(R.id.sorting, !hasFocus)
                        stb.menu.findItem(R.id.sleeptimer).isVisible = !hasFocus
                    }
                }
                setMenuOnItemClickListener(this)
            }
        }
    }

    private fun getSortedList(): MutableList<String>? {
        return when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                ListsHelper.getSortedList(
                    mSorting,
                    mMusicViewModel.deviceAlbumsByArtist?.keys?.toMutableList()
                )
            GoConstants.FOLDER_VIEW ->
                ListsHelper.getSortedList(
                    mSorting,
                    mMusicViewModel.deviceMusicByFolder?.keys?.toMutableList()
                )

            else ->
                ListsHelper.getSortedListWithNull(
                    mSorting,
                    mMusicViewModel.deviceMusicByAlbum?.keys?.toMutableList()
                )
        }
    }

    private fun getSortingMethodFromPrefs(): Int {
        return when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                goPreferences.artistsSorting
            GoConstants.FOLDER_VIEW ->
                goPreferences.foldersSorting
            else ->
                goPreferences.albumsSorting
        }
    }

    private fun getFragmentTitle(): String {
        val stringId = when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                R.string.artists
            GoConstants.FOLDER_VIEW ->
                R.string.folders
            else ->
                R.string.albums
        }
        return getString(stringId)
    }

    private fun setListDataSource(selectedList: List<String>?) {
        if (!selectedList.isNullOrEmpty()) {
            mListAdapter.swapList(selectedList)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun onUpdateCoverOption() {
        _musicContainerListBinding?.artistsFoldersRv?.adapter?.notifyDataSetChanged()
    }

    fun onListFiltered(stringToFilter: String) = if (mList == null) {
        false
    } else {
        mList?.run {
            val index = indexOf(stringToFilter)
            remove(stringToFilter)
            _musicContainerListBinding?.artistsFoldersRv?.adapter?.notifyItemRemoved(index)
        }
        true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setListDataSource(ListsHelper.processQueryForStringsLists(newText, getSortedList()) ?: mList)
        return false
    }

    override fun onQueryTextSubmit(query: String?) = false

    private fun setupIndicatorFastScrollerView() {

        _musicContainerListBinding?.let { _binding ->

            // Set indexes if artists rv is scrollable
            _binding.artistsFoldersRv.afterMeasured {

                sIsFastScroller = computeVerticalScrollRange() > height

                if (sIsFastScroller) {

                    _binding.fastscroller.setupWithRecyclerView(
                        this,
                        { position ->
                            // Return a text tab_indicator
                            mList?.get(position)?.run {
                                getFastScrollerItem(requireActivity())
                            }
                        }, showIndicator = { _, indicatorPosition, totalIndicators ->
                            // Hide every other indicator
                            if (ThemeHelper.isDeviceLand(resources)) {
                                indicatorPosition % 2 == 0
                            } else {
                                if (totalIndicators >= 30) {
                                    indicatorPosition % 2 == 0
                                } else {
                                    true
                                }
                            }
                        })

                    _binding.fastscrollerThumb.setupWithFastScroller(_binding.fastscroller)
                    _binding.fastscroller.useDefaultScroller = false
                    _binding.fastscroller.itemIndicatorSelectedCallbacks += object :
                        FastScrollerView.ItemIndicatorSelectedCallback {
                        override fun onItemIndicatorSelected(
                            indicator: FastScrollItemIndicator,
                            indicatorCenterY: Int,
                            itemPosition: Int
                        ) {
                            val artistsLayoutManager = layoutManager as LinearLayoutManager
                            artistsLayoutManager.scrollToPositionWithOffset(itemPosition, 0)
                        }
                    }
                }
                _binding.artistsFoldersRv.setupFastScrollerPadding(forceNoPadding = false, resources)
            }
        }
    }

    private fun setMenuOnItemClickListener(menu: Menu) {
        _musicContainerListBinding?.searchToolbar?.setOnMenuItemClickListener {

            if (it.itemId == R.id.sleeptimer) {

                DialogHelper.showSleeptimerDialog(requireActivity(), requireContext())

            }
            else {

                mSorting = it.order

                mList = getSortedList()
                setListDataSource(mList)

                mSortMenuItem.setTitleColor(
                    ThemeHelper.resolveColorAttr(
                        requireActivity(),
                        android.R.attr.textColorPrimary
                    )
                )

                mSortMenuItem = ListsHelper.getSelectedSorting(mSorting, menu).apply {
                    setTitleColor(ThemeHelper.resolveThemeAccent(requireActivity()))
                }

                saveSortingMethodToPrefs(mSorting)
            }

            return@setOnMenuItemClickListener true
        }
    }

    private fun saveSortingMethodToPrefs(sortingMethod: Int) {
        when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                goPreferences.artistsSorting = sortingMethod
            GoConstants.FOLDER_VIEW ->
                goPreferences.foldersSorting = sortingMethod
            else ->
                goPreferences.albumsSorting = sortingMethod
        }
    }

    companion object {

        private const val TAG_LAUNCHED_BY = "SELECTED_FRAGMENT"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MusicContainersListFragment.
         */
        @JvmStatic
        fun newInstance(launchedBy: String) = MusicContainersFragment().apply {
            arguments = bundleOf(TAG_LAUNCHED_BY to launchedBy)
        }
    }

    private inner class MusicContainersAdapter : RecyclerView.Adapter<MusicContainersAdapter.ArtistHolder>() {

        @SuppressLint("NotifyDataSetChanged")
        fun swapList(newItems: List<String>?) {
            mList = newItems?.toMutableList()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ArtistHolder(
            LayoutInflater.from(parent.context).inflate(
                if (sLaunchedByAlbumView) {
                    R.layout.containers_album_item
                } else {
                    R.layout.generic_item
                },
                parent,
                false
            )
        )

        override fun getItemCount(): Int {
            return mList?.size!!
        }

        override fun onBindViewHolder(holder: ArtistHolder, position: Int) {
            holder.bindItems(mList?.get(holder.absoluteAdapterPosition)!!)
        }

        inner class ArtistHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bindItems(item: String) {

                itemView.run {

                    val title = itemView.findViewById<TextView>(R.id.title)
                    val subtitle = itemView.findViewById<TextView>(R.id.subtitle)

                    if (sLaunchedByAlbumView) {
                        val albumCover = itemView.findViewById<ImageView>(R.id.album_cover)
                        if (goPreferences.isCovers) {
                            albumCover.load(mMusicViewModel.deviceMusicByAlbum?.get(item)?.first()?.albumId?.toAlbumArtURI()) {
                                error(ContextCompat.getDrawable(requireActivity(), R.drawable.album_art))
                            }
                        } else {
                            albumCover.load(ContextCompat.getDrawable(requireActivity(), R.drawable.album_art))
                        }
                    }

                    title.text = item
                    subtitle.text = getItemsSubtitle(item)

                    setOnClickListener {
                        respondToTouch(isLongClick = false, item, null)
                    }
                    setOnLongClickListener {
                        val vh = _musicContainerListBinding?.artistsFoldersRv?.findViewHolderForAdapterPosition(absoluteAdapterPosition)?.itemView
                        respondToTouch(isLongClick = true, item, vh)
                        return@setOnLongClickListener true
                    }
                }
            }
        }

        private fun getItemsSubtitle(item: String): String? {
            return when (mLaunchedBy) {
                GoConstants.ARTIST_VIEW ->
                    getArtistSubtitle(item)
                GoConstants.FOLDER_VIEW ->
                    getString(
                        R.string.folder_info,
                        mMusicViewModel.deviceMusicByFolder?.getValue(item)?.size
                    )
                else -> mMusicViewModel.deviceMusicByAlbum?.get(item)?.first()?.artist
            }
        }

        private fun getArtistSubtitle(item: String) = getString(
            R.string.artist_info,
            mMusicViewModel.deviceAlbumsByArtist?.getValue(item)?.size,
            mMusicViewModel.deviceSongsByArtist?.getValue(item)?.size
        )

        private fun respondToTouch(isLongClick: Boolean, item: String, itemView: View?) {
            if (isLongClick) {
                DialogHelper.showPopupForHide(
                    requireActivity(),
                    itemView,
                    item
                )
            } else {
                mUiControlInterface.onArtistOrFolderSelected(
                    item,
                    mLaunchedBy
                )
            }
        }
    }
}
