package com.iven.musicplayergo.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.DataSource
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.musicLibrary
import com.iven.musicplayergo.ui.GenericViewHolder
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.UIControlInterface
import kotlinx.android.synthetic.main.fragment_all_music.*
import kotlinx.android.synthetic.main.search_toolbar.*

/**
 * A simple [Fragment] subclass.
 * Use the [AllMusicFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllMusicFragment : Fragment() {

    private lateinit var mUIControlInterface: UIControlInterface

    private lateinit var mAllMusic: MutableList<Music>
    private lateinit var mDataSource: DataSource<Any>

    private lateinit var mSongsRecyclerView: RecyclerView

    override fun onAttach(context: Context) {
        super.onAttach(context)

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
        return inflater.inflate(R.layout.fragment_all_music, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context != null) {

            val searchToolbar = search_toolbar
            searchToolbar.inflateMenu(R.menu.menu_all_music)
            searchToolbar.title = getString(R.string.songs)
            val itemSearch = searchToolbar.menu.findItem(R.id.action_search)

            val isSearchBarEnabled = goPreferences.isSearchBarEnabled
            itemSearch.isVisible = isSearchBarEnabled

            val itemShuffle = searchToolbar.menu.findItem(R.id.action_shuffle_am)
            itemShuffle.setOnMenuItemClickListener {
                mUIControlInterface.onShuffleSongs(mAllMusic)
                return@setOnMenuItemClickListener true
            }

            mAllMusic = musicLibrary.allSongsFiltered

            mDataSource = dataSourceOf(mAllMusic)

            mSongsRecyclerView = all_music_rv

            // setup{} is an extension method on RecyclerView
            mSongsRecyclerView.setup {
                // item is a `val` in `this` here
                withDataSource(mDataSource)
                withItem<Music, GenericViewHolder>(R.layout.song_item) {
                    onBind(::GenericViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        title.text = item.title
                        subtitle.text =
                            getString(R.string.artist_and_album, item.artist, item.album)
                    }

                    onClick {
                        mUIControlInterface.onSongSelected(
                            item,
                            MusicUtils.getAlbumFromList(
                                item.album,
                                musicLibrary.allAlbumsForArtist.getValue(item.artist!!)
                            )
                                .first.music!!.toList()
                        )
                    }
                }
            }

            mSongsRecyclerView.addItemDecoration(
                ThemeHelper.getRecyclerViewDivider(context!!)
            )

            if (isSearchBarEnabled) {
                val searchView = itemSearch.actionView as SearchView
                setupSearchViewForMusic(searchView)
            }
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
            mAllMusic.iterator().forEach {
                if (it.title?.toLowerCase()!!.contains(query.toLowerCase())) {
                    results.add(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (results.size > 0) {
            mDataSource.set(results)
        }
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
