package com.iven.musicplayergo.helpers

import android.app.Activity
import android.content.Context
import android.text.Spanned
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.text.parseAsHtml
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface


object DialogHelper {

    @JvmStatic
    fun showClearQueueDialog(context: Context, mediaPlayerHolder: MediaPlayerHolder) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.queue)
            .setMessage(R.string.queue_songs_clear)
            .setPositiveButton(R.string.yes) { _, _ ->
                goPreferences.isQueue = null
                goPreferences.queue = null
                with(mediaPlayerHolder) {
                    queueSongs.clear()
                    setQueueEnabled(enabled = false, canSkip = isQueueStarted)
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    @JvmStatic
    fun showClearFavoritesDialog(activity: Activity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.favorites)
            .setMessage(R.string.favorites_clear)
            .setPositiveButton(R.string.yes) { _, _ ->
                (activity as UIControlInterface).onFavoritesUpdated(clear = true)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    @JvmStatic
    fun showPopupForHide(
        activity: Activity,
        itemView: View?,
        stringToFilter: String?
    ) {
        itemView?.let { view ->

            PopupMenu(activity, view).apply {

                inflate(R.menu.popup_filter)

                menu.findItem(R.id.music_container_title).setTitle(activity, stringToFilter)
                menu.enablePopupIcons(activity)
                gravity = Gravity.END

                setOnMenuItemClickListener {
                    (activity as UIControlInterface).onAddToFilter(stringToFilter)
                    return@setOnMenuItemClickListener true
                }
                show()
            }
        }
    }

    @JvmStatic
    fun showPopupForSongs(
        activity: Activity,
        itemView: View?,
        song: Music?,
        launchedBy: String
    ) {
        val mediaControlInterface = activity as MediaControlInterface
        itemView?.let { view ->

            PopupMenu(activity, view).apply {

                inflate(R.menu.popup_songs)

                menu.findItem(R.id.song_title).setTitle(activity, song?.title)
                menu.enablePopupIcons(activity)
                gravity = Gravity.END

                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.favorites_add -> {
                            ListsHelper.addToFavorites(
                                activity,
                                song,
                                canRemove = false,
                                0,
                                launchedBy
                            )
                            (activity as UIControlInterface).onFavoriteAddedOrRemoved()
                        }
                        else -> mediaControlInterface.onAddToQueue(song)
                    }
                    return@setOnMenuItemClickListener true
                }
                show()
            }
        }
    }

    @JvmStatic
    fun showPopupForPlaybackSpeed(
        activity: Activity,
        view: View
    ) {

        PopupMenu(activity, view).apply {
            inflate(R.menu.popup_speed)
            gravity = Gravity.END

            if (goPreferences.playbackSpeedMode != GoConstants.PLAYBACK_SPEED_ONE_ONLY) {
                menu.findItem(getSelectedPlaybackItem(goPreferences.latestPlaybackSpeed)).setTitleColor(ThemeHelper.resolveThemeAccent(activity))
            }

            setOnMenuItemClickListener { menuItem ->
                val playbackSpeed = when (menuItem.itemId) {
                    R.id.speed_0 -> 0.25F
                    R.id.speed_1 -> 0.5F
                    R.id.speed_2 -> 0.75F
                    R.id.speed_3 -> 1.0F
                    R.id.speed_4 -> 1.25F
                    R.id.speed_5 -> 1.5F
                    R.id.speed_6 -> 1.75F
                    R.id.speed_7 -> 2.0F
                    R.id.speed_8 -> 2.5F
                    else -> 2.5F
                }
                if (goPreferences.playbackSpeedMode != GoConstants.PLAYBACK_SPEED_ONE_ONLY) {
                    menu.findItem(getSelectedPlaybackItem(playbackSpeed)).setTitleColor(ThemeHelper.resolveThemeAccent(activity))
                }
                (activity as MediaControlInterface).onGetMediaPlayerHolder()?.setPlaybackSpeed(playbackSpeed)
                return@setOnMenuItemClickListener true
            }
            show()
        }
    }

    private fun getSelectedPlaybackItem(playbackSpeed: Float) = when (playbackSpeed) {
        0.25F -> R.id.speed_0
        0.5F -> R.id.speed_1
        0.75F -> R.id.speed_2
        1.0F -> R.id.speed_3
        1.25F -> R.id.speed_4
        1.5F -> R.id.speed_5
        1.75F -> R.id.speed_6
        2.0F -> R.id.speed_7
        2.25F -> R.id.speed_8
        else -> R.id.speed_9
    }

    @JvmStatic
    fun stopPlaybackDialog(context: Context, mediaPlayerHolder: MediaPlayerHolder) {
        MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setTitle(R.string.app_name)
            .setMessage(R.string.on_close_activity)
            .setPositiveButton(R.string.yes) { _, _ ->
                mediaPlayerHolder.stopPlaybackService(stopPlayback = true)
            }
            .setNegativeButton(R.string.no) { _, _ ->
                mediaPlayerHolder.stopPlaybackService(stopPlayback = false)
            }
            .show()
    }

    @JvmStatic
    fun notifyForegroundServiceStopped(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setTitle(R.string.app_name)
            .setMessage(R.string.error_fs_not_allowed)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    @JvmStatic
    fun computeDurationText(ctx: Context, favorite: Music?): Spanned? {
        if (favorite?.startFrom != null && favorite.startFrom > 0L) {
            return ctx.getString(
                R.string.favorite_subtitle,
                favorite.startFrom.toLong().toFormattedDuration(
                    isAlbum = false,
                    isSeekBar = false
                ),
                favorite.duration.toFormattedDuration(isAlbum = false, isSeekBar = false)
            ).parseAsHtml()
        }
        return favorite?.duration?.toFormattedDuration(isAlbum = false, isSeekBar = false)
            ?.parseAsHtml()
    }
}
