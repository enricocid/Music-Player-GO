package com.iven.musicplayergo.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentMusicContainersBinding
import com.iven.musicplayergo.extensions.loadWithError
import com.iven.musicplayergo.extensions.setTitleColor
import com.iven.musicplayergo.extensions.waitForCover
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface
import com.iven.musicplayergo.utils.Lists
import com.iven.musicplayergo.utils.Popups
import com.iven.musicplayergo.utils.Theming
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider


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

    private val sLaunchedByAlbumView get() = mLaunchedBy == GoConstants.ALBUM_VIEW

    private val sIsFastScrollerPopup get() = mSorting == GoConstants.ASCENDING_SORTING || mSorting == GoConstants.DESCENDING_SORTING

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

        mMusicViewModel = ViewModelProvider(requireActivity()).get(MusicViewModel::class.java)

        mSorting = getSortingMethodFromPrefs()
        mList = getSortedList()
        finishSetup()
    }

    private fun finishSetup() {

        _musicContainerListBinding?.artistsFoldersRv?.run {
            setHasFixedSize(true)
            itemAnimator = null
            mListAdapter = MusicContainersAdapter()
            adapter = mListAdapter
            FastScrollerBuilder(this).useMd2Style().build()
            if (sLaunchedByAlbumView) {
                recycledViewPool.setMaxRecycledViews(0, 0)
            }
        }

        _musicContainerListBinding?.searchToolbar?.let { stb ->

            stb.inflateMenu(R.menu.menu_search)
            stb.title = getFragmentTitle()
            stb.overflowIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_sort)

            stb.setNavigationOnClickListener {
                mUiControlInterface.onCloseActivity()
            }

            with (stb.menu) {

                mSortMenuItem = Lists.getSelectedSorting(mSorting, this).apply {
                    setTitleColor(Theming.resolveThemeAccent(requireContext()))
                }

                with(findItem(R.id.action_search).actionView as SearchView) {
                    setOnQueryTextListener(this@MusicContainersFragment)
                    setOnQueryTextFocusChangeListener { _, hasFocus ->
                        stb.menu.setGroupVisible(R.id.sorting, !hasFocus)
                        stb.menu.findItem(R.id.sleeptimer).isVisible = !hasFocus
                    }
                }
                setMenuOnItemClickListener(this)
            }
        }

        (requireActivity() as MediaControlInterface).onGetMediaPlayerHolder()?.let { mp ->
            tintSleepTimerIcon(enabled = mp.isSleepTimer)
        }
    }

    fun tintSleepTimerIcon(enabled: Boolean) {
        _musicContainerListBinding?.searchToolbar?.run {
            Theming.tintSleepTimerMenuItem(this, enabled)
        }
    }

    private fun getSortedList(): MutableList<String>? {
        return when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                Lists.getSortedList(
                    mSorting,
                    mMusicViewModel.deviceAlbumsByArtist?.keys?.toMutableList()
                )
            GoConstants.FOLDER_VIEW ->
                Lists.getSortedList(
                    mSorting,
                    mMusicViewModel.deviceMusicByFolder?.keys?.toMutableList()
                )

            else ->
                Lists.getSortedListWithNull(
                    mSorting,
                    mMusicViewModel.deviceMusicByAlbum?.keys?.toMutableList()
                )
        }
    }

    private fun getSortingMethodFromPrefs(): Int {
        return when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                GoPreferences.getPrefsInstance().artistsSorting
            GoConstants.FOLDER_VIEW ->
                GoPreferences.getPrefsInstance().foldersSorting
            else ->
                GoPreferences.getPrefsInstance().albumsSorting
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

    override fun onQueryTextChange(newText: String?): Boolean {
        setListDataSource(Lists.processQueryForStringsLists(newText, getSortedList()) ?: mList)
        return false
    }

    override fun onQueryTextSubmit(query: String?) = false

    private fun setMenuOnItemClickListener(menu: Menu) {
        _musicContainerListBinding?.searchToolbar?.setOnMenuItemClickListener {

            when (it.itemId) {
                R.id.sleeptimer -> mUiControlInterface.onOpenSleepTimerDialog()
                else -> if (it.itemId != R.id.action_search) {
                    mSorting = it.order

                    mList = getSortedList()
                    setListDataSource(mList)

                    mSortMenuItem.setTitleColor(
                        Theming.resolveColorAttr(
                            requireContext(),
                            android.R.attr.textColorPrimary
                        )
                    )

                    mSortMenuItem = Lists.getSelectedSorting(mSorting, menu).apply {
                        setTitleColor(Theming.resolveThemeAccent(requireContext()))
                    }

                    saveSortingMethodToPrefs(mSorting)
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun saveSortingMethodToPrefs(sortingMethod: Int) {
        when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                GoPreferences.getPrefsInstance().artistsSorting = sortingMethod
            GoConstants.FOLDER_VIEW ->
                GoPreferences.getPrefsInstance().foldersSorting = sortingMethod
            else ->
                GoPreferences.getPrefsInstance().albumsSorting = sortingMethod
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

    private inner class MusicContainersAdapter : RecyclerView.Adapter<MusicContainersAdapter.ArtistHolder>(), PopupTextProvider {

        @SuppressLint("NotifyDataSetChanged")
        fun swapList(newItems: List<String>?) {
            mList = newItems?.toMutableList()
            notifyDataSetChanged()
        }

        override fun getPopupText(position: Int): String {
            if (sIsFastScrollerPopup) {
                mList?.get(position)?.run {
                    if (length > 0) {
                        return first().toString()
                    }
                }
            }
            return ""
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

                with(itemView) {

                    val title = findViewById<TextView>(R.id.title)
                    val subtitle = findViewById<TextView>(R.id.subtitle)

                    if (sLaunchedByAlbumView) {
                        val albumCover = findViewById<ImageView>(R.id.album_cover).apply {
                            background.alpha = Theming.getAlbumCoverAlpha(requireContext())
                        }
                        mMusicViewModel.deviceMusicByAlbum?.get(item)?.first()?.albumId?.waitForCover(requireContext()) { bmp, error ->
                            albumCover.loadWithError(bmp, error, R.drawable.ic_music_note_cover_alt)
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
                if (mList?.size!! >= 2) {
                    Popups.showPopupForHide(
                        requireActivity(),
                        itemView,
                        item
                    )
                }
            } else {
                mUiControlInterface.onArtistOrFolderSelected(
                    item,
                    mLaunchedBy
                )
            }
        }
    }
}
