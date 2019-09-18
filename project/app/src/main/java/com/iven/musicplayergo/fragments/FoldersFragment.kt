package com.iven.musicplayergo.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.R
import com.iven.musicplayergo.musicRepo
import com.iven.musicplayergo.ui.GenericViewHolder
import kotlinx.android.synthetic.main.fragment_folders.*
import java.io.File

/**
 * A simple [Fragment] subclass.
 * Use the [FoldersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FoldersFragment : Fragment() {

    private lateinit var mFolders: MutableList<String>
    private lateinit var mFoldersRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_folders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context != null) {

            mFoldersRecyclerView = main_rv

            mFolders = musicRepo.allCategorizedMusicByFolder.keys.toMutableList()
            val dataSource = dataSourceOf(mFolders)

            // setup{} is an extension method on RecyclerView
            mFoldersRecyclerView.setup {
                withDataSource(dataSource)
                withItem<String, GenericViewHolder>(R.layout.recycler_view_item) {
                    onBind(::GenericViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        title.text = item
                        title.isSelected = true

                        //getting parent path of the first song
                        val songRootPath =
                            musicRepo.allCategorizedMusicByFolder.getValue(item)[0].path
                        val parentPath = File(songRootPath!!).parentFile?.parent

                        subtitle.text = getString(R.string.in_directory, parentPath)
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
         * @return A new instance of fragment NowPlaying.
         */
        @JvmStatic
        fun newInstance() = FoldersFragment()
    }
}
