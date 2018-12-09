package com.iven.musicplayergo.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.uihelpers.UIUtils
import kotlinx.android.synthetic.main.artist_item.view.*

class ArtistsAdapter(
    private val resources: Resources,
    artists: MutableList<String>,
    private val music: Map<String, Map<String, List<Music>>>
) :
    RecyclerView.Adapter<ArtistsAdapter.ArtistsHolder>() {

    var onArtistClick: ((String) -> Unit)? = null
    private var mArtists = artists

    private var mLetters = LinkedHashMap<String, Int>()

    init {
        mArtists.sort()
        generateLetters()
    }

    private fun generateLetters() {
        try {
            mArtists.forEachIndexed { index, artist ->
                if (!mLetters.containsKey(artist.substring(0, 1))) {
                    mLetters[artist.substring(0, 1)] = index
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLetterPosition(letter: String): Int {
        if (mLetters.containsKey(letter)) {
            return mLetters[letter]!!
        }
        return -1
    }

    fun getLetters(): Array<String> {
        return mLetters.keys.toTypedArray()
    }

    fun setQueryResults(artists: MutableList<String>) {
        mArtists = artists
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistsHolder {
        return ArtistsHolder(LayoutInflater.from(parent.context).inflate(R.layout.artist_item, parent, false))
    }

    override fun getItemCount(): Int {
        return mArtists.size
    }

    override fun onBindViewHolder(holder: ArtistsHolder, position: Int) {
        val artist = mArtists[holder.adapterPosition]
        val albums = music[artist]!!
        holder.bindItems(mArtists[holder.adapterPosition], albums.keys.size, MusicUtils.getArtistSongsCount(albums))
    }

    inner class ArtistsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(title: String, albumCount: Int, songCount: Int) {
            itemView.artist.text = title
            itemView.album_count.text =
                    MusicUtils.buildSpanned(resources.getString(R.string.artist_info, albumCount, songCount))
            itemView.setOnClickListener { onArtistClick?.invoke(mArtists[adapterPosition]) }
            UIUtils.setHorizontalScrollBehavior(itemView, itemView.artist)
        }
    }
}