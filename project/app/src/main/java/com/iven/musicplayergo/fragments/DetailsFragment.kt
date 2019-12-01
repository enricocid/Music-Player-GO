package com.iven.musicplayergo.fragments

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
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

/**
 * A simple [Fragment] subclass.
 * Use the [DetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

const val REVEAL_DURATION: Long = 1000

class DetailsFragment : Fragment() {

    var isFolder = false

    private lateinit var mArtistDetailsView: View
    private lateinit var mArtistDetailsAnimator: Animator

    private lateinit var mDetailsToolbar: Toolbar

    private lateinit var mSelectedAlbumsDataSource: DataSource<Any>
    private lateinit var mSongsDataSource: DataSource<Any>

    private lateinit var mAlbumsRecyclerView: RecyclerView
    private lateinit var mSongsRecyclerView: RecyclerView

    private lateinit var mSelectedArtistAlbums: List<Album>
    private lateinit var mSongsForArtistOrFolder: List<Music>

    private var mSelectedArtistOrFolder = "unknown"
    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mSelectedAlbum: Album

    private lateinit var mToolbarSubtitle: String

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_ARTIST_FOLDER)?.let {
            mSelectedArtistOrFolder = it
        }

        arguments?.getBoolean(TAG_IS_FOLDER)?.let {
            isFolder = it
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
        revealFragment(false)
        return mArtistDetailsAnimator
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mArtistDetailsView = view

        if (context != null) {

            mDetailsToolbar = details_toolbar

            mDetailsToolbar.title = mSelectedArtistOrFolder

            if (isFolder) mDetailsToolbar.elevation =
                resources.getDimensionPixelSize(R.dimen.search_bar_elevation).toFloat()

            mDetailsToolbar.setNavigationOnClickListener {
                activity?.onBackPressed()
            }

            val menu = if (isFolder) R.menu.menu_folder_details else R.menu.menu_artist_details
            mDetailsToolbar.inflateMenu(menu)
            mDetailsToolbar.overflowIcon =
                AppCompatResources.getDrawable(context!!, R.drawable.ic_shuffle)

            val itemShuffle = mDetailsToolbar.menu.findItem(R.id.action_shuffle_sa)

            itemShuffle.isVisible = !isFolder

            mDetailsToolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_shuffle_am -> mUIControlInterface.onShuffleSongs(
                        mSongsForArtistOrFolder.toMutableList()
                    )
                    R.id.action_shuffle_sa -> mUIControlInterface.onShuffleSongs(mSelectedAlbum.music!!)
                }
                return@setOnMenuItemClickListener true
            }

            mAlbumsRecyclerView = albums_rv
            mSongsRecyclerView = songs_rv

            if (!isFolder) {

                mSelectedArtistAlbums = musicLibrary.allAlbumsForArtist[mSelectedArtistOrFolder]!!
                mSongsForArtistOrFolder =
                    musicLibrary.allSongsForArtist.getValue(mSelectedArtistOrFolder)
                mSelectedAlbum = mSelectedArtistAlbums[0]

                mSelectedAlbumsDataSource = dataSourceOf(mSelectedArtistAlbums)

                mToolbarSubtitle = getString(
                    R.string.artist_info,
                    mSelectedArtistAlbums.size,
                    mSongsForArtistOrFolder.size
                )

                mAlbumsRecyclerView.setup {
                    withDataSource(mSelectedAlbumsDataSource)
                    withLayoutManager(
                        LinearLayoutManager(
                            context,
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                    )

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

                        onClick {

                            if (mSelectedAlbum.title != item.title) {

                                mAlbumsRecyclerView.adapter?.notifyItemChanged(
                                    MusicUtils.getAlbumFromList(
                                        item.title,
                                        mSelectedArtistAlbums
                                    ).second
                                )

                                mAlbumsRecyclerView.adapter?.notifyItemChanged(
                                    MusicUtils.getAlbumFromList(
                                        mSelectedAlbum.title,
                                        mSelectedArtistAlbums
                                    ).second
                                )
                                mSelectedAlbum = item
                                swapAlbum(item.music)
                            }
                        }
                    }
                }

            } else {

                mAlbumsRecyclerView.visibility = View.GONE

                mSongsForArtistOrFolder =
                    musicLibrary.allSongsForFolder.getValue(mSelectedArtistOrFolder)

                mToolbarSubtitle = getString(
                    R.string.folder_info,
                    mSongsForArtistOrFolder.size
                )

                val itemSearch = mDetailsToolbar.menu.findItem(R.id.action_search)
                val searchView = itemSearch.actionView as SearchView
                setupSearchViewForMusic(searchView)
            }

            mDetailsToolbar.subtitle = mToolbarSubtitle

            mSongsDataSource =
                dataSourceOf(if (isFolder) mSongsForArtistOrFolder else mSelectedAlbum.music!!)

            // setup{} is an extension method on RecyclerView
            mSongsRecyclerView.setup {
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
                        val selectedArtistAlbums =

                            if (isFolder) MusicUtils.getAlbumFromList(
                                item.album,
                                musicLibrary.allAlbumsForArtist[item.artist]!!
                            ).first.music!!.toList()
                            else
                                MusicUtils.getAlbumFromList(
                                    item.album,
                                    mSelectedArtistAlbums
                                ).first.music!!.toList()

                        mUIControlInterface.onSongSelected(
                            item,
                            selectedArtistAlbums
                        )
                    }

                    onLongClick { index ->
                        val itemViewHolder =
                            mSongsRecyclerView.findViewHolderForAdapterPosition(index)
                        Utils.showAddToLovedSongsPopup(
                            context!!,
                            itemViewHolder?.itemView!!,
                            item,
                            mUIControlInterface
                        )
                    }
                }
            }

            mSongsRecyclerView.addItemDecoration(
                ThemeHelper.getRecyclerViewDivider(
                    context!!
                )
            )
        }
        view.afterMeasured {
            revealFragment(true)
        }
    }

    fun updateView(selectedArtist: String) {
        if (selectedArtist != mSelectedArtistOrFolder) {

            mSelectedArtistOrFolder = selectedArtist
            mSelectedArtistAlbums = musicLibrary.allAlbumsForArtist[mSelectedArtistOrFolder]!!
            mSongsForArtistOrFolder =
                musicLibrary.allSongsForArtist.getValue(mSelectedArtistOrFolder)
            mSelectedAlbum = mSelectedArtistAlbums[0]

            mDetailsToolbar.title = mSelectedArtistOrFolder
            mDetailsToolbar.subtitle = getString(
                R.string.artist_info,
                mSelectedArtistAlbums.size,
                mSongsForArtistOrFolder.size
            )

            mSelectedAlbumsDataSource.set(mSelectedArtistAlbums)
            mSongsDataSource.set(mSelectedAlbum.music!!)
        }
    }

    private fun swapAlbum(songs: MutableList<Music>?) {
        mSelectedAlbumsDataSource.set(mSelectedArtistAlbums)
        mSongsDataSource.set(songs!!)
        mSongsRecyclerView.scrollToPosition(0)
    }

    private fun revealFragment(show: Boolean) {

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
            )

        mArtistDetailsAnimator.interpolator = FastOutSlowInInterpolator()
        mArtistDetailsAnimator.duration = REVEAL_DURATION
        mArtistDetailsAnimator.start()

        val accent = ThemeHelper.resolveThemeAccent(context!!)
        val backgroundColor =
            ThemeHelper.resolveColorAttr(context!!, android.R.attr.windowBackground)
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
        val anim = ValueAnimator()
        anim.setIntValues(startColor, endColor)
        anim.setEvaluator(ArgbEvaluator())
        anim.addUpdateListener { valueAnimator -> view.setBackgroundColor((valueAnimator.animatedValue as Int)) }
        anim.duration = REVEAL_DURATION
        anim.start()
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

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MusicFragment.
         */
        @JvmStatic
        fun newInstance(selectedArtistOrFolder: String, isFolder: Boolean) =
            DetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(TAG_ARTIST_FOLDER, selectedArtistOrFolder)
                    putBoolean(TAG_IS_FOLDER, isFolder)
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
