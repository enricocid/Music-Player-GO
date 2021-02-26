package com.iven.musicplayergo.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.afollestad.recyclical.ViewHolder
import com.iven.musicplayergo.R

class GenericViewHolder(itemView: View) : ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(R.id.title)
    val subtitle: TextView = itemView.findViewById(R.id.subtitle)
}

class ContainersAlbumViewHolder(itemView: View) : ViewHolder(itemView) {
    val albumCover: ImageView = itemView.findViewById(R.id.album_cover)
    val title: TextView = itemView.findViewById(R.id.title)
    val subtitle: TextView = itemView.findViewById(R.id.subtitle)
}

class DetailsAlbumsViewHolder(itemView: View) : ViewHolder(itemView) {
    val albumCover: ImageView = itemView.findViewById(R.id.image)
    val album: TextView = itemView.findViewById(R.id.album)
    val year: TextView = itemView.findViewById(R.id.year)
    val totalDuration: TextView = itemView.findViewById(R.id.total_duration)
}

class SongsViewHolder(itemView: View) : ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(R.id.title)
    val duration: TextView = itemView.findViewById(R.id.duration)
    val subtitle: TextView = itemView.findViewById(R.id.subtitle)
}
