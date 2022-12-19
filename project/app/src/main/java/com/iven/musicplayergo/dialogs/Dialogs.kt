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
import com.iven.musicplayergo.utils.Lists


object Dialogs {

    @JvmStatic
    fun showClearFiltersDialog(activity: Activity) {
        val uiControlInterface = (activity as UIControlInterface)
        if (GoPreferences.getPrefsInstance().isAskForRemoval) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.filter_pref_title)
                .setMessage(R.string.filters_clear)
                .setPositiveButton(R.string.yes) { _, _ ->
                    uiControlInterface.onFiltersCleared()
                }
                .setNegativeButton(R.string.no, null)
                .show()
            return
        }
        uiControlInterface.onFiltersCleared()
    }

    @JvmStatic
    fun showClearQueueDialog(context: Context) {

        val mediaPlayerHolder = MediaPlayerHolder.getInstance()
        val prefs = GoPreferences.getPrefsInstance()
        fun clearQueue() {
            prefs.isQueue = null
            prefs.queue = null
            with(mediaPlayerHolder) {
                queueSongs.clear()
                setQueueEnabled(enabled = false, canSkip = isQueueStarted)
            }
        }

        if (prefs.isAskForRemoval) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.queue)
                .setMessage(R.string.queue_songs_clear)
                .setPositiveButton(R.string.yes) { _, _ ->
                    clearQueue()
                }
                .setNegativeButton(R.string.no, null)
                .show()
            return
        }
        clearQueue()
    }

    @JvmStatic
    fun showClearFavoritesDialog(activity: Activity) {
        val uiControlInterface = activity as UIControlInterface
        if (GoPreferences.getPrefsInstance().isAskForRemoval) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.favorites)
                .setMessage(R.string.favorites_clear)
                .setPositiveButton(R.string.yes) { _, _ ->
                    uiControlInterface.onFavoritesUpdated(clear = true)
                }
                .setNegativeButton(R.string.no, null)
                .show()
            return
        }
        uiControlInterface.onFavoritesUpdated(clear = true)
    }

    @JvmStatic
    fun stopPlaybackDialog(context: Context) {
        val mediaPlayerHolder = MediaPlayerHolder.getInstance()
        if (GoPreferences.getPrefsInstance().isAskForRemoval) {
            MaterialAlertDialogBuilder(context)
                .setCancelable(false)
                .setTitle(R.string.app_name)
                .setMessage(R.string.on_close_activity)
                .setPositiveButton(R.string.yes) { _, _ ->
                    mediaPlayerHolder.stopPlaybackService(stopPlayback = true, fromUser = true, fromFocus = false)
                }
                .setNegativeButton(R.string.no) { _, _ ->
                    mediaPlayerHolder.stopPlaybackService(stopPlayback = false, fromUser = true, fromFocus = false)
                }
                .setNeutralButton(R.string.cancel, null)
                .show()
            return
        }
        mediaPlayerHolder.stopPlaybackService(stopPlayback = false, fromUser = true, fromFocus = false)
    }

    @JvmStatic
    fun showSaveSortingDialog(activity: Activity, artistOrFolder: String?, launchedBy: String, sorting: Int) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.sorting_pref)
            .setMessage(R.string.sorting_pref_save)
            .setPositiveButton(R.string.yes) { _, _ ->
                Lists.addToSortings(activity, GoPreferences.PREFS_DETAILS_SORTING, launchedBy, sorting)
            }
            .setNegativeButton(R.string.no) { _, _ ->
                Lists.addToSortings(activity, artistOrFolder, launchedBy, sorting)
            }
            .setNeutralButton(R.string.sorting_pref_reset_neutral) { _, _ ->
                GoPreferences.getPrefsInstance().isSetDefSorting = false
            }
            .show()
    }

    @JvmStatic
    fun showResetSortingsDialog(context: Context) {
        val prefs = GoPreferences.getPrefsInstance()
        if (prefs.isAskForRemoval) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.sorting_pref)
                .setMessage(R.string.sorting_pref_reset_confirm)
                .setPositiveButton(R.string.yes) { _, _ ->
                    prefs.sortings = null
                    prefs.isSetDefSorting = true
                }
                .setNegativeButton(R.string.no, null)
                .show()
            return
        }
        prefs.sortings = null
        prefs.isSetDefSorting = true
    }

    @JvmStatic
    fun computeDurationText(ctx: Context, favorite: Music?): Spanned? {
        favorite?.startFrom?.let { start ->
            if (start > 0L) {
                return ctx.getString(
                    R.string.favorite_subtitle,
                    start.toLong().toFormattedDuration(
                        isAlbum = false,
                        isSeekBar = false
                    ),
                    favorite.duration.toFormattedDuration(isAlbum = false, isSeekBar = false)
                ).parseAsHtml()
            }
        }
        return favorite?.duration?.toFormattedDuration(isAlbum = false, isSeekBar = false)
            ?.parseAsHtml()
    }
}
