package com.iven.musicplayergo.fragments

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.R
import com.iven.musicplayergo.music.Album
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.musicLibrary
import com.iven.musicplayergo.ui.*
import kotlinx.android.synthetic.main.fragment_details.*
import kotlin.math.max


/**
 * A simple [Fragment] subclass.
 * Use the [DetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

private const val REVEAL_DURATION: Long = 500

class DetailsFragment : Fragment(), SearchView.OnQueryTextListener {

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

    private var sDeviceLand = false

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    fun onHandleBackPressed(context: Context): Animator {
        if (!mArtistDetailsAnimator.isRunning) revealFragment(context, false)
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

        context?.let {

            sDeviceLand = ThemeHelper.isDeviceLand(it.resources)

            mDetailsToolbar.apply {

                overflowIcon = AppCompatResources.getDrawable(it, R.drawable.ic_shuffle)

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

                setupMenu(false)
            }

            if (!sFolder) {

                setupAlbumsContainer(it, false)

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

                    if (sDeviceLand)
                        withLayoutManager(GridLayoutManager(it, 2))
                    else
                        addItemDecoration(ThemeHelper.getRecyclerViewDivider(it))

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
                                selectedPlaylist
                            )
                        }

                        onLongClick { index ->
                            Utils.showAddToLovedQueueSongsPopup(
                                it,
                                findViewHolderForAdapterPosition(index)?.itemView!!,
                                item,
                                mUIControlInterface
                            )
                        }
                    }
                }
            }

            view.afterMeasured {
                revealFragment(it, true)
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

    private fun setupMenu(onUpdateView: Boolean) {

        mDetailsToolbar.apply {

            if (onUpdateView) mDetailsToolbar.menu.clear()

            val menuToInflate =
                if (sFolder && !onUpdateView) R.menu.menu_folder_details else R.menu.menu_artist_details

            inflateMenu(menuToInflate)

            menu.apply {
                findItem(R.id.action_shuffle_sa).isEnabled = !sFolder
                if (!sFolder) findItem(R.id.action_shuffle_am).isEnabled =
                    mSelectedArtistAlbums?.size!! >= 2
            }

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_shuffle_am -> mUIControlInterface.onShuffleSongs(
                        mSongsForArtistOrFolder?.toMutableList()
                    )
                    R.id.action_shuffle_sa -> mUIControlInterface.onShuffleSongs(mSelectedAlbum?.music)
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

    private fun setupAlbumsContainer(context: Context, onUpdateView: Boolean) {

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
                    if (sDeviceLand) LinearLayoutManager.VERTICAL else LinearLayoutManager.HORIZONTAL,
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
            if (mSelectedAlbumPosition != -1 or 0 && !onUpdateView) mAlbumsRecyclerViewLayoutManager.scrollToPositionWithOffset(
                mSelectedAlbumPosition,
                0
            )
        }
    }

    fun updateView(context: Context, selectedArtist: String?, playedAlbumPosition: Int) {

        fun invalidateDetails() {

            mSelectedArtistOrFolder = selectedArtist
            mSelectedArtistAlbums =
                musicLibrary.allAlbumsByArtist?.get(mSelectedArtistOrFolder)

            //restore album position
            if (playedAlbumPosition != -1) {
                mSelectedAlbumPosition = playedAlbumPosition
                mSelectedAlbum = mSelectedArtistAlbums?.get(mSelectedAlbumPosition)
            }

            if (sFolder) {
                sFolder = false
                mDetailsToolbar.title = mSelectedArtistOrFolder
                mAlbumsRecyclerView.visibility = View.VISIBLE
                mSelectedAlbumTitle.visibility = View.VISIBLE
                mSelectedAlbumYearDuration.visibility = View.VISIBLE

                setupToolbarSpecs(sFolder)
                setupMenu(true)
                setupAlbumsContainer(context, true)

            } else {

                mDetailsToolbar.apply {
                    title = mSelectedArtistOrFolder
                    subtitle = getString(
                        R.string.artist_info,
                        mSelectedArtistAlbums?.size,
                        mSongsForArtistOrFolder?.size
                    )
                }

                updateSelectedAlbumTitle()
            }

            setAlbumsDataSource(mSelectedArtistAlbums)

            mAlbumsRecyclerViewLayoutManager.scrollToPositionWithOffset(
                mSelectedAlbumPosition,
                0
            )

            mSongsForArtistOrFolder =
                musicLibrary.allSongsByArtist?.getValue(mSelectedArtistOrFolder)

            setSongsDataSource(mSelectedAlbum?.music)
        }

        when {
            selectedArtist != mSelectedArtistOrFolder -> invalidateDetails()
            sFolder -> invalidateDetails()
            else -> mAlbumsRecyclerView.smoothSnapToPosition(playedAlbumPosition)
        }
    }

    //https://stackoverflow.com/a/53986874
    @SuppressLint("StaticFieldLeak")
    private fun RecyclerView.smoothSnapToPosition(position: Int) {
        val smoothScroller = object : LinearSmoothScroller(this.context) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }

            override fun getHorizontalSnapPreference(): Int {
                return SNAP_TO_START
            }

            override fun onStop() {
                super.onStop()
                mAlbumsRecyclerView.findViewHolderForAdapterPosition(position)
                    ?.itemView?.performClick()
            }
        }
        smoothScroller.targetPosition = position
        layoutManager?.startSmoothScroll(smoothScroller)
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

    private fun revealFragment(context: Context, show: Boolean) {

        val radius = max(mArtistDetailsView.width, mArtistDetailsView.height).toFloat()

        val startRadius = if (show) 0f else radius
        val finalRadius = if (show) radius else 0f

        mArtistDetailsAnimator =
            ViewAnimationUtils.createCircularReveal(
                mArtistDetailsView,
                0,
                0,
                startRadius,
                finalRadius
            ).apply {
                interpolator = FastOutSlowInInterpolator()
                duration = REVEAL_DURATION
                start()
            }

        val accent = ThemeHelper.resolveThemeAccent(context)
        val backgroundColor =
            ThemeHelper.resolveColorAttr(context, android.R.attr.windowBackground)
        val startColor = if (show) accent else backgroundColor
        val endColor = if (show) backgroundColor else accent

        startColorAnimation(
            mArtistDetailsView,
            startColor,
            endColor
        )
    }

    private fun startColorAnimation(
        view: View,
        startColor: Int,
        endColor: Int
    ) {
        ValueAnimator().apply {
            setIntValues(startColor, endColor)
            setEvaluator(ArgbEvaluator())
            addUpdateListener { valueAnimator -> view.setBackgroundColor((valueAnimator.animatedValue as Int)) }
            duration = REVEAL_DURATION
            start()
        }
    }

    companion object {

        const val TAG_ARTIST_FOLDER = "SELECTED_ARTIST_FOLDER"
        const val TAG_IS_FOLDER = "IS_FOLDER"
        const val TAG_SELECTED_ALBUM_POSITION = "SELECTED_ALBUM_POSITION"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MusicFragment.
         */
        @JvmStatic
        fun newInstance(
            selectedArtistOrFolder: String?,
            sFolder: Boolean,
            playedAlbumPosition: Int
        ) =
            DetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(TAG_ARTIST_FOLDER, selectedArtistOrFolder)
                    putBoolean(TAG_IS_FOLDER, sFolder)
                    putInt(TAG_SELECTED_ALBUM_POSITION, playedAlbumPosition)
                }
            }
    }

    //viewTreeObserver extension to measure layout params
    //https://antonioleiva.com/kotlin-ongloballayoutlistener/
    private inline fun <T : View> T.afterMeasured(crossinline f: T.() -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    f()
                }
            }
        })
    }
}
