package com.iven.musicplayergo.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.R
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.musicRepo
import com.iven.musicplayergo.ui.GenericViewHolder
import kotlinx.android.synthetic.main.fragment_all_music.*

/**
 * A simple [Fragment] subclass.
 * Use the [AllMusicFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllMusicFragment : Fragment() {

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

            val dataSource = dataSourceOf(musicRepo.allSongsFiltered)
            // setup{} is an extension method on RecyclerView
            songs_rv.setup {
                withDataSource(dataSource)
                withItem<Music, GenericViewHolder>(R.layout.recycler_view_item) {
                    onBind(::GenericViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        title.text = item.title
                        title.isSelected = true
                        subtitle.text =
                            getString(R.string.artist_and_album, item.artist, item.album)
                        subtitle.isSelected = true
                    }
                    onClick { index ->
                        // item is a `val` in `this` here
                        Log.d("yo", "Clicked $index: ${item}")
                    }
                    onLongClick { index ->
                        // item is a `val` in `this` here
                        Log.d("yo2", "Clicked $index: ${item}")
                    }
                }
            }
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
