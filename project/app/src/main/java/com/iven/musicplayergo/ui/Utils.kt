package com.iven.musicplayergo.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.*
import com.iven.musicplayergo.adapters.LovedSongsAdapter
import com.iven.musicplayergo.adapters.QueueAdapter
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.player.MediaPlayerHolder
import java.util.*

@SuppressLint("DefaultLocale")
object Utils {

    @JvmStatic
    fun hasToShowPermissionRationale(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun showPermissionRationale(activity: Activity) {

        activity.apply {
            MaterialDialog(this).show {

                cancelOnTouchOutside(false)
                cornerRadius(res = R.dimen.md_corner_radius)

                title(res = R.string.app_name)
                icon(res = R.drawable.ic_folder)

                message(R.string.perm_rationale)
                positiveButton {
                    ActivityCompat.requestPermissions(
                        this@apply,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        2588
                    )
                }
                negativeButton {
                    makeToast(
                        this@apply,
                        getString(R.string.perm_rationale)
                    )
                    dismiss()
                    finishAndRemoveTask()
                }
            }
        }
    }

    @JvmStatic
    fun makeToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG)
            .show()
    }

    @JvmStatic
    fun processQueryForStringsLists(
        query: String?,
        list: List<String>
    ): List<String>? {
        // in real app you'd have it instantiated just once
        val filteredStrings = mutableListOf<String>()

        return try {
            // case insensitive search
            list.iterator().forEach {
                if (it.toLowerCase().contains(query?.toLowerCase()!!)) {
                    filteredStrings.add(it)
                }
            }
            return filteredStrings
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun processQueryForMusic(query: String?, musicList: List<Music>): List<Any>? {
        // in real app you'd have it instantiated just once
        val filteredSongs = mutableListOf<Any>()

        return try {
            // case insensitive search
            musicList.iterator().forEach {
                if (it.title?.toLowerCase()!!.contains(query?.toLowerCase()!!)) {
                    filteredSongs.add(it)
                }
            }
            return filteredSongs
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun getSortedList(
        id: Int,
        list: MutableList<String>,
        defaultList: MutableList<String>
    ): MutableList<String> {
        return when (id) {

            ASCENDING_SORTING -> {
                Collections.sort(list, String.CASE_INSENSITIVE_ORDER)
                list
            }

            DESCENDING_SORTING -> {
                Collections.sort(list, String.CASE_INSENSITIVE_ORDER)
                list.asReversed()
            }
            else -> defaultList
        }
    }

    @JvmStatic
    fun getSelectedSortingMenuItem(sorting: Int, menu: Menu): MenuItem {
        return when (sorting) {
            DEFAULT_SORTING -> menu.findItem(R.id.default_sorting)
            ASCENDING_SORTING -> menu.findItem(R.id.ascending_sorting)
            else -> menu.findItem(R.id.descending_sorting)
        }
    }

    @JvmStatic
    fun showQueueSongsDialog(
        context: Context,
        mediaPlayerHolder: MediaPlayerHolder
    ): Pair<MaterialDialog, QueueAdapter> {

        val dialog = MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

            cornerRadius(res = R.dimen.md_corner_radius)

            title(res = R.string.queue)

            AppCompatResources.getDrawable(context, R.drawable.ic_queue_music)?.apply {
                mutate()
                setTint(ThemeHelper.resolveColorAttr(context, android.R.attr.textColorPrimary))
                icon(drawable = this)
            }

            customListAdapter(
                QueueAdapter(context, this, mediaPlayerHolder)
            )
            getRecyclerView().addItemDecoration(
                ThemeHelper.getRecyclerViewDivider(
                    context
                )
            )
            if (goPreferences.isEdgeToEdge && window != null) ThemeHelper.handleEdgeToEdge(
                window,
                view
            )
        }
        return Pair(dialog, dialog.getListAdapter() as QueueAdapter)
    }

    @JvmStatic
    fun showDeleteQueueSongDialog(
        context: Context,
        song: Pair<Music, Int>,
        queueSongsDialog: MaterialDialog,
        queueAdapter: QueueAdapter,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context).show {

            cornerRadius(res = R.dimen.md_corner_radius)

            title(res = R.string.queue)
            icon(res = R.drawable.ic_delete_forever)

            message(
                text = context.getString(
                    R.string.queue_song_remove,
                    song.first.title
                )
            )
            positiveButton {

                mediaPlayerHolder.apply {
                    queueSongs.removeAt(song.second)
                    queueAdapter.swapQueueSongs(queueSongs)

                    if (queueSongs.isEmpty()) {
                        isQueue = false
                        mediaPlayerInterface.onQueueStartedOrEnded(false)
                        queueSongsDialog.dismiss()
                    }
                }
            }
            negativeButton {}
        }
    }

    @JvmStatic
    fun showClearQueueDialog(
        context: Context,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context).show {

            cornerRadius(res = R.dimen.md_corner_radius)

            title(res = R.string.queue)
            icon(res = R.drawable.ic_delete_forever)

            message(text = context.getString(R.string.queue_songs_clear))

            positiveButton {

                mediaPlayerHolder.apply {
                    if (isQueueStarted && isPlaying) {

                        restorePreQueueSongs()
                        skip(
                            true
                        )
                    }
                    setQueueEnabled(false)
                }
            }
            negativeButton {}
        }
    }

    @JvmStatic
    fun addToLovedSongs(context: Context, song: Music, currentPosition: Int) {
        val lovedSongs =
            if (goPreferences.lovedSongs != null) goPreferences.lovedSongs else mutableListOf()
        if (!lovedSongs?.contains(Pair(song, currentPosition))!!) {
            lovedSongs.add(
                Pair(
                    song,
                    currentPosition
                )
            )
            makeToast(
                context,
                context.getString(
                    R.string.loved_song_added,
                    song.title!!,
                    MusicUtils.formatSongDuration(currentPosition.toLong(), false)
                )
            )
            goPreferences.lovedSongs = lovedSongs
        }
    }

    @JvmStatic
    fun showLovedSongsDialog(
        context: Context,
        uiControlInterface: UIControlInterface,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

            cornerRadius(res = R.dimen.md_corner_radius)
            title(res = R.string.loved_songs)
            icon(res = R.drawable.ic_favorite)

            customListAdapter(
                LovedSongsAdapter(context, this, uiControlInterface, mediaPlayerHolder)
            )
            getRecyclerView().addItemDecoration(
                ThemeHelper.getRecyclerViewDivider(
                    context
                )
            )
            if (goPreferences.isEdgeToEdge && window != null) ThemeHelper.handleEdgeToEdge(
                window,
                view
            )
        }
    }

    @JvmStatic
    fun showDeleteLovedSongDialog(
        context: Context,
        item: Pair<Music, Int>,
        lovedSongsAdapter: LovedSongsAdapter
    ) {

        val lovedSongs = goPreferences.lovedSongs?.toMutableList()

        MaterialDialog(context).show {

            cornerRadius(res = R.dimen.md_corner_radius)

            title(res = R.string.loved_songs)
            icon(res = R.drawable.ic_delete_forever)

            message(
                text = context.getString(
                    R.string.loved_song_remove,
                    item.first.title,
                    MusicUtils.formatSongDuration(item.second.toLong(), false)
                )
            )
            positiveButton {
                lovedSongs?.remove(item)
                goPreferences.lovedSongs = lovedSongs
                lovedSongsAdapter.swapSongs(lovedSongs!!)
            }
            negativeButton {}
        }
    }

    @JvmStatic
    fun showClearLovedSongDialog(
        context: Context,
        uiControlInterface: UIControlInterface
    ) {

        MaterialDialog(context).show {

            cornerRadius(res = R.dimen.md_corner_radius)

            title(res = R.string.loved_songs)
            icon(res = R.drawable.ic_delete_forever)

            message(
                text = context.getString(R.string.loved_songs_clear)
            )
            positiveButton {
                uiControlInterface.onLovedSongsUpdate(true)
            }
            negativeButton {}
        }
    }

    @JvmStatic
    fun showAddToLovedQueueSongsPopup(
        context: Context,
        itemView: View,
        song: Music,
        uiControlInterface: UIControlInterface
    ) {
        PopupMenu(context, itemView).apply {
            setOnMenuItemClickListener {

                when (it.itemId) {
                    R.id.loved_songs_add -> {
                        addToLovedSongs(
                            context,
                            song,
                            0
                        )
                        uiControlInterface.onLovedSongsUpdate(false)
                    }
                    R.id.queue_add -> {
                        uiControlInterface.onAddToQueue(song)
                    }
                }

                return@setOnMenuItemClickListener true
            }
            inflate(R.menu.menu_do_something)
            gravity = Gravity.END
            show()
        }
    }

    @JvmStatic
    fun stopPlaybackDialog(
        context: Context,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context).show {

            cornerRadius(res = R.dimen.md_corner_radius)

            title(res = R.string.app_name)
            icon(res = R.drawable.ic_stop)

            message(text = context.getString(R.string.on_close_activity))
            positiveButton {
                mediaPlayerHolder.stopPlaybackService(true)
            }
            negativeButton(text = context.getString(android.R.string.no)) {
                mediaPlayerHolder.stopPlaybackService(false)
            }
        }
    }

    @JvmStatic
    fun openCustomTab(
        context: Context,
        link: String
    ) {

        try {
            val accent = ThemeHelper.resolveThemeAccent(context)

            CustomTabsIntent.Builder().apply {
                setSecondaryToolbarColor(accent)
                addDefaultShareMenuItem()
                setShowTitle(true)

                // https://stackoverflow.com/a/55260049
                AppCompatResources.getDrawable(context, R.drawable.ic_navigate_before)?.let {
                    DrawableCompat.setTint(it, accent)
                    setCloseButtonIcon(it.toBitmap())
                }

                build().launchUrl(context, Uri.parse(link))
            }
        } catch (e: Exception) {
            makeToast(context, context.getString(R.string.no_browser))
            e.printStackTrace()
        }
    }
}
