package com.iven.musicplayergo.ui

import android.view.View
import android.widget.TextView
import com.afollestad.recyclical.ViewHolder
import com.iven.musicplayergo.R

class ArtistsViewHolder(itemView: View) : ViewHolder(itemView) {
    val name = itemView as TextView
}

class AllMusicHolder(itemView: View) : ViewHolder(itemView) {
    val songTitle: TextView = itemView.findViewById(R.id.song_title)
    val artistName: TextView = itemView.findViewById(R.id.artist_name)
}
