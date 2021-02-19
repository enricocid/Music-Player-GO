package com.iven.musicplayergo.adapters

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
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.UIControlInterface


class LovedSongsAdapter(
    private val activity: Activity,
    private val lovedSongsDialog: MaterialDialog,
    private val mediaPlayerHolder: MediaPlayerHolder
) :
    RecyclerView.Adapter<LovedSongsAdapter.LoveHolder>() {

    private var mLovedSongs = goPreferences.lovedSongs?.toMutableList()
    private val mUiControlInterface = activity as UIControlInterface
    private val mMediaControlInterface = activity as MediaControlInterface

    fun swapSongs(lovedSongs: MutableList<Music>?) {
        mLovedSongs = lovedSongs
        notifyDataSetChanged()
        mUiControlInterface.onLovedSongsUpdate(false)
        if (mLovedSongs?.isEmpty()!!) {
            lovedSongsDialog.dismiss()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoveHolder {
        return LoveHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.music_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mLovedSongs?.size!!
    }

    override fun onBindViewHolder(holder: LoveHolder, position: Int) {
        holder.bindItems(mLovedSongs?.get(holder.adapterPosition))
    }


    inner class LoveHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(lovedSong: Music?) {

            val title = itemView.findViewById<TextView>(R.id.title)
            val duration = itemView.findViewById<TextView>(R.id.duration)
            val subtitle = itemView.findViewById<TextView>(R.id.subtitle)

            val displayedTitle =
                    if (goPreferences.songsVisualization != GoConstants.TITLE) {
                        lovedSong?.displayName?.toFilenameWithoutExtension()
                    } else {
                        lovedSong?.title
                    }
            title.text = displayedTitle
            duration.text =
                    DialogHelper.computeDurationText(activity, lovedSong)


            subtitle.text =
                activity.getString(R.string.artist_and_album, lovedSong?.artist, lovedSong?.album)

            with(itemView) {
                setOnClickListener {
                    mediaPlayerHolder.isSongFromLovedSongs =
                        Pair(true, lovedSong?.startFrom!!)
                    (activity as MediaControlInterface).onAddAlbumToQueue(
                        mLovedSongs,
                        Pair(false, lovedSong),
                        isLovedSongs = true,
                        isShuffleMode = false,
                        clearShuffleMode = true,
                        mediaPlayerHolder.launchedBy
                    )
                }
                setOnLongClickListener {
                    showPopupForFavoriteSongs(adapterPosition, this)
                    return@setOnLongClickListener true
                }
            }
        }
    }

    private fun showPopupForFavoriteSongs(
            adapterPosition: Int,
            itemView: View?
    ) {
        mLovedSongs?.get(adapterPosition)?.let { song ->
            itemView?.let { view ->

                PopupMenu(activity, view).apply {

                    inflate(R.menu.popup_favorites_songs)

                    menu.findItem(R.id.song_title).setTitle(activity, song.title)
                    menu.enablePopupIcons(activity)
                    gravity = Gravity.END

                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.loved_songs_delete -> performLovedSongDeletion(adapterPosition, false)
                            else -> mMediaControlInterface.onAddToQueue(song, song.launchedBy)
                        }
                        return@setOnMenuItemClickListener true
                    }
                    show()
                }
            }
        }
    }

    fun addLovedSongToQueue(adapterPosition: Int) {
        mLovedSongs?.get(adapterPosition)?.let { song ->
            mMediaControlInterface.onAddToQueue(
                    song,
                    song.launchedBy
            )
        }
    }

    fun performLovedSongDeletion(position: Int, isSwipe: Boolean) {
        mLovedSongs?.get(position).let { song ->
            DialogHelper.showDeleteLovedSongDialog(
                activity,
                song,
                this@LovedSongsAdapter,
                Pair(isSwipe, position)
            )
        }
    }
}
