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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.callbacks.onCancel
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.ui.ItemSwipeCallback
import com.iven.musicplayergo.adapters.LovedSongsAdapter
import com.iven.musicplayergo.adapters.QueueAdapter
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.ItemTouchCallback
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

        ItemTouchHelper(ItemTouchCallback(queueAdapter.queueSongs, false))
                .attachToRecyclerView(recyclerView)

        ItemTouchHelper(ItemSwipeCallback(context, true) { viewHolder: RecyclerView.ViewHolder, _: Int ->
            val title = viewHolder.itemView.findViewById<TextView>(R.id.title)
            if (!queueAdapter.performQueueSongDeletion(viewHolder.adapterPosition, title)) {
                queueAdapter.notifyItemChanged(viewHolder.adapterPosition)
            }
        }).attachToRecyclerView(recyclerView)

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

        ItemTouchHelper(ItemSwipeCallback(context, false) { viewHolder: RecyclerView.ViewHolder,
                                                            direction: Int ->
            if (direction == ItemTouchHelper.RIGHT) {
                lovedSongsAdapter.addLovedSongToQueue(viewHolder.adapterPosition)
            } else {
                lovedSongsAdapter.performLovedSongDeletion(
                        viewHolder.adapterPosition,
                        true
                )
            }
            lovedSongsAdapter.notifyDataSetChanged()
        }).attachToRecyclerView(recyclerView)
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
                (activity as MediaControlInterface).onLovedSongAdded(songToDelete, false)
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
        val mediaControlInterface = activity as MediaControlInterface
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
                            mediaControlInterface.onLovedSongAdded(song, true)
                            (activity as UIControlInterface).onLovedSongsUpdate(false)
                        }
                        else -> mediaControlInterface.onAddToQueue(song, launchedBy)
                    }
                    return@setOnMenuItemClickListener true
                }
                show()
            }
        }
    }

    @JvmStatic
    fun showPopupForPlaybackSpeed(
            activity: Activity,
            view: View
    ) {

        PopupMenu(activity, view).apply {
            inflate(R.menu.popup_speed)
            gravity = Gravity.END

            if (goPreferences.isPlaybackSpeedPersisted) {
                menu.findItem(getSelectedPlaybackItem(goPreferences.latestPlaybackSpeed)).setTitleColor(ThemeHelper.resolveThemeAccent(activity))
            }

            setOnMenuItemClickListener { menuItem ->
                val playbackSpeed = when (menuItem.itemId) {
                    R.id.speed_0 -> 0.25F
                    R.id.speed_1 -> 0.5F
                    R.id.speed_2 -> 0.75F
                    R.id.speed_3 -> 1.0F
                    R.id.speed_4 -> 1.25F
                    R.id.speed_5 -> 1.5F
                    R.id.speed_6 -> 1.75F
                    R.id.speed_7 -> 2.0F
                    R.id.speed_8 -> 2.5F
                    else -> 2.5F
                }
                if (goPreferences.isPlaybackSpeedPersisted) {
                    menu.findItem(getSelectedPlaybackItem(playbackSpeed)).setTitleColor(ThemeHelper.resolveThemeAccent(activity))
                }
                (activity as MediaControlInterface).onChangePlaybackSpeed(playbackSpeed)
                return@setOnMenuItemClickListener true
            }
            show()
        }
    }

    private fun getSelectedPlaybackItem(playbackSpeed: Float) = when (playbackSpeed) {
        0.25F -> R.id.speed_0
        0.5F -> R.id.speed_1
        0.75F -> R.id.speed_2
        1.0F -> R.id.speed_3
        1.25F -> R.id.speed_4
        1.5F -> R.id.speed_5
        1.75F -> R.id.speed_6
        2.0F -> R.id.speed_7
        2.25F -> R.id.speed_8
        else -> R.id.speed_9
    }

    @JvmStatic
    fun addToLovedSongs(activity: Activity, song: Music?, launchedBy: String) {
        ListsHelper.addToLovedSongs(
                song,
                0,
                launchedBy
        )
        (activity as MediaControlInterface).onLovedSongAdded(song, true)
        (activity as UIControlInterface).onLovedSongsUpdate(false)
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
