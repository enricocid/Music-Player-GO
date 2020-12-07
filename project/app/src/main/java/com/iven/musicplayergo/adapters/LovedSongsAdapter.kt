package com.iven.musicplayergo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toFormattedDuration
import com.iven.musicplayergo.extensions.toSpanned
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.UIControlInterface

class LovedSongsAdapter(
    private val ctx: Context,
    private val lovedSongsDialog: MaterialDialog,
    private val mediaPlayerHolder: MediaPlayerHolder,
    private val uiControlInterface: UIControlInterface
) :
    RecyclerView.Adapter<LovedSongsAdapter.LoveHolder>() {

    private var mLovedSongs = goPreferences.lovedSongs?.toMutableList()

    fun swapSongs(lovedSongs: MutableList<Music>?) {
        mLovedSongs = lovedSongs
        notifyDataSetChanged()
        uiControlInterface.onLovedSongsUpdate(false)
        if (mLovedSongs?.isEmpty()!!) {
            lovedSongsDialog.dismiss()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoveHolder {
        return LoveHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.music_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mLovedSongs?.size!!
    }

    override fun onBindViewHolder(holder: LoveHolder, position: Int) {
        holder.bindItems(mLovedSongs?.get(holder.adapterPosition))
    }

    inner class LoveHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(lovedSong: Music?) {

            val title = itemView.findViewById<TextView>(R.id.title)
            val duration = itemView.findViewById<TextView>(R.id.duration)
            val subtitle = itemView.findViewById<TextView>(R.id.subtitle)

            title.text = lovedSong?.title
            duration.text = ctx.getString(
                R.string.loved_song_subtitle,
                lovedSong?.startFrom?.toLong()?.toFormattedDuration(
                    isAlbum = false,
                    isSeekBar = false
                ),
                lovedSong?.duration?.toFormattedDuration(isAlbum = false, isSeekBar = false)
            ).toSpanned()
            subtitle.text =
                ctx.getString(R.string.artist_and_album, lovedSong?.artist, lovedSong?.album)

            itemView.run {
                setOnClickListener {
                    mediaPlayerHolder.isSongFromLovedSongs =
                        Pair(true, lovedSong?.startFrom!!)
                    uiControlInterface.onAddAlbumToQueue(
                        mLovedSongs,
                        Pair(false, lovedSong),
                        isLovedSongs = true,
                        isShuffleMode = false,
                        mediaPlayerHolder.launchedBy
                    )
                }
                setOnLongClickListener {
                    performLovedSongDeletion(adapterPosition, false)
                    return@setOnLongClickListener true
                }
            }
        }
    }

    fun performLovedSongDeletion(position: Int, isSwipe: Boolean) {
        mLovedSongs?.get(position).let { song ->
            DialogHelper.showDeleteLovedSongDialog(
                ctx,
                song,
                this@LovedSongsAdapter,
                uiControlInterface,
                Pair(isSwipe, position)
            )
        }
    }
}
