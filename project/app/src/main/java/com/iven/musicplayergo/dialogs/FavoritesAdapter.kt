package com.iven.musicplayergo.dialogs


import android.app.Activity
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
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface


class FavoritesAdapter(private val activity: Activity) : RecyclerView.Adapter<FavoritesAdapter.FavoritesHolder>() {

    // interfaces
    private val mMediaControlInterface = activity as MediaControlInterface
    private val mUIControlInterface = activity as UIControlInterface

    var onFavoritesCleared: (() -> Unit)? = null

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

    inner class FavoritesHolder(private val binding: MusicItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindItems(favorite: Music?) {

            val displayedTitle = favorite?.toName()

            with(binding) {
                title.text = displayedTitle
                duration.text = Dialogs.computeDurationText(activity, favorite)
                subtitle.text =
                    activity.getString(R.string.artist_and_album, favorite?.artist, favorite?.album)

                root.setOnClickListener {
                    mMediaControlInterface.onAddAlbumToQueue(
                        mFavorites,
                        forcePlay = Pair(first = true, second = mFavorites?.get(absoluteAdapterPosition))
                    )
                }
                root.setOnLongClickListener {
                    showPopupForFavoriteSongs(absoluteAdapterPosition, root)
                    return@setOnLongClickListener true
                }
            }
        }
    }

    private fun showPopupForFavoriteSongs(
        adapterPosition: Int,
        itemView: View?
    ) {
        mFavorites?.get(adapterPosition)?.run {
            itemView?.let { view ->

                PopupMenu(activity, view).apply {

                    inflate(R.menu.popup_favorites_songs)

                    menu.findItem(R.id.song_title).setTitle(activity.resources, title)
                    menu.enablePopupIcons(activity)
                    gravity = Gravity.END

                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.favorite_delete -> performFavoriteDeletion(adapterPosition)
                            else -> mMediaControlInterface.onAddToQueue(this@run)
                        }
                        return@setOnMenuItemClickListener true
                    }
                    show()
                }
            }
        }
    }

    fun performFavoriteDeletion(position: Int) {

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
                mUIControlInterface.onFavoritesUpdated(clear = false)
                mUIControlInterface.onFavoriteAddedOrRemoved()
            }
        }

        mFavorites?.get(position)?.let { song ->
            if (GoPreferences.getPrefsInstance().isAskForRemoval) {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.favorites)
                    .setMessage(activity.getString(
                        R.string.favorite_remove,
                        song.title,
                        song.startFrom.toLong().toFormattedDuration(
                            isAlbum = false,
                            isSeekBar = false)))
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
