package com.iven.musicplayergo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.iven.musicplayergo.R
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.Utils

class QueueAdapter(
    private val context: Context,
    private val queueSongsDialog: MaterialDialog,
    private val mediaPlayerHolder: MediaPlayerHolder
) :
    RecyclerView.Adapter<QueueAdapter.QueueHolder>() {

    private var mQueueSongs = mediaPlayerHolder.queueSongs
    private var mCurrentQueueIndex = mediaPlayerHolder.currentQueueIndex

    fun swapIndex(currentQueueIndex: Int) {
        notifyItemChanged(mCurrentQueueIndex)
        notifyItemChanged(currentQueueIndex)
        mCurrentQueueIndex = currentQueueIndex
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueHolder {
        return QueueHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.generic_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mQueueSongs.size
    }

    override fun onBindViewHolder(holder: QueueHolder, position: Int) {
        holder.bindItems(mQueueSongs[holder.adapterPosition])
    }

    inner class QueueHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(song: Music) {

            val title = itemView.findViewById<TextView>(R.id.title)
            val subtitle = itemView.findViewById<TextView>(R.id.subtitle)

            title.text = song.title
            subtitle.text =
                context.getString(R.string.artist_and_album, song.artist, song.album)

            val defaultTextColor = title.currentTextColor
            val themeAccent = ThemeHelper.resolveThemeAccent(context)

            if (mCurrentQueueIndex != adapterPosition) title.setTextColor(defaultTextColor) else title.setTextColor(
                themeAccent
            )

            itemView.setOnClickListener {
                mediaPlayerHolder.setCurrentSong(song, mediaPlayerHolder.queueSongs)
                mediaPlayerHolder.currentQueueIndex = adapterPosition
                if (mediaPlayerHolder.isSongRestoredFromPrefs) mediaPlayerHolder.isPlay = true
                mediaPlayerHolder.initMediaPlayer(song)
            }

            itemView.setOnLongClickListener {
                if (title.currentTextColor != ThemeHelper.resolveThemeAccent(context))
                    Utils.showDeleteQueueSongDialog(
                        context,
                        Pair(song, adapterPosition),
                        queueSongsDialog,
                        this@QueueAdapter,
                        mediaPlayerHolder
                    )

                return@setOnLongClickListener true
            }
        }
    }
}
