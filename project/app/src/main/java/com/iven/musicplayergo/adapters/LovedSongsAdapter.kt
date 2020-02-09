package com.iven.musicplayergo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.musicloadutils.SavedMusic
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.toFormattedDuration
import com.iven.musicplayergo.utils.MusicUtils
import com.iven.musicplayergo.utils.ThemeHelper
import com.iven.musicplayergo.utils.UIControlInterface
import com.iven.musicplayergo.utils.Utils

class LovedSongsAdapter(
    private val context: Context,
    private val lovedSongsDialog: MaterialDialog,
    private val uiControlInterface: UIControlInterface,
    private val mediaPlayerHolder: MediaPlayerHolder
) :
    RecyclerView.Adapter<LovedSongsAdapter.LoveHolder>() {

    private var mLovedSongs = goPreferences.lovedSongs?.toMutableList()

    fun swapSongs(lovedSongs: MutableList<SavedMusic>?) {
        mLovedSongs = lovedSongs
        notifyDataSetChanged()
        uiControlInterface.onLovedSongsUpdate(false)
        if (mLovedSongs?.isEmpty()!!) lovedSongsDialog.dismiss()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoveHolder {
        return LoveHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.song_item_alt,
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
            duration.text = ThemeHelper.buildSpanned(
                context.getString(
                    R.string.loved_song_subtitle,
                    lovedSong?.startFrom?.toLong()?.toFormattedDuration(false),
                    lovedSong?.duration?.toFormattedDuration(false)
                )
            )
            subtitle.text =
                context.getString(R.string.artist_and_album, lovedSong?.artist, lovedSong?.album)

            itemView.apply {
                setOnClickListener {
                    mediaPlayerHolder.isSongFromLovedSongs = Pair(true, lovedSong?.startFrom!!)
                    MusicUtils.getSongForRestore(lovedSong)?.let { songToPlay ->
                        uiControlInterface.onSongSelected(
                            songToPlay,
                            MusicUtils.getAlbumSongs(
                                songToPlay.artist,
                                songToPlay.album
                            ),
                            lovedSong.isFromFolder
                        )
                    }
                }
                setOnLongClickListener {
                    Utils.showDeleteLovedSongDialog(context, lovedSong, this@LovedSongsAdapter)
                    return@setOnLongClickListener true
                }
            }
        }
    }
}
