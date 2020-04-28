package com.iven.musicplayergo.helpers

import android.content.Context
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.MusicRepository
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.LovedSongsAdapter
import com.iven.musicplayergo.adapters.QueueAdapter
import com.iven.musicplayergo.extensions.toFormattedDuration
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.models.SavedMusic
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.UIControlInterface
import de.halfbit.edgetoedge.Edge
import de.halfbit.edgetoedge.edgeToEdge

object DialogHelper {

    @JvmStatic
    fun showQueueSongsDialog(
        context: Context,
        mediaPlayerHolder: MediaPlayerHolder
    ) = MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

        title(R.string.queue)

        customListAdapter(
            QueueAdapter(context, this, mediaPlayerHolder)
        )

        val recyclerView = getRecyclerView()

        if (ThemeHelper.isDeviceLand(context.resources)) {
            recyclerView.layoutManager = GridLayoutManager(context, 3)
        } else {
            recyclerView.addItemDecoration(
                ThemeHelper.getRecyclerViewDivider(
                    context
                )
            )
            if (goPreferences.isEdgeToEdge) {
                window?.apply {
                    ThemeHelper.handleLightSystemBars(context.resources.configuration, decorView)
                    edgeToEdge {
                        recyclerView.fit { Edge.Bottom }
                        decorView.fit { Edge.Top }
                    }
                }
            }
        }
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

            title(R.string.queue)

            message(
                text = context.getString(
                    R.string.queue_song_remove,
                    song.first.title
                )
            )
            positiveButton(R.string.yes) {

                mediaPlayerHolder.apply {
                    queueSongs.removeAt(song.second)
                    queueAdapter.swapQueueSongs(queueSongs)

                    if (queueSongs.isEmpty()) {
                        isQueue = false
                        mediaPlayerInterface.onQueueStartedOrEnded(false)
                        queueSongsDialog.dismiss()
                    }
                }
            }
            negativeButton(R.string.no)
        }
    }

    @JvmStatic
    fun showClearQueueDialog(
        context: Context,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context).show {

            title(R.string.queue)

            message(R.string.queue_songs_clear)

            positiveButton(R.string.yes) {

                mediaPlayerHolder.apply {
                    if (isQueueStarted && isPlaying) {

                        restorePreQueueSongs()
                        skip(
                            true
                        )
                    }
                    setQueueEnabled(false)
                }
            }
            negativeButton(R.string.no)
        }
    }

    @JvmStatic
    fun showLovedSongsDialog(
        context: Context,
        uiControlInterface: UIControlInterface,
        mediaPlayerHolder: MediaPlayerHolder,
        musicRepository: MusicRepository
    ) {

        MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

            title(R.string.loved_songs)

            customListAdapter(
                LovedSongsAdapter(
                    context,
                    this,
                    uiControlInterface,
                    mediaPlayerHolder,
                    musicRepository
                )
            )

            val recyclerView = getRecyclerView()

            if (ThemeHelper.isDeviceLand(context.resources)) {
                recyclerView.layoutManager = GridLayoutManager(context, 3)
            } else {
                recyclerView.addItemDecoration(
                    ThemeHelper.getRecyclerViewDivider(
                        context
                    )
                )
                if (goPreferences.isEdgeToEdge) {
                    window?.apply {
                        ThemeHelper.handleLightSystemBars(
                            context.resources.configuration,
                            decorView
                        )
                        edgeToEdge {
                            recyclerView.fit { Edge.Bottom }
                            decorView.fit { Edge.Top }
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun showDeleteLovedSongDialog(
        context: Context,
        songToDelete: SavedMusic?,
        lovedSongsAdapter: LovedSongsAdapter
    ) {

        val lovedSongs = goPreferences.lovedSongs?.toMutableList()

        MaterialDialog(context).show {

            title(R.string.loved_songs)

            message(
                text = context.getString(
                    R.string.loved_song_remove,
                    songToDelete?.title,
                    songToDelete?.startFrom?.toLong()?.toFormattedDuration(
                        isAlbum = false,
                        isSeekBar = false
                    )
                )
            )
            positiveButton(R.string.yes) {
                lovedSongs?.remove(songToDelete)
                goPreferences.lovedSongs = lovedSongs
                lovedSongsAdapter.swapSongs(lovedSongs)
            }

            negativeButton(R.string.no)
        }
    }

    @JvmStatic
    fun showClearLovedSongDialog(
        context: Context,
        uiControlInterface: UIControlInterface
    ) {

        MaterialDialog(context).show {

            title(R.string.loved_songs)

            message(R.string.loved_songs_clear)
            positiveButton(R.string.yes) {
                uiControlInterface.onLovedSongsUpdate(true)
            }
            negativeButton(R.string.no)
        }
    }

    @JvmStatic
    fun showHidePopup(
        context: Context,
        itemView: View?,
        stringToFilter: String?,
        uiControlInterface: UIControlInterface
    ) {
        itemView?.let { view ->
            PopupMenu(context, view).apply {
                setOnMenuItemClickListener {
                    uiControlInterface.onAddToFilter(stringToFilter)
                    return@setOnMenuItemClickListener true
                }
                inflate(R.menu.menu_filter)
                gravity = Gravity.END
                show()
            }
        }
    }

    @JvmStatic
    fun showDoSomethingPopup(
        context: Context,
        itemView: View?,
        song: Music?,
        isFolder: Boolean,
        uiControlInterface: UIControlInterface
    ) {
        itemView?.let {
            PopupMenu(context, itemView).apply {
                setOnMenuItemClickListener {

                    when (it.itemId) {
                        R.id.loved_songs_add -> {
                            ListsHelper.addOrRemoveFromLovedSongs(
                                context,
                                song,
                                0,
                                isFolder
                            )
                            uiControlInterface.onLovedSongsUpdate(false)
                        }
                        R.id.queue_add -> uiControlInterface.onAddToQueue(song)
                    }

                    return@setOnMenuItemClickListener true
                }
                inflate(R.menu.menu_do_something)
                gravity = Gravity.END
                show()
            }
        }
    }

    @JvmStatic
    fun stopPlaybackDialog(
        context: Context,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context).show {

            title(R.string.app_name)

            message(R.string.on_close_activity)
            positiveButton(R.string.yes) {
                mediaPlayerHolder.stopPlaybackService(true)
            }
            negativeButton(R.string.no) {
                mediaPlayerHolder.stopPlaybackService(false)
            }
        }
    }
}
