package com.iven.musicplayergo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.startSongFromQueue
import com.iven.musicplayergo.extensions.toFilenameWithoutExtension
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
import java.util.*

class QueueAdapter(
    private val ctx: Context,
    private val queueSongsDialog: MaterialDialog,
    private val mediaPlayerHolder: MediaPlayerHolder
) :
    RecyclerView.Adapter<QueueAdapter.QueueHolder>() {

    private var mQueueSongs = mediaPlayerHolder.queueSongs
    private var mSelectedSong = mediaPlayerHolder.currentSong

    private val mDefaultTextColor =
        ThemeHelper.resolveColorAttr(ctx, android.R.attr.textColorPrimary)

    fun swapSelectedSong(song: Music?) {
        notifyItemChanged(mQueueSongs.indexOf(mSelectedSong.first))
        mSelectedSong = Pair(song, true)
        notifyItemChanged(mQueueSongs.indexOf(mSelectedSong.first))
    }

    fun swapQueueSongs(queueSongs: MutableList<Music>) {
        mQueueSongs = queueSongs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueHolder {
        return QueueHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.music_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = mQueueSongs.size

    override fun onBindViewHolder(holder: QueueHolder, position: Int) {
        holder.bindItems(mQueueSongs[holder.adapterPosition])
    }

    inner class QueueHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(song: Music) {

            itemView.run {

                val title = findViewById<TextView>(R.id.title)
                val duration = findViewById<TextView>(R.id.duration)
                val subtitle = findViewById<TextView>(R.id.subtitle)

                val displayedTitle =
                        if (goPreferences.songsVisualization != GoConstants.TITLE) {
                            song.displayName?.toFilenameWithoutExtension()
                        } else {
                            song.title
                        }

                title.text = displayedTitle

                when {
                    mQueueSongs.indexOf(mSelectedSong.first) == adapterPosition && mSelectedSong.second -> title.setTextColor(
                            ThemeHelper.resolveThemeAccent(ctx)
                    )
                    else -> title.setTextColor(mDefaultTextColor)
                }

                duration.text = DialogHelper.computeDurationText(ctx, song)

                subtitle.text =
                    context.getString(R.string.artist_and_album, song.artist, song.album)

                setOnClickListener {
                    mediaPlayerHolder.startSongFromQueue(song, mediaPlayerHolder.launchedBy)
                }
            }
        }
    }

    fun performQueueSongDeletion(position: Int, title: TextView, isSwipe: Boolean): Boolean {
        val song = mQueueSongs[position]
        return if (title.currentTextColor != ThemeHelper.resolveThemeAccent(ctx)) {
            DialogHelper.showDeleteQueueSongDialog(
                ctx,
                Pair(song, position),
                queueSongsDialog,
                this@QueueAdapter,
                mediaPlayerHolder,
                isSwipe
            )
            true
        } else {
            false
        }
    }

    // https://mobikul.com/drag-and-drop-item-on-recyclerview/
    val itemTouchCallback: ItemTouchHelper.Callback = object : ItemTouchHelper.Callback() {

        override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
        ): Int {
            val dragFlags =
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.END or ItemTouchHelper.START
            return makeMovementFlags(dragFlags, 0)
        }

        override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
        ) = onItemMoved(viewHolder.adapterPosition, target.adapterPosition)

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun isLongPressDragEnabled() = true
    }

    private fun onItemMoved(fromPosition: Int, toPosition: Int) : Boolean {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(mQueueSongs, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(mQueueSongs, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        return true
    }
}
