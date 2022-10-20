package com.iven.musicplayergo.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.QueueItemBinding
import com.iven.musicplayergo.extensions.startSongFromQueue
import com.iven.musicplayergo.extensions.toName
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.utils.Theming


class QueueAdapter(private val ctx: Context, private val mediaPlayerHolder: MediaPlayerHolder) : RecyclerView.Adapter<QueueAdapter.QueueHolder>() {

    var queueSongs = mediaPlayerHolder.queueSongs
    private var mSelectedSong = mediaPlayerHolder.currentSong

    var onQueueCleared: (() -> Unit)? = null

    private val mDefaultTextColor =
        Theming.resolveColorAttr(ctx, android.R.attr.textColorPrimary)

    fun swapSelectedSong(song: Music?) {
        notifyItemChanged(queueSongs.indexOf(mSelectedSong))
        mSelectedSong = song
        notifyItemChanged(queueSongs.indexOf(mSelectedSong))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueHolder {
        val binding = QueueItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QueueHolder(binding)
    }

    override fun getItemCount() = queueSongs.size

    override fun onBindViewHolder(holder: QueueHolder, position: Int) {
        holder.bindItems(queueSongs[holder.absoluteAdapterPosition])
    }

    inner class QueueHolder(private val binding: QueueItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindItems(song: Music) {

            with(binding) {

                val displayedTitle = song.toName()

                title.text = displayedTitle
                duration.text = Dialogs.computeDurationText(ctx, song)
                subtitle.text =
                    ctx.getString(R.string.artist_and_album, song.artist, song.album)

                title.setTextColor(if (mediaPlayerHolder.isQueue != null && mediaPlayerHolder.isQueueStarted && queueSongs.indexOf(mSelectedSong) == absoluteAdapterPosition) {
                    Theming.resolveThemeColor(ctx.resources)
                } else {
                    mDefaultTextColor
                })

                root.setOnClickListener {
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
        if (GoPreferences.getPrefsInstance().isAskForRemoval) {
            notifyItemChanged(adapterPosition)
            return if (song != mSelectedSong || mediaPlayerHolder.isQueue == null) {
                MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.queue)
                    .setMessage(ctx.getString(
                        R.string.queue_song_remove,
                        song.title
                    ))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        with(mediaPlayerHolder) {
                            // remove and update adapter
                            queueSongs.remove(song)
                            notifyItemRemoved(adapterPosition)

                            // dismiss sheet if empty
                            if (queueSongs.isEmpty()) {
                                isQueue = null
                                mediaPlayerInterface.onQueueStartedOrEnded(started = false)
                                onQueueCleared?.invoke()
                            }

                            // update queue songs
                            GoPreferences.getPrefsInstance().queue = queueSongs
                        }
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
                true
            } else {
                false
            }
        } else {
            return if (song != mSelectedSong || mediaPlayerHolder.isQueue == null) {
                queueSongs.remove(song)
                notifyItemRemoved(adapterPosition)
                true
            } else {
                false
            }
        }
    }
}
