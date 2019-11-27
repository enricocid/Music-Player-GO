package com.iven.musicplayergo.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.LovedSongsAdapter
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.player.MediaPlayerHolder
import java.util.*

object Utils {

    @JvmStatic
    fun makeToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG)
            .show()
    }

    @JvmStatic
    fun setupSearchViewForStringLists(
        searchView: SearchView,
        list: List<String>,
        onResultsChanged: (List<String>) -> Unit
    ) {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override
            fun onQueryTextChange(newText: String): Boolean {
                onResultsChanged(
                    processQueryForStringsLists(
                        newText,
                        list
                    )
                )
                return false
            }

            override
            fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
        })
    }

    @JvmStatic
    @SuppressLint("DefaultLocale")
    private fun processQueryForStringsLists(
        query: String,
        list: List<String>
    ): List<String> {
        // in real app you'd have it instantiated just once
        val results = mutableListOf<String>()

        try {
            // case insensitive search
            list.iterator().forEach {
                if (it.toLowerCase().contains(query.toLowerCase())) {
                    results.add(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results.toList()
    }

    @JvmStatic
    fun getSortedList(
        id: Int,
        list: MutableList<String>,
        defaultList: MutableList<String>
    ): MutableList<String> {
        return when (id) {

            R.id.ascending_sorting -> {

                Collections.sort(list, String.CASE_INSENSITIVE_ORDER)
                list
            }

            R.id.descending_sorting -> {

                Collections.sort(list, String.CASE_INSENSITIVE_ORDER)
                list.asReversed()
            }
            else -> defaultList
        }
    }

    @JvmStatic
    fun addToLovedSongs(context: Context, song: Music, currentPosition: Int) {
        val lovedSongs =
            if (goPreferences.lovedSongs != null) goPreferences.lovedSongs else mutableListOf()
        if (!lovedSongs?.contains(Pair(song, currentPosition))!!) {
            lovedSongs.add(
                Pair(
                    song,
                    currentPosition
                )
            )
            makeToast(
                context,
                context.getString(
                    R.string.loved_song_added,
                    song.title!!,
                    MusicUtils.formatSongDuration(currentPosition.toLong())
                )
            )
            goPreferences.lovedSongs = lovedSongs
        }
    }

    fun showLovedSongsDialog(
        context: Context,
        uiControlInterface: UIControlInterface,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

            cornerRadius(res = R.dimen.md_corner_radius)
            title(R.string.loved_songs)

            customListAdapter(
                LovedSongsAdapter(context, this, uiControlInterface, mediaPlayerHolder)
            )
            getRecyclerView().addItemDecoration(
                ThemeHelper.getRecyclerViewDivider(
                    context
                )
            )
        }
    }

    @JvmStatic
    fun makeDeleteLovedSongDialog(
        context: Context,
        item: Pair<Music, Int>,
        lovedSongsAdapter: LovedSongsAdapter
    ): MaterialDialog {

        val lovedSongs = goPreferences.lovedSongs?.toMutableList()

        return MaterialDialog(context).show {

            cornerRadius(res = R.dimen.md_corner_radius)
            title(text = item.first.artist)
            message(
                text = context.getString(
                    R.string.loved_song_remove,
                    item.first.title,
                    MusicUtils.formatSongDuration(item.second.toLong())
                )
            )
            positiveButton {
                lovedSongs?.remove(item)
                goPreferences.lovedSongs = lovedSongs
                lovedSongsAdapter.swapSongs(lovedSongs!!)
            }
            negativeButton {}
        }
    }

    fun showAddToLovedSongsPopup(
        context: Context,
        itemView: View,
        song: Music,
        uiControlInterface: UIControlInterface
    ) {
        val popup = PopupMenu(context, itemView)
        popup.setOnMenuItemClickListener {

            addToLovedSongs(
                context,
                song,
                0
            )
            uiControlInterface.onLovedSongsUpdate()
            return@setOnMenuItemClickListener true
        }
        popup.inflate(R.menu.menu_show_love)
        popup.gravity = Gravity.END
        popup.show()
    }
}
