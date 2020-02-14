package com.iven.musicplayergo.utils

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.*
import com.iven.musicplayergo.adapters.LovedSongsAdapter
import com.iven.musicplayergo.adapters.QueueAdapter
import com.iven.musicplayergo.musicloadutils.Music
import com.iven.musicplayergo.musicloadutils.SavedMusic
import com.iven.musicplayergo.player.MediaPlayerHolder
import de.halfbit.edgetoedge.Edge
import de.halfbit.edgetoedge.edgeToEdge
import java.util.*

@SuppressLint("DefaultLocale")
object Utils {

    @JvmStatic
    fun isAndroidQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @JvmStatic
    fun hasToAskForReadStoragePermission(activity: Activity) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED

    @JvmStatic
    fun manageAskForReadStoragePermission(
        activity: Activity,
        uiControlInterface: UIControlInterface
    ) {

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {

            MaterialDialog(activity).show {

                cancelOnTouchOutside(false)

                title(R.string.app_name)
                icon(R.drawable.ic_folder)

                message(R.string.perm_rationale)
                positiveButton(android.R.string.ok) {
                    askForReadStoragePermission(activity)
                }
                negativeButton {
                    uiControlInterface.onDenyPermission()
                }
            }
        } else {
            askForReadStoragePermission(activity)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun askForReadStoragePermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_READ_EXTERNAL_STORAGE
        )
    }

