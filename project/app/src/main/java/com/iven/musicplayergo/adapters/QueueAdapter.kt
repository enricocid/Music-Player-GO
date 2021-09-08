package com.iven.musicplayergo.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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


class QueueAdapter(
    private val ctx: Context,
    private val queueSongsDialog: MaterialDialog,
    private val mediaPlayerHolder: MediaPlayerHolder
) :
    RecyclerView.Adapter<QueueAdapter.QueueHolder>() {

    var queueSongs = mediaPlayerHolder.queueSongs
    private var mSelectedSong = mediaPlayerHolder.currentSong

    private val mDefaultTextColor =
        ThemeHelper.resolveColorAttr(ctx, android.R.attr.textColorPrimary)

    fun swapSelectedSong(song: Music?) {
        notifyItemChanged(queueSongs.indexOf(mSelectedSong))
        mSelectedSong = song
        notifyItemChanged(queueSongs.indexOf(mSelectedSong))
    }

    @SuppressLint("NotifyDataSetChanged")
    fun swapQueueSongs(updatedQueueSongs: MutableList<Music>) {
        queueSongs = updatedQueueSongs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = QueueHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.queue_item,
            parent,
            false
        )
    )

    override fun getItemCount() = queueSongs.size

    override fun onBindViewHolder(holder: QueueHolder, position: Int) {
        holder.bindItems(queueSongs[holder.absoluteAdapterPosition])
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

                title.setTextColor(if (mediaPlayerHolder.isQueue != null && mediaPlayerHolder.isQueueStarted && queueSongs.indexOf(mSelectedSong) == absoluteAdapterPosition) {
                    ThemeHelper.resolveThemeAccent(ctx)
                } else {
                    mDefaultTextColor
                })

                duration.text = DialogHelper.computeDurationText(ctx, song)

                subtitle.text =
                    context.getString(R.string.artist_and_album, song.artist, song.album)

                setOnClickListener {
                    with(mediaPlayerHolder) {
                        if (isQueue == null) {
                            isQueue = currentSong
                        }
                        startSongFromQueue(song)
                    }
                }
            }
        }
    }

    fun performQueueSongDeletion(adapterPosition: Int): Boolean {
        val song = queueSongs[adapterPosition]
        notifyItemChanged(adapterPosition)
        return if (mediaPlayerHolder.isQueue != null && mediaPlayerHolder.isQueueStarted && song != mSelectedSong) {
            MaterialDialog(ctx).show {

                title(R.string.queue)

                message(
                    text = ctx.getString(
                        R.string.queue_song_remove,
                        song.title
                    )
                )
                positiveButton(R.string.yes) {

                    mediaPlayerHolder.run {
                        queueSongs.remove(song)
                        swapQueueSongs(queueSongs)

                        if (queueSongs.isEmpty()) {
                            isQueue = null
                            mediaPlayerInterface.onQueueStartedOrEnded(started = false)
                            queueSongsDialog.dismiss()
                        }
                    }
                }
                negativeButton(R.string.no)
            }
            true
        } else {
            false
        }
    }
}
