package com.iven.musicplayergo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.player.MediaPlayerHolder
import kotlinx.android.synthetic.main.song_item.view.*

class SongsAdapter(albumMusic: MutableList<Music>) :
    RecyclerView.Adapter<SongsAdapter.SongsHolder>() {

    var onSongClick: ((Music) -> Unit)? = null

    private var mAlbumMusic = albumMusic

    fun swapSongs(albumMusic: MutableList<Music>) {
        mAlbumMusic = albumMusic
        notifyDataSetChanged()
    }

    fun randomPlaySelectedAlbum(mediaPlayerHolder: MediaPlayerHolder) {
        val currentAlbum = mAlbumMusic
        currentAlbum.shuffle()
        val song = currentAlbum[0]
        mediaPlayerHolder.setCurrentSong(song, mAlbumMusic)
        mediaPlayerHolder.initMediaPlayer(song)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsHolder {
        return SongsHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.song_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mAlbumMusic.size
    }

    override fun onBindViewHolder(holder: SongsHolder, position: Int) {
        val track = mAlbumMusic[holder.adapterPosition].track
        val title = mAlbumMusic[holder.adapterPosition].title
        val duration = mAlbumMusic[holder.adapterPosition].duration

        holder.bindItems(track, title!!, duration)
    }

    inner class SongsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(track: Int, title: String, duration: Long) {
            itemView.track.text = MusicUtils.formatSongTrack(track).toString()
            itemView.title.text = title
            itemView.duration.text = MusicUtils.formatSongDuration(duration)
            itemView.setOnClickListener { onSongClick?.invoke(mAlbumMusic[adapterPosition]) }
        }
    }
}
