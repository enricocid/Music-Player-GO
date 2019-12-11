package com.iven.musicplayergo.fragments

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator
import com.afollestad.recyclical.datasource.DataSource
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
import kotlin.properties.Delegates

/**
 * A simple [Fragment] subclass.
 * Use the [DetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

private const val REVEAL_DURATION: Long = 1000

class DetailsFragment : Fragment() {

    private var sFolder = false

    private lateinit var mArtistDetailsView: View
    private lateinit var mArtistDetailsAnimator: Animator

    private lateinit var mDetailsToolbar: Toolbar

    private lateinit var mSelectedAlbumTitle: TextView
    private lateinit var mSelectedAlbumYearDuration: TextView

    private lateinit var mSelectedAlbumsDataSource: DataSource<Any>
    private lateinit var mSongsDataSource: DataSource<Any>

    private lateinit var mAlbumsRecyclerView: RecyclerView
    private lateinit var mAlbumsRecyclerViewLayoutManager: LinearLayoutManager
    private lateinit var mSongsRecyclerView: RecyclerView

    private lateinit var mSelectedArtistAlbums: List<Album>
    private lateinit var mSongsForArtistOrFolder: List<Music>

    private var mSelectedArtistOrFolder: String by Delegates.notNull()
    private var mSelectedAlbumPosition = -1

    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mSelectedAlbum: Album

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

            mSelectedArtistAlbums = musicLibrary.allAlbumsForArtist[mSelectedArtistOrFolder]!!
            mSongsForArtistOrFolder =
                musicLibrary.allSongsForArtist.getValue(mSelectedArtistOrFolder)

            mSelectedAlbum =
                if (mSelectedAlbumPosition != -1) {
                    mSelectedArtistAlbums[mSelectedAlbumPosition]
                } else {
                    mSelectedAlbumPosition = 0
                    mSelectedArtistAlbums[0]
                }
        } else {
            mSongsForArtistOrFolder =
                musicLibrary.allSongsForFolder.getValue(mSelectedArtistOrFolder)
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

    fun onHandleBackPressed(): Animator {
        context?.let {
            revealFragment(it, false)
        }
        return mArtistDetailsAnimator
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

            mDetailsToolbar.apply {

                overflowIcon = AppCompatResources.getDrawable(it, R.drawable.ic_shuffle)

                title = mSelectedArtistOrFolder

                if (sFolder) elevation =
                    resources.getDimensionPixelSize(R.dimen.search_bar_elevation).toFloat()

                setNavigationOnClickListener {
                    activity?.onBackPressed()
                }

                setupMenu(false, this)
            }

            if (!sFolder) {

                setupAlbumsContainer()

            } else {

                mAlbumsRecyclerView.visibility = View.GONE
                mSelectedAlbumTitle.visibility = View.GONE
                mSelectedAlbumYearDuration.visibility = View.GONE

                mSongsForArtistOrFolder =
                    musicLibrary.allSongsForFolder.getValue(mSelectedArtistOrFolder)

                mDetailsToolbar.subtitle = getString(
                    R.string.folder_info,
                    mSongsForArtistOrFolder.size
                )

                val itemSearch = mDetailsToolbar.menu.findItem(R.id.action_search)
                setupSearchViewForMusic(itemSearch.actionView as SearchView)
            }

            mSongsDataSource =
                dataSourceOf(if (sFolder) mSongsForArtistOrFolder else mSelectedAlbum.music!!)

            mSongsRecyclerView.apply {

                // setup{} is an extension method on RecyclerView
                setup {
                    // item is a `val` in `this` here
                    withDataSource(mSongsDataSource)
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
                                    MusicUtils.getAlbumFromList(
                                        item.album,
                                        mSelectedArtistAlbums
                                    ).first.music!!.toList()

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

                addItemDecoration(ThemeHelper.getRecyclerViewDivider(it))
            }

            view.afterMeasured {
                revealFragment(it, true)
            }
        }
    }

    private fun setupMenu(isUpdateView: Boolean, toolbar: Toolbar) {

        toolbar.apply {

            if (isUpdateView) mDetailsToolbar.menu.clear()

            val menuToInflate =
                if (sFolder && !isUpdateView) R.menu.menu_folder_details else R.menu.menu_artist_details
            inflateMenu(menuToInflate)

            menu.apply {
                findItem(R.id.action_shuffle_sa).isVisible = !sFolder
            }

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_shuffle_am -> mUIControlInterface.onShuffleSongs(
                        mSongsForArtistOrFolder.toMutableList()
                    )
                    R.id.action_shuffle_sa -> mUIControlInterface.onShuffleSongs(mSelectedAlbum.music!!)
                }
                return@setOnMenuItemClickListener true
            }
        }

    }

    private fun setupAlbumsContainer() {

        selected_album_container.setOnClickListener {
            mAlbumsRecyclerViewLayoutManager.scrollToPositionWithOffset(
                mSelectedAlbumPosition,
                0
            )
        }

        mSelectedAlbumTitle.isSelected = true

        updateSelectedAlbumTitle()

        mSelectedAlbumsDataSource = dataSourceOf(mSelectedArtistAlbums)

        mDetailsToolbar.subtitle = getString(
            R.string.artist_info,
            mSelectedArtistAlbums.size,
            mSongsForArtistOrFolder.size
        )

        mAlbumsRecyclerView.apply {

            setup {

                withDataSource(mSelectedAlbumsDataSource)

                mAlbumsRecyclerViewLayoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
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
                            MusicUtils.formatSongDuration(item.totalDuration!!, true)
                        checkbox.visibility =
                            if (mSelectedAlbum.title != item.title) View.GONE else View.VISIBLE
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
            if (mSelectedAlbumPosition != -1 or 0) mAlbumsRecyclerViewLayoutManager.scrollToPositionWithOffset(
                mSelectedAlbumPosition,
                0
            )
        }
    }

    fun updateView(selectedArtist: String, playedAlbumPosition: Int) {

        if (selectedArtist != mSelectedArtistOrFolder) {

            mSelectedArtistOrFolder = selectedArtist
            mSelectedArtistAlbums = musicLibrary.allAlbumsForArtist[mSelectedArtistOrFolder]!!

            //restore album position
            if (playedAlbumPosition != -1) {
                mSelectedAlbumPosition = playedAlbumPosition
                mSelectedAlbum = mSelectedArtistAlbums[mSelectedAlbumPosition]
            }

            if (sFolder) {
                sFolder = false
                mDetailsToolbar.title = mSelectedArtistOrFolder
                mAlbumsRecyclerView.visibility = View.VISIBLE
                mSelectedAlbumTitle.visibility = View.VISIBLE
                mSelectedAlbumYearDuration.visibility = View.VISIBLE
                mSelectedAlbumsDataSource = dataSourceOf(mSelectedArtistAlbums)

                setupMenu(true, mDetailsToolbar)
                setupAlbumsContainer()

            } else {

                mDetailsToolbar.apply {
                    title = mSelectedArtistOrFolder
                    subtitle = getString(
                        R.string.artist_info,
                        mSelectedArtistAlbums.size,
                        mSongsForArtistOrFolder.size
                    )
                }

                mSelectedAlbumsDataSource.set(mSelectedArtistAlbums)
                updateSelectedAlbumTitle()
            }

            mAlbumsRecyclerViewLayoutManager.scrollToPositionWithOffset(
                mSelectedAlbumPosition,
                0
            )

            mSongsForArtistOrFolder =
                musicLibrary.allSongsForArtist.getValue(mSelectedArtistOrFolder)

            mSongsDataSource.set(mSelectedAlbum.music!!)
        }
    }

    private fun updateSelectedAlbumTitle() {
        mSelectedAlbumTitle.text = mSelectedAlbum.title
        mSelectedAlbumYearDuration.text = getString(
            R.string.year_and_duration,
            mSelectedAlbum.year,
            MusicUtils.formatSongDuration(mSelectedAlbum.totalDuration!!, true)
        )
    }

    private fun swapAlbum(songs: MutableList<Music>?) {
        mSongsDataSource.set(songs!!)
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

    private fun setupSearchViewForMusic(searchView: SearchView) {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override
            fun onQueryTextChange(newText: String): Boolean {
                processQuery(newText)
                return false
            }

            override
            fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
        })
    }

    @SuppressLint("DefaultLocale")
    private fun processQuery(query: String) {
        // in real app you'd have it instantiated just once
        val results = mutableListOf<Any>()

        try {
            // case insensitive search
            mSongsForArtistOrFolder.iterator().forEach {
                if (it.title?.toLowerCase()!!.contains(query.toLowerCase())) {
                    results.add(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (results.size > 0) {
            mSongsDataSource.set(results)
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
            selectedArtistOrFolder: String,
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
