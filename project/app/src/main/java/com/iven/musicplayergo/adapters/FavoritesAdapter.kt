package com.iven.musicplayergo.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.enablePopupIcons
import com.iven.musicplayergo.extensions.setTitle
import com.iven.musicplayergo.extensions.toFilenameWithoutExtension
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface


class FavoritesAdapter(
    private val activity: Activity,
    private val FavoritesDialog: MaterialDialog
) :
    RecyclerView.Adapter<FavoritesAdapter.FavoritesHolder>() {

    private var mFavorites = goPreferences.favorites?.toMutableList()
    private val mUiControlInterface = activity as UIControlInterface
    private val mMediaControlInterface = activity as MediaControlInterface

    @SuppressLint("NotifyDataSetChanged")
    fun swapSongs(favorites: MutableList<Music>?) {
        mFavorites = favorites
        notifyDataSetChanged()
        mUiControlInterface.onFavoritesUpdated(clear = false)
        if (mFavorites?.isNullOrEmpty()!!) {
            FavoritesDialog.dismiss()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesHolder {
        return FavoritesHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.music_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mFavorites?.size!!
    }

    override fun onBindViewHolder(holder: FavoritesHolder, position: Int) {
        holder.bindItems(mFavorites?.get(holder.absoluteAdapterPosition))
    }


    inner class FavoritesHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(favorite: Music?) {

            val title = itemView.findViewById<TextView>(R.id.title)
            val duration = itemView.findViewById<TextView>(R.id.duration)
            val subtitle = itemView.findViewById<TextView>(R.id.subtitle)

            val displayedTitle =
                if (goPreferences.songsVisualization != GoConstants.TITLE) {
                    favorite?.displayName?.toFilenameWithoutExtension()
                } else {
                    favorite?.title
                }
            title.text = displayedTitle
            duration.text =
                DialogHelper.computeDurationText(activity, favorite)

            subtitle.text =
                activity.getString(R.string.artist_and_album, favorite?.artist, favorite?.album)

            with(itemView) {
                setOnClickListener {
                    mMediaControlInterface.onAddAlbumToQueue(
                        favorite,
                        mFavorites,
                        favorite?.launchedBy!!,
                        forcePlay = true
                    )
                }
                setOnLongClickListener {
                    showPopupForFavoriteSongs(absoluteAdapterPosition, this)
                    return@setOnLongClickListener true
                }
            }
        }
    }

    private fun showPopupForFavoriteSongs(
        adapterPosition: Int,
        itemView: View?
    ) {
        mFavorites?.get(adapterPosition)?.let { song ->
            itemView?.let { view ->

                PopupMenu(activity, view).apply {

                    inflate(R.menu.popup_favorites_songs)

                    menu.findItem(R.id.song_title).setTitle(activity, song.title)
                    menu.enablePopupIcons(activity)
                    gravity = Gravity.END

                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.favorite_delete -> performFavoriteDeletion(adapterPosition, isSwipe = false)
                            else -> mMediaControlInterface.onAddToQueue(song, song.launchedBy)
                        }
                        return@setOnMenuItemClickListener true
                    }
                    show()
                }
            }
        }
    }

    fun addFavoriteToQueue(adapterPosition: Int) {
        mFavorites?.get(adapterPosition)?.let { song ->
            mMediaControlInterface.onAddToQueue(
                song,
                song.launchedBy
            )
        }
    }

    fun performFavoriteDeletion(position: Int, isSwipe: Boolean) {
        mFavorites?.get(position).let { song ->
            DialogHelper.showDeleteFavoriteDialog(
                activity,
                song,
                this@FavoritesAdapter,
                Pair(first = isSwipe, second = position)
            )
        }
    }
}
