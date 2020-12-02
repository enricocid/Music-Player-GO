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
import com.iven.musicplayergo.helpers.MusicOrgHelper
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.models.SavedMusic
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.UIControlInterface

class LovedSongsAdapter(
        private val ctx: Context,
        private val lovedSongsDialog: MaterialDialog,
        private val mediaPlayerHolder: MediaPlayerHolder,
        private val uiControlInterface: UIControlInterface,
        private val deviceSongs: MutableList<Music>
) :
        RecyclerView.Adapter<LovedSongsAdapter.LoveHolder>() {

    private var mConvertedSongs = mutableListOf<Music>()

    private var mLovedSongs = goPreferences.lovedSongs?.toMutableList()?.apply {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            mConvertedSongs.add(MusicOrgHelper.getSongForRestore(item, deviceSongs))
        }
    }

    fun swapSongs(lovedSongs: MutableList<SavedMusic>?) {
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

        fun bindItems(lovedSong: SavedMusic?) {

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

            itemView.apply {
                setOnClickListener {

                    mediaPlayerHolder.isSongFromLovedSongs =
                            Pair(true, lovedSong?.startFrom!!)
                    uiControlInterface.onAddAlbumToQueue(ctx.getString(R.string.queue_song_add, ctx.getString(R.string.loved_songs)), mConvertedSongs, Pair(false, MusicOrgHelper.getSongForRestore(lovedSong, deviceSongs)))
                }
                setOnLongClickListener {
                    performLovedSongDeletion(adapterPosition, deviceSongs, false)
                    return@setOnLongClickListener true
                }
            }
        }
    }

    fun performLovedSongDeletion(position: Int, deviceSongs: MutableList<Music>, isSwipe: Boolean) {
        mLovedSongs?.get(position).let { song ->
            DialogHelper.showDeleteLovedSongDialog(
                    ctx,
                    song,
                    this@LovedSongsAdapter,
                    uiControlInterface,
                    deviceSongs,
                    Pair(isSwipe, position)
            )
        }
    }
}
