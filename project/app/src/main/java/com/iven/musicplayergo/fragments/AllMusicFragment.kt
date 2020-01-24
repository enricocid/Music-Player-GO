package com.iven.musicplayergo.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.emptyDataSource
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

/**
 * A simple [Fragment] subclass.
 * Use the [AllMusicFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllMusicFragment : Fragment(R.layout.fragment_all_music), SearchView.OnQueryTextListener {

    private lateinit var mSongsRecyclerView: RecyclerView

    private var mAllMusic: MutableList<Music>? = null
    private val mDataSource = emptyDataSource()

    private lateinit var mUIControlInterface: UIControlInterface

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mSongsRecyclerView = all_music_rv

        mAllMusic = musicLibrary.allSongsFiltered

        setMusicDataSource(mAllMusic)

        context?.let { cxt ->

            mSongsRecyclerView.apply {

                // setup{} is an extension method on RecyclerView
                setup {

                    // item is a `val` in `this` here
                    withDataSource(mDataSource)

                    if (ThemeHelper.isDeviceLand(resources)) withLayoutManager(
                        GridLayoutManager(
                            cxt,
                            3
                        )
                    )
                    else addItemDecoration(
                        ThemeHelper.getRecyclerViewDivider(cxt)
                    )

                    withItem<Music, SongsViewHolder>(R.layout.song_item_alt) {
                        onBind(::SongsViewHolder) { _, item ->
                            // GenericViewHolder is `this` here
                            title.text = item.title
                            duration.text = MusicUtils.formatSongDuration(item.duration, false)
                            subtitle.text =
                                getString(R.string.artist_and_album, item.artist, item.album)
                        }

                        onClick {
                            mUIControlInterface.onSongSelected(
                                item,
                                MusicUtils.getAlbumSongs(item.artist, item.album)
                            )
                        }

                        onLongClick { index ->
                            Utils.showAddToLovedQueueSongsPopup(
                                cxt,
                                findViewHolderForAdapterPosition(index)?.itemView!!,
                                item,
                                mUIControlInterface
                            )
                        }
                    }
                }
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

    private fun setMusicDataSource(musicList: List<Music>?) {
        musicList?.apply {
            mDataSource.set(this)
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setMusicDataSource(Utils.processQueryForMusic(newText, mAllMusic) ?: mAllMusic)
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MusicFragment.
         */
        @JvmStatic
        fun newInstance() = AllMusicFragment()
    }
}
