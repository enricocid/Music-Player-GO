package com.iven.musicplayergo.dialogs


import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.MusicItemBinding
import com.iven.musicplayergo.extensions.enablePopupIcons
import com.iven.musicplayergo.extensions.setTitle
import com.iven.musicplayergo.extensions.toFormattedDuration
import com.iven.musicplayergo.extensions.toName
import com.iven.musicplayergo.models.Music


class FavoritesAdapter : RecyclerView.Adapter<FavoritesAdapter.FavoritesHolder>() {

    var onFavoritesCleared: (() -> Unit)? = null
    var onFavoritesUpdate: (() -> Unit)? = null

    var onFavoriteQueued: ((song: Music?) -> Unit)? = null
    var onFavoritesQueued: ((songs: List<Music>?, forcePlay: Pair<Boolean, Music?>) -> Unit)? = null

    // favorites
    private var mFavorites = GoPreferences.getPrefsInstance().favorites?.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesHolder {
        val binding = MusicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavoritesHolder(binding)
    }

    override fun getItemCount(): Int {
        return mFavorites?.size!!
    }

    override fun onBindViewHolder(holder: FavoritesHolder, position: Int) {
        holder.bindItems(mFavorites?.get(holder.absoluteAdapterPosition))
    }

    inner class FavoritesHolder(private val binding: MusicItemBinding): RecyclerView.ViewHolder(binding.root) {

        fun bindItems(favorite: Music?) {

            val displayedTitle = favorite?.toName()

            with(binding) {
                val context = root.context
                title.text = displayedTitle
                duration.text = Dialogs.computeDurationText(context, favorite)
                subtitle.text =
                    context.getString(R.string.artist_and_album, favorite?.artist, favorite?.album)

                root.setOnClickListener {
                    onFavoritesQueued?.invoke(
                        mFavorites,
                        Pair(first = true, second = mFavorites?.get(absoluteAdapterPosition))
                    )
                }
                root.setOnLongClickListener {
                    showPopupForFavoriteSongs(absoluteAdapterPosition, root)
                    return@setOnLongClickListener true
                }
            }
        }
    }

    private fun showPopupForFavoriteSongs(adapterPosition: Int, itemView: View?) {
        mFavorites?.get(adapterPosition)?.run {

            itemView?.let { view ->

                PopupMenu(view.context, view).apply {

                    val resources = view.resources
                    inflate(R.menu.popup_favorites_songs)

                    menu.findItem(R.id.song_title).setTitle(resources, title)
                    menu.enablePopupIcons(resources)
                    gravity = Gravity.END

                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.favorite_delete -> performFavoriteDeletion(view.context, adapterPosition)
                            else -> onFavoriteQueued?.invoke(this@run)
                        }
                        return@setOnMenuItemClickListener true
                    }
                    show()
                }
            }
        }
    }

    fun performFavoriteDeletion(context: Context, position: Int) {

        fun deleteSong(song: Music) {
            mFavorites?.run {
                // remove item
                remove(song)
                notifyItemRemoved(position)
                // update
                GoPreferences.getPrefsInstance().favorites = mFavorites
                if (mFavorites.isNullOrEmpty()) {
                    onFavoritesCleared?.invoke()
                }
                onFavoritesUpdate?.invoke()
            }
        }

        mFavorites?.get(position)?.let { song ->
            if (GoPreferences.getPrefsInstance().isAskForRemoval) {

                var msg = context.getString(
                    R.string.favorite_remove,
                    song.title,
                    song.startFrom.toLong().toFormattedDuration(
                        isAlbum = false,
                        isSeekBar = false
                    )
                )
                if (song.startFrom == 0) {
                    msg = msg.replace(context.getString(R.string.favorites_no_position), "")
                }

                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.favorites)
                    .setMessage(msg)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        deleteSong(song)
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
                return
            }
            deleteSong(song)
        }
    }
}
