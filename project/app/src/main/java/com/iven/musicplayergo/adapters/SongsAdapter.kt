package com.iven.musicplayergo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.uihelpers.UIUtils
import kotlinx.android.synthetic.main.song_item.view.*

class SongsAdapter(private val music: MutableList<Music>) : RecyclerView.Adapter<SongsAdapter.SongsHolder>() {

    var onSongClick: ((Music) -> Unit)? = null

    init {
        music.sortBy { it.track }
    }

    fun randomPlaySelectedAlbum(mediaPlayerHolder: MediaPlayerHolder) {
        val currentAlbum = music
        currentAlbum.shuffle()
        val song = currentAlbum[0]
        mediaPlayerHolder.setCurrentSong(song, music)
        mediaPlayerHolder.initMediaPlayer(song)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsHolder {
        return SongsHolder(LayoutInflater.from(parent.context).inflate(R.layout.song_item, parent, false))
    }

    override fun getItemCount(): Int {
        return music.size
    }

    override fun onBindViewHolder(holder: SongsHolder, position: Int) {
        val track = music[holder.adapterPosition].track
        val title = music[holder.adapterPosition].title
        val duration = music[holder.adapterPosition].duration

        holder.bindItems(track, title, duration)
    }

    inner class SongsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(track: Int, title: String, duration: Long) {
            itemView.track.text = MusicUtils.formatSongTrack(track).toString()
            itemView.title.text = title
            itemView.duration.text = MusicUtils.formatSongDuration(duration)
            itemView.setOnClickListener { onSongClick?.invoke(music[adapterPosition]) }
            UIUtils.setHorizontalScrollBehavior(itemView, itemView.title)
        }
    }
}