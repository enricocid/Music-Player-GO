package com.iven.musicplayergo.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.LovedSongsAdapter
import com.iven.musicplayergo.adapters.QueueAdapter
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
    fun showQueueSongsDialog(
        context: Context,
        mediaPlayerHolder: MediaPlayerHolder
    ): Pair<MaterialDialog, QueueAdapter>? {

        val dialog = MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

            cornerRadius(res = R.dimen.md_corner_radius)

            title(res = R.string.queue)

            val icon = AppCompatResources.getDrawable(context, R.drawable.ic_queue_music)
            icon?.mutate()
            icon?.setTint(ThemeHelper.resolveColorAttr(context, android.R.attr.textColorPrimary))
            icon(drawable = icon)

            customListAdapter(
                QueueAdapter(context, this, mediaPlayerHolder)
            )
            getRecyclerView().addItemDecoration(
                ThemeHelper.getRecyclerViewDivider(
                    context
                )
            )
            if (goPreferences.isEdgeToEdge && window != null) ThemeHelper.handleEdgeToEdge(
                this.window!!,
                view
            )
        }
        return Pair(dialog, dialog.getListAdapter() as QueueAdapter)
    }

    @JvmStatic
    fun showDeleteQueueSongDialog(
        context: Context,
        song: Pair<Music, Int>,
        queueSongsDialog: MaterialDialog,
        queueAdapter: QueueAdapter,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context).show {

            cornerRadius(res = R.dimen.md_corner_radius)

            title(res = R.string.app_name)
            icon(res = R.drawable.ic_delete_forever)

            message(
                text = context.getString(
                    R.string.queue_song_remove,
                    song.first.title
                )
            )
            positiveButton {

                mediaPlayerHolder.queueSongs.removeAt(song.second)
                queueAdapter.swapQueueSongs(mediaPlayerHolder.queueSongs)

                if (mediaPlayerHolder.queueSongs.isEmpty()) {
                    mediaPlayerHolder.isQueue = false
                    mediaPlayerHolder.mediaPlayerInterface.onQueueStartedOrEnded(false)
                    queueSongsDialog.dismiss()
                }
            }
            negativeButton {}
        }
    }

    @JvmStatic
    fun showClearQueueDialog(
        context: Context,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context).show {

            cornerRadius(res = R.dimen.md_corner_radius)

            title(res = R.string.app_name)
            icon(res = R.drawable.ic_delete_forever)

            message(
                text = context.getString(R.string.queue_songs_clear)
            )
            positiveButton {

                if (mediaPlayerHolder.isQueueStarted && mediaPlayerHolder.isPlaying) {

                    mediaPlayerHolder.restorePreQueueSongs()
                    mediaPlayerHolder.skip(
                        true
                    )
                }

                mediaPlayerHolder.setQueueEnabled(false)
            }
            negativeButton {}
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
                    MusicUtils.formatSongDuration(currentPosition.toLong(), false)
                )
            )
            goPreferences.lovedSongs = lovedSongs
        }
    }

    @JvmStatic
    fun showLovedSongsDialog(
        context: Context,
        uiControlInterface: UIControlInterface,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

            cornerRadius(res = R.dimen.md_corner_radius)
            title(res = R.string.loved_songs)
            icon(res = R.drawable.ic_favorite)

            customListAdapter(
                LovedSongsAdapter(context, this, uiControlInterface, mediaPlayerHolder)
            )
            getRecyclerView().addItemDecoration(
                ThemeHelper.getRecyclerViewDivider(
                    context
                )
            )
            if (goPreferences.isEdgeToEdge && window != null) ThemeHelper.handleEdgeToEdge(
                this.window!!,
                view
            )
        }
    }

    @JvmStatic
    fun showDeleteLovedSongDialog(
        context: Context,
        item: Pair<Music, Int>,
        lovedSongsAdapter: LovedSongsAdapter
    ) {

        val lovedSongs = goPreferences.lovedSongs?.toMutableList()

        MaterialDialog(context).show {

            cornerRadius(res = R.dimen.md_corner_radius)

            title(res = R.string.app_name)
            icon(res = R.drawable.ic_delete_forever)

            message(
                text = context.getString(
                    R.string.loved_song_remove,
                    item.first.title,
                    MusicUtils.formatSongDuration(item.second.toLong(), false)
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

    @JvmStatic
    fun showAddToLovedQueueSongsPopup(
        context: Context,
        itemView: View,
        song: Music,
        uiControlInterface: UIControlInterface
    ) {
        val popup = PopupMenu(context, itemView)
        popup.setOnMenuItemClickListener {

            when (it.itemId) {
                R.id.loved_songs_add -> {
                    addToLovedSongs(
                        context,
                        song,
                        0
                    )
                    uiControlInterface.onLovedSongsUpdate(false)
                }
                R.id.queue_add -> {
                    uiControlInterface.onAddToQueue(song)
                }
            }

            return@setOnMenuItemClickListener true
        }
        popup.inflate(R.menu.menu_do_something)
        popup.gravity = Gravity.END
        popup.show()
    }

    @JvmStatic
    fun showClearLovedSongDialog(
        context: Context,
        uiControlInterface: UIControlInterface
    ) {

        MaterialDialog(context).show {

            cornerRadius(res = R.dimen.md_corner_radius)

            title(res = R.string.app_name)
            icon(res = R.drawable.ic_delete_forever)

            message(
                text = context.getString(R.string.loved_songs_clear)
            )
            positiveButton {
                uiControlInterface.onLovedSongsUpdate(true)
            }
            negativeButton {}
        }
    }

    @JvmStatic
    fun stopPlaybackDialog(
        context: Context,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context).show {

            cornerRadius(res = R.dimen.md_corner_radius)

            title(res = R.string.app_name)
            icon(res = R.drawable.ic_stop)

            message(text = context.getString(R.string.on_close_activity))
            positiveButton {
                mediaPlayerHolder.stopPlaybackService(true)
            }
            negativeButton(text = context.getString(android.R.string.no)) {
                mediaPlayerHolder.stopPlaybackService(false)
            }
        }
    }
}
