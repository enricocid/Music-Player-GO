package com.iven.musicplayergo.fragments

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.content.res.AppCompatResources
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
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.music.Album
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.musicLibrary
import com.iven.musicplayergo.ui.AlbumsViewHolder
import com.iven.musicplayergo.ui.GenericViewHolder
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.UIControlInterface
import kotlinx.android.synthetic.main.fragment_artist_details.*
import kotlin.math.max

/**
 * A simple [Fragment] subclass.
 * Use the [ArtistDetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

const val REVEAL_DURATION: Long = 1000

class ArtistDetailsFragment : Fragment() {

    private lateinit var mArtistDetailsView: View
    private lateinit var mArtistDetailsAnimator: Animator

    private lateinit var mArtistDetailsToolbar: Toolbar

    private lateinit var mSelectedAlbumsDataSource: DataSource<Any>
    private lateinit var mAlbumSongsDataSource: DataSource<Any>

    private lateinit var mAlbumsRecyclerView: RecyclerView
    private lateinit var mSongsRecyclerView: RecyclerView

    private lateinit var mSelectedArtistAlbums: List<Album>
    private lateinit var mSongsForArtist: List<Music>

    private var mSelectedArtist = "unknown"
    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mSelectedAlbum: Album

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG)?.let {
            mSelectedArtist = it
            mSelectedArtistAlbums = musicLibrary.allAlbumsForArtist[mSelectedArtist]!!
            mSongsForArtist = musicLibrary.allSongsForArtist.getValue(mSelectedArtist)
            mSelectedAlbum = mSelectedArtistAlbums[0]
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement MyInterface ")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_artist_details, container, false)
    }

    private fun swapAlbum(songs: MutableList<Music>) {
        mSelectedAlbumsDataSource.set(mSelectedArtistAlbums)
        mAlbumSongsDataSource.set(songs)
        mSongsRecyclerView.scrollToPosition(0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mArtistDetailsView = view

        if (context != null) {

            mArtistDetailsToolbar = artist_details_toolbar

            mArtistDetailsToolbar.title = mSelectedArtist
            mArtistDetailsToolbar.subtitle = getString(
                R.string.artist_info,
                mSelectedArtistAlbums.size,
                mSongsForArtist.size
            )

            mArtistDetailsToolbar.inflateMenu(R.menu.menu_artist_details)
            mArtistDetailsToolbar.overflowIcon =
                AppCompatResources.getDrawable(context!!, R.drawable.ic_shuffle)

            mArtistDetailsToolbar.setNavigationOnClickListener {
                activity?.onBackPressed()
            }

            val itemShuffle = mArtistDetailsToolbar.menu.findItem(R.id.action_shuffle_am)

            itemShuffle.setOnMenuItemClickListener {
                mUIControlInterface.onShuffleSongs(mSongsForArtist.toMutableList())
                return@setOnMenuItemClickListener true
            }

            mArtistDetailsToolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_shuffle_am -> mUIControlInterface.onShuffleSongs(mSongsForArtist.toMutableList())
                    else -> mUIControlInterface.onShuffleSongs(mSelectedAlbum.music!!)
                }
                return@setOnMenuItemClickListener true
            }

            mAlbumsRecyclerView = albums_rv
            mSongsRecyclerView = songs_rv

            mSelectedAlbumsDataSource = dataSourceOf(mSelectedArtistAlbums)

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
                            swapAlbum(item.music!!)
                        }
                    }
                }
            }

            mAlbumSongsDataSource = dataSourceOf(mSelectedAlbum.music!!)

            // setup{} is an extension method on RecyclerView
            mSongsRecyclerView.setup {
                // item is a `val` in `this` here
                withDataSource(mAlbumSongsDataSource)
                withItem<Music, GenericViewHolder>(R.layout.song_item) {
                    onBind(::GenericViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        title.text = MusicUtils.buildSpanned(
                            getString(
                                R.string.track_song,
                                MusicUtils.formatSongTrack(item.track),
                                item.title
                            )
                        )
                        subtitle.text = MusicUtils.formatSongDuration(item.duration)
                    }

                    onClick {
                        mUIControlInterface.onSongSelected(
                            item,
                            MusicUtils.getAlbumFromList(
                                item.album,
                                mSelectedArtistAlbums
                            ).first.music!!.toList()
                        )
                    }
                }
            }

            if (goPreferences.isDividerEnabled) mSongsRecyclerView.addItemDecoration(
                ThemeHelper.getRecyclerViewDivider(
                    context!!
                )
            )
        }
        view.afterMeasured {
            revealFragment(true)
        }
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

    fun updateView(selectedArtist: String) {
        if (selectedArtist != mSelectedArtist) {
            mSelectedArtist = selectedArtist
            mSelectedArtistAlbums = musicLibrary.allAlbumsForArtist[mSelectedArtist]!!
            mSongsForArtist = musicLibrary.allSongsForArtist.getValue(mSelectedArtist)
            mSelectedAlbum = mSelectedArtistAlbums[0]

            mArtistDetailsToolbar.title = mSelectedArtist
            mArtistDetailsToolbar.subtitle = getString(
                R.string.artist_info,
                mSelectedArtistAlbums.size,
                mSongsForArtist.size
            )

            mSelectedAlbumsDataSource.set(mSelectedArtistAlbums)
            mAlbumSongsDataSource.set(mSelectedAlbum.music!!)
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MusicFragment.
         */
        @JvmStatic
        fun newInstance(selectedArtist: String) = ArtistDetailsFragment().apply {
            arguments = Bundle().apply {
                putString(TAG, selectedArtist)
            }
        }

        @JvmStatic
        val TAG = "SELECTED_ARTIST"
    }

    fun onHandleBackPressed(): Animator {
        revealFragment(false)
        return mArtistDetailsAnimator
    }
}
