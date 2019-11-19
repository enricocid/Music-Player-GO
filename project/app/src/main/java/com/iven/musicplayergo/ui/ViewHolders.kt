package com.iven.musicplayergo.ui

import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.afollestad.recyclical.ViewHolder
import com.iven.musicplayergo.R

class GenericViewHolder(itemView: View) : ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(R.id.title)
    val subtitle: TextView = itemView.findViewById(R.id.subtitle)
}

class AlbumsViewHolder(itemView: View) : ViewHolder(itemView) {
    val albumCard = itemView as CardView
    val album: TextView = itemView.findViewById(R.id.album)
    val year: TextView = itemView.findViewById(R.id.year)
}
