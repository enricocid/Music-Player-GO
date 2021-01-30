package com.iven.musicplayergo.helpers

import android.app.Activity
import android.content.Context
import android.text.Spanned
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.text.parseAsHtml
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.callbacks.onCancel
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.LovedSongsAdapter
import com.iven.musicplayergo.adapters.QueueAdapter
import com.iven.musicplayergo.extensions.addBidirectionalSwipeHandler
import com.iven.musicplayergo.extensions.enablePopupIcons
import com.iven.musicplayergo.extensions.setTitle
import com.iven.musicplayergo.extensions.toFormattedDuration
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.models.Music
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
        val queueAdapter = QueueAdapter(context, this, mediaPlayerHolder)

        customListAdapter(queueAdapter)

        val recyclerView = getRecyclerView()

        if (ThemeHelper.isDeviceLand(context.resources)) {
            recyclerView.layoutManager = GridLayoutManager(context, 3)
        } else {
            if (VersioningHelper.isOreoMR1()) {
                window?.let { win ->
                    edgeToEdge {
                        recyclerView.fit { Edge.Bottom }
                        win.decorView.fit { Edge.Top }
                    }
                }
            }
        }

        recyclerView.addBidirectionalSwipeHandler(context, true) { viewHolder: RecyclerView.ViewHolder,
                                                                   _: Int ->
            val title = viewHolder.itemView.findViewById<TextView>(R.id.title)
            if (!queueAdapter.performQueueSongDeletion(viewHolder.adapterPosition, title, true)) {
                queueAdapter.notifyItemChanged(viewHolder.adapterPosition)
            }
        }
    }

    @JvmStatic
    fun showDeleteQueueSongDialog(
            context: Context,
            song: Pair<Music, Int>,
            queueSongsDialog: MaterialDialog,
            queueAdapter: QueueAdapter,
            mediaPlayerHolder: MediaPlayerHolder,
            isSwipe: Boolean
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

                mediaPlayerHolder.run {
                    queueSongs.removeAt(song.second)
                    queueAdapter.swapQueueSongs(queueSongs)

                    if (queueSongs.isEmpty()) {
                        isQueue = false
                        mediaPlayerInterface.onQueueStartedOrEnded(false)
                        queueSongsDialog.dismiss()
                    }
                }
            }
            negativeButton(R.string.no) {
                if (isSwipe) {
                    queueAdapter.notifyItemChanged(song.second)
                }
            }
            onCancel {
                if (isSwipe) {
                    queueAdapter.notifyItemChanged(song.second)
                }
            }
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

                mediaPlayerHolder.run {
                    if (isQueueStarted) {
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
            activity: Activity,
            mediaPlayerHolder: MediaPlayerHolder
    ): MaterialDialog = MaterialDialog(activity, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

        title(R.string.loved_songs)

        val lovedSongsAdapter = LovedSongsAdapter(
                activity,
                this,
                mediaPlayerHolder
        )

        customListAdapter(lovedSongsAdapter)

        val recyclerView = getRecyclerView()

        if (ThemeHelper.isDeviceLand(context.resources)) {
            recyclerView.layoutManager = GridLayoutManager(context, 3)
        } else {
            if (VersioningHelper.isOreoMR1()) {
                window?.let { win ->
                    edgeToEdge {
                        recyclerView.fit { Edge.Bottom }
                        win.decorView.fit { Edge.Top }
                    }
                }
            }
        }

        recyclerView.addBidirectionalSwipeHandler(context, true) { viewHolder: RecyclerView.ViewHolder, _: Int ->
            lovedSongsAdapter.performLovedSongDeletion(
                    viewHolder.adapterPosition,
                    true
            )
        }
    }

    @JvmStatic
    fun showDeleteLovedSongDialog(
            activity: Activity,
            songToDelete: Music?,
            lovedSongsAdapter: LovedSongsAdapter,
            isSwipe: Pair<Boolean, Int>
    ) {

        val lovedSongs = goPreferences.lovedSongs?.toMutableList()

        MaterialDialog(activity).show {

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
                (activity as UIControlInterface).onLovedSongAdded(songToDelete, false)
            }

            negativeButton(R.string.no) {
                if (isSwipe.first) {
                    lovedSongsAdapter.notifyItemChanged(isSwipe.second)
                }
            }
            onCancel {
                if (isSwipe.first) {
                    lovedSongsAdapter.notifyItemChanged(isSwipe.second)
                }
            }
        }
    }

    @JvmStatic
    fun showClearLovedSongDialog(
            activity: Activity
    ) {
        MaterialDialog(activity).show {

            title(R.string.loved_songs)

            message(R.string.loved_songs_clear)
            positiveButton(R.string.yes) {
                (activity as UIControlInterface).onLovedSongsUpdate(true)
            }
            negativeButton(R.string.no)
        }
    }

    @JvmStatic
    fun showPopupForHide(
            activity: Activity,
            itemView: View?,
            stringToFilter: String?
    ) {
        itemView?.let { view ->

            PopupMenu(activity, view).apply {

                inflate(R.menu.popup_filter)

                menu.findItem(R.id.music_container_title).setTitle(activity, stringToFilter)
                menu.enablePopupIcons(activity)
                gravity = Gravity.END

                setOnMenuItemClickListener {
                    (activity as UIControlInterface).onAddToFilter(stringToFilter)
                    return@setOnMenuItemClickListener true
                }
                show()
            }
        }
    }

    @JvmStatic
    fun showPopupForSongs(
            activity: Activity,
            itemView: View?,
            song: Music?,
            launchedBy: String
    ) {
        val uiControlInterface = activity as UIControlInterface

        itemView?.let { view ->

            PopupMenu(activity, view).apply {

                inflate(R.menu.popup_songs)

                menu.findItem(R.id.song_title).setTitle(activity, song?.title)
                menu.enablePopupIcons(activity)
                gravity = Gravity.END

                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.loved_songs_add -> {
                            ListsHelper.addToLovedSongs(
                                    song,
                                    0,
                                    launchedBy
                            )
                            uiControlInterface.onLovedSongAdded(song, true)
                            uiControlInterface.onLovedSongsUpdate(false)
                        }
                        else -> uiControlInterface.onAddToQueue(song, launchedBy)
                    }
                    return@setOnMenuItemClickListener true
                }
                show()
            }
        }
    }

    @JvmStatic
    fun addToLovedSongs(song: Music?, launchedBy: String, uiControlInterface: UIControlInterface) {
        ListsHelper.addToLovedSongs(
                song,
                0,
                launchedBy
        )
        uiControlInterface.onLovedSongAdded(song, true)
        uiControlInterface.onLovedSongsUpdate(false)
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

    @JvmStatic
    fun computeDurationText(ctx: Context, lovedSong: Music?): Spanned? {
        if (lovedSong?.startFrom != null && lovedSong.startFrom > 0L) {
            return ctx.getString(
                    R.string.loved_song_subtitle,
                    lovedSong.startFrom.toLong().toFormattedDuration(
                            isAlbum = false,
                            isSeekBar = false
                    ),
                    lovedSong.duration.toFormattedDuration(isAlbum = false, isSeekBar = false)
            ).parseAsHtml()
        }
        return lovedSong?.duration?.toFormattedDuration(isAlbum = false, isSeekBar = false)
                ?.parseAsHtml()
    }
}
