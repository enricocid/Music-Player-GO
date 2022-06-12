package com.iven.musicplayergo.dialogs

import android.app.Activity
import android.content.Context
import android.text.Spanned
import androidx.core.text.parseAsHtml
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toFormattedDuration
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.UIControlInterface


object Dialogs {

    @JvmStatic
    fun showClearQueueDialog(context: Context, mediaPlayerHolder: MediaPlayerHolder) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.queue)
            .setMessage(R.string.queue_songs_clear)
            .setPositiveButton(R.string.yes) { _, _ ->
                GoPreferences.getPrefsInstance().isQueue = null
                GoPreferences.getPrefsInstance().queue = null
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
            .setNeutralButton(R.string.cancel, null)
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
