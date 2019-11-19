package com.iven.musicplayergo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.music.Album
import com.iven.musicplayergo.ui.Utils
import kotlinx.android.synthetic.main.album_item.view.*

class AlbumsAdapter(
    private val albums: List<Album>,
    private val accent: Int,
    private val isThemeInverted: Boolean
) :
    RecyclerView.Adapter<AlbumsAdapter.AlbumsHolder>() {

    var onAlbumClick: ((String?) -> Unit)? = null

    private var mSelectedAlbum: String? = null
    private var mSelectedPosition = 0

    init {
        mSelectedAlbum = albums[0].title
    }

    fun swapSelectedAlbum(newSelectedPosition: Int) {
        mSelectedPosition = newSelectedPosition
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumsHolder {

        val albumView =
            LayoutInflater.from(parent.context).inflate(R.layout.album_item, parent, false)
        val materialCardView = albumView as CardView
        val backgroundColor =
            if (isThemeInverted) Utils.darkenColor(accent, 0.85F) else Utils.lightenColor(
                accent,
                0.80F
            )
        materialCardView.setCardBackgroundColor(backgroundColor)

        return AlbumsHolder(albumView)
    }

    override fun getItemCount(): Int {
        return albums.size
    }

    override fun onBindViewHolder(holder: AlbumsHolder, position: Int) {
        val title = albums[holder.adapterPosition].title
        val year = albums[holder.adapterPosition].year
        holder.bindItems(title, year)
    }

    inner class AlbumsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(title: String?, year: String?) {

            val albumCard = itemView as CardView

            /*         if (mSelectedPosition != adapterPosition) albumCard.strokeColor =
                         Color.TRANSPARENT else albumCard.strokeColor = accent*/

            itemView.album.text = title
            itemView.year.text = year
            itemView.setOnClickListener {
                if (adapterPosition != mSelectedPosition) {
                    notifyItemChanged(mSelectedPosition)
                    mSelectedPosition = adapterPosition
                    //   albumCard.strokeColor = accent
                    mSelectedAlbum = title
                    onAlbumClick?.invoke(title)
                }
            }
        }
    }
}