    @JvmStatic
    fun processQueryForStringsLists(
        query: String?,
        list: List<String>?
    ): List<String>? {
        // in real app you'd have it instantiated just once
        val filteredStrings = mutableListOf<String>()

        return try {
            // case insensitive search
            list?.iterator()?.forEach { filteredString ->
                if (filteredString.toLowerCase().contains(query?.toLowerCase()!!)) {
                    filteredStrings.add(filteredString)
                }
            }
            return filteredStrings
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun processQueryForMusic(query: String?, musicList: List<Music>?): List<Music>? {
        // in real app you'd have it instantiated just once
        val filteredSongs = mutableListOf<Music>()

        return try {
            // case insensitive search
            musicList?.iterator()?.forEach { filteredSong ->
                if (filteredSong.title?.toLowerCase()!!.contains(query?.toLowerCase()!!)) {
                    filteredSongs.add(filteredSong)
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
        list: MutableList<String>?
    ) = when (id) {
        DESCENDING_SORTING -> {
            list?.apply {
                Collections.sort(this, String.CASE_INSENSITIVE_ORDER)
            }
            list
        }

        ASCENDING_SORTING -> {
            list?.apply {
                Collections.sort(this, String.CASE_INSENSITIVE_ORDER)
            }
            list?.asReversed()
        }
        else -> list
    }

    @JvmStatic
    fun getSelectedSorting(sorting: Int, menu: Menu): MenuItem = when (sorting) {
        DEFAULT_SORTING -> menu.findItem(R.id.default_sorting)
        ASCENDING_SORTING -> menu.findItem(R.id.ascending_sorting)
        else -> menu.findItem(R.id.descending_sorting)
    }

    @JvmStatic
    fun getSortedMusicList(
        id: Int,
        list: MutableList<Music>?
    ) = when (id) {

        DESCENDING_SORTING -> {
            list?.sortBy { it.title }
            list
        }

        ASCENDING_SORTING -> {
            list?.sortBy { it.title }
            list?.asReversed()
        }

        TRACK_SORTING -> {
            list?.sortBy { it.track }
            list
        }

        TRACK_SORTING_INVERTED -> {
            list?.sortBy { it.track }
            list?.asReversed()
        }
        else -> list
    }

    @JvmStatic
    fun getSongsSorting(currentSorting: Int) = when (currentSorting) {
        TRACK_SORTING -> TRACK_SORTING_INVERTED
        TRACK_SORTING_INVERTED -> ASCENDING_SORTING
        ASCENDING_SORTING -> DESCENDING_SORTING
        else -> TRACK_SORTING
    }

    @JvmStatic
    fun showQueueSongsDialog(
        context: Context,
        mediaPlayerHolder: MediaPlayerHolder
    ): Pair<MaterialDialog, QueueAdapter> {

        val dialog = MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

            title(R.string.queue)

            AppCompatResources.getDrawable(context, R.drawable.ic_queue_music)?.apply {
                mutate()
                setTint(ThemeHelper.resolveColorAttr(context, android.R.attr.textColorPrimary))
                icon(drawable = this)
            }

            customListAdapter(
                QueueAdapter(context, this, mediaPlayerHolder)
            )

            val recyclerView = getRecyclerView()

            if (ThemeHelper.isDeviceLand(context.resources)) {
                recyclerView.layoutManager = GridLayoutManager(context, 3)
            } else {
                recyclerView.addItemDecoration(
                    ThemeHelper.getRecyclerViewDivider(
                        context
                    )
                )
                if (goPreferences.isEdgeToEdge) {
                    window?.apply {
                        ThemeHelper.handleLightSystemBars(decorView)
                        edgeToEdge {
                            recyclerView.fit { Edge.Bottom }
                            decorView.fit { Edge.Top }
                        }
                    }
                }
            }
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

            title(R.string.queue)
            icon(R.drawable.ic_delete_forever)

            message(
                text = context.getString(
                    R.string.queue_song_remove,
                    song.first.title
                )
            )
            positiveButton(R.string.yes) {

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
            negativeButton(R.string.no)
        }
    }

    @JvmStatic
    fun showClearQueueDialog(
        context: Context,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context).show {

            title(R.string.queue)
            icon(R.drawable.ic_delete_forever)

            message(R.string.queue_songs_clear)

            positiveButton(R.string.yes) {

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
            negativeButton(R.string.no)
        }
    }

    @JvmStatic
    fun addToLovedSongs(context: Context, songToSave: SavedMusic?) {
        val lovedSongs =
            if (goPreferences.lovedSongs != null) goPreferences.lovedSongs else mutableListOf()

        songToSave?.let { song ->
            if (!lovedSongs?.contains(song)!!) {
                lovedSongs.add(
                    song
                )
                context.getString(
                    R.string.loved_song_added,
                    song.title,
                    song.startFrom.toLong().toFormattedDuration(false)
                ).toToast(context)
                goPreferences.lovedSongs = lovedSongs
            }
        }

    }

    @JvmStatic
    fun showLovedSongsDialog(
        context: Context,
        uiControlInterface: UIControlInterface,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {

            title(R.string.loved_songs)
            icon(R.drawable.ic_favorite)

            customListAdapter(
                LovedSongsAdapter(context, this, uiControlInterface, mediaPlayerHolder)
            )

            val recyclerView = getRecyclerView()

            if (ThemeHelper.isDeviceLand(context.resources)) {
                recyclerView.layoutManager = GridLayoutManager(context, 3)
            } else {
                recyclerView.addItemDecoration(
                    ThemeHelper.getRecyclerViewDivider(
                        context
                    )
                )
                if (goPreferences.isEdgeToEdge) {
                    window?.apply {
                        ThemeHelper.handleLightSystemBars(decorView)
                        edgeToEdge {
                            recyclerView.fit { Edge.Bottom }
                            decorView.fit { Edge.Top }
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun showDeleteLovedSongDialog(
        context: Context,
        songToDelete: SavedMusic?,
        lovedSongsAdapter: LovedSongsAdapter
    ) {

        val lovedSongs = goPreferences.lovedSongs?.toMutableList()

        MaterialDialog(context).show {

            title(R.string.loved_songs)
            icon(R.drawable.ic_delete_forever)

            message(
                text = context.getString(
                    R.string.loved_song_remove,
                    songToDelete?.title,
                    songToDelete?.startFrom?.toLong()?.toFormattedDuration(false)
                )
            )
            positiveButton(R.string.yes) {
                lovedSongs?.remove(songToDelete)
                goPreferences.lovedSongs = lovedSongs
                lovedSongsAdapter.swapSongs(lovedSongs)
            }

            negativeButton(R.string.no)
        }
    }

    @JvmStatic
    fun showClearLovedSongDialog(
        context: Context,
        uiControlInterface: UIControlInterface
    ) {

        MaterialDialog(context).show {

            title(R.string.loved_songs)
            icon(R.drawable.ic_delete_forever)

            message(R.string.loved_songs_clear)
            positiveButton(R.string.yes) {
                uiControlInterface.onLovedSongsUpdate(true)
            }
            negativeButton(R.string.no)
        }
    }

    @JvmStatic
    fun showHidePopup(
        context: Context,
        itemView: View?,
        stringToFilter: String?,
        uiControlInterface: UIControlInterface
    ) {
        itemView?.let { view ->
            PopupMenu(context, view).apply {
                setOnMenuItemClickListener {
                    uiControlInterface.onAddToFilter(stringToFilter)
                    return@setOnMenuItemClickListener true
                }
                inflate(R.menu.menu_filter)
                gravity = Gravity.END
                show()
            }
        }
    }

    @JvmStatic
    fun showDoSomethingPopup(
        context: Context,
        itemView: View?,
        song: Music?,
        isFolder: Boolean,
        uiControlInterface: UIControlInterface
    ) {
        itemView?.let {
            PopupMenu(context, itemView).apply {
                setOnMenuItemClickListener {

                    when (it.itemId) {
                        R.id.loved_songs_add -> {
                            addToLovedSongs(
                                context,
                                song?.toSavedMusic(0, isFolder)
                            )
                            uiControlInterface.onLovedSongsUpdate(false)
                        }
                        R.id.queue_add -> uiControlInterface.onAddToQueue(song)
                    }

                    return@setOnMenuItemClickListener true
                }
                inflate(R.menu.menu_do_something)
                gravity = Gravity.END
                show()
            }
        }
    }

    @JvmStatic
    fun addToHiddenItems(item: String) {
        val hiddenArtistsFolders = goPreferences.filters?.toMutableList()
        hiddenArtistsFolders?.add(item)
        goPreferences.filters = hiddenArtistsFolders?.toSet()
    }

    @JvmStatic
    fun stopPlaybackDialog(
        context: Context,
        mediaPlayerHolder: MediaPlayerHolder
    ) {

        MaterialDialog(context).show {

            title(R.string.app_name)
            icon(R.drawable.ic_stop)

            message(R.string.on_close_activity)
            positiveButton(R.string.yes) {
                mediaPlayerHolder.stopPlaybackService(true)
            }
            negativeButton(R.string.no) {
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
            CustomTabsIntent.Builder().apply {
                addDefaultShareMenuItem()
                setShowTitle(true)
                build().launchUrl(context, Uri.parse(link))
            }
        } catch (e: Exception) {
            context.getString(R.string.error_no_browser).toToast(context)
            e.printStackTrace()
        }
    }
}
