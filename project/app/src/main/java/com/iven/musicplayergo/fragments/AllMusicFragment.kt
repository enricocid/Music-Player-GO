package com.iven.musicplayergo.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.DataSource
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.R
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.musicLibrary
import com.iven.musicplayergo.ui.SongsViewHolder
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.UIControlInterface
import com.iven.musicplayergo.ui.Utils
import kotlinx.android.synthetic.main.fragment_all_music.*
import kotlinx.android.synthetic.main.search_toolbar.*
import kotlin.properties.Delegates

/**
 * A simple [Fragment] subclass.
 * Use the [AllMusicFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllMusicFragment : Fragment(), SearchView.OnQueryTextListener {

    private lateinit var mSongsRecyclerView: RecyclerView
    private lateinit var mSongsRecyclerViewLayoutManager: LinearLayoutManager

    private lateinit var mAllMusic: MutableList<Music>
    private lateinit var mDataSource: DataSource<Any>

    private lateinit var mUIControlInterface: UIControlInterface

    private var mPlayingSong: String by Delegates.notNull()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_PLAYING_SONG)?.let {
            mPlayingSong = it
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
        return inflater.inflate(R.layout.fragment_all_music, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mSongsRecyclerView = all_music_rv

        mAllMusic = musicLibrary.allSongsFiltered
        mDataSource = dataSourceOf(mAllMusic)

        context?.let {

            mSongsRecyclerView.apply {

                // setup{} is an extension method on RecyclerView
                setup {
                    // item is a `val` in `this` here
                    withDataSource(mDataSource)

                    mSongsRecyclerViewLayoutManager = LinearLayoutManager(
                        it,
                        LinearLayoutManager.VERTICAL,
                        false
                    )

                    withLayoutManager(mSongsRecyclerViewLayoutManager)

                    withItem<Music, SongsViewHolder>(R.layout.song_item_alt) {
                        onBind(::SongsViewHolder) { _, item ->
                            // GenericViewHolder is `this` here
                            title.apply {
                                text = item.title
                                setTextColor(
                                    if (mPlayingSong != item.title) ThemeHelper.resolveColorAttr(
                                        it,
                                        android.R.attr.textColorPrimary
                                    ) else ThemeHelper.resolveThemeAccent(it)
                                )
                            }
                            duration.text = MusicUtils.formatSongDuration(item.duration, false)
                            subtitle.text =
                                getString(R.string.artist_and_album, item.artist, item.album)
                        }

                        onClick {
                            mUIControlInterface.onSongSelected(
                                item,
                                MusicUtils.getAlbumFromList(
                                    item.album,
                                    musicLibrary.allAlbumsByArtist.getValue(item.artist!!)
                                )
                                    .first.music!!.toList(),
                                true
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
                addItemDecoration(
                    ThemeHelper.getRecyclerViewDivider(it)
                )
                mSongsRecyclerViewLayoutManager.scrollToPositionWithOffset(
                    mAllMusic.indexOfFirst {
                        it.title == mPlayingSong
                    }, 0
                )
            }

            search_toolbar.apply {

                inflateMenu(R.menu.menu_all_music)
                title = getString(R.string.songs)
                setNavigationOnClickListener {
                    mUIControlInterface.onCloseActivity()
                }

                menu.apply {
                    findItem(R.id.action_shuffle_am).setOnMenuItemClickListener {
                        mUIControlInterface.onShuffleSongs(mAllMusic)
                        return@setOnMenuItemClickListener true
                    }
                    val searchView = findItem(R.id.action_search).actionView as SearchView
                    searchView.apply {
                        setOnQueryTextListener(this@AllMusicFragment)
                        setOnQueryTextFocusChangeListener { _, hasFocus ->
                            menu.findItem(R.id.action_shuffle_am).isVisible = !hasFocus
                        }
                    }
                }
            }
        }
    }

    fun swapPlayingSong(playingSong: String, isUserClicking: Boolean) {
        if (playingSong != mPlayingSong) {
            mSongsRecyclerView.adapter?.apply {
                notifyItemChanged(mAllMusic.indexOfFirst {
                    it.title == mPlayingSong
                })
                val newSelectedPosition = mAllMusic.indexOfFirst {
                    it.title == playingSong
                }
                mSongsRecyclerViewLayoutManager.scrollToPositionWithOffset(newSelectedPosition, 0)
                notifyItemChanged(newSelectedPosition)
            }
            mPlayingSong = playingSong
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        mDataSource.set(Utils.processQueryForMusic(newText, mAllMusic) ?: mAllMusic)
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    companion object {

        const val TAG_PLAYING_SONG = "PLAYING_SONG"
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MusicFragment.
         */
        @JvmStatic
        fun newInstance(playingSong: String?) = AllMusicFragment().apply {
            arguments = Bundle().apply {
                putString(TAG_PLAYING_SONG, playingSong)
            }
        }
    }
}
