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

class AlbumsViewHolder(itemView: View) : ViewHolder(itemView) {
    val album: TextView = itemView.findViewById(R.id.album)
    val year: TextView = itemView.findViewById(R.id.year)
    val checkbox: ImageView = itemView.findViewById(R.id.checkbox)
}
