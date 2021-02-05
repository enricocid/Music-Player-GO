package com.iven.musicplayergo.helpers

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MenuItem
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toSavedMusic
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.models.Music
import java.util.*

@SuppressLint("DefaultLocale")
object ListsHelper {

    @JvmStatic
    fun processQueryForStringsLists(
        query: String?,
        list: List<String>?
    ): List<String>? {
        // In real app you'd have it instantiated just once
        val filteredStrings = mutableListOf<String>()

        return try {
            // Case insensitive search
            list?.iterator()?.let { iterate ->
                while (iterate.hasNext()) {
                    val filteredString = iterate.next()
                    if (filteredString.toLowerCase().contains(query?.toLowerCase()!!)) {
                        filteredStrings.add(filteredString)
                    }
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
        // In real app you'd have it instantiated just once
        val filteredSongs = mutableListOf<Music>()

        return try {
            // Case insensitive search
            musicList?.iterator()?.let { iterate ->
                while (iterate.hasNext()) {
                    val filteredSong = iterate.next()
                    if (filteredSong.title?.toLowerCase()!!.contains(query?.toLowerCase()!!)) {
                        filteredSongs.add(filteredSong)
                    }
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
        GoConstants.ASCENDING_SORTING -> {
            list?.apply {
                Collections.sort(this, String.CASE_INSENSITIVE_ORDER)
            }
        }

        GoConstants.DESCENDING_SORTING -> {
            list?.apply {
                Collections.sort(this, String.CASE_INSENSITIVE_ORDER)
            }?.asReversed()
        }
        else -> list
    }

    @JvmStatic
    fun getSortedListWithNull(
        id: Int,
        list: MutableList<String?>?
    ): MutableList<String>? {
        val withoutNulls = list?.map {
            transformNullToEmpty(it)
        }?.toMutableList()

        return getSortedList(id, withoutNulls)
    }

    private fun transformNullToEmpty(toTrans: String?): String {
        if (toTrans == null) {
            return ""
        }
        return toTrans
    }

    fun getSelectedSorting(sorting: Int, menu: Menu, isAllMusic: Boolean): MenuItem {
        if (isAllMusic) {
            return when (sorting) {
                GoConstants.DEFAULT_SORTING -> menu.findItem(R.id.default_sorting)
                GoConstants.ASCENDING_SORTING -> menu.findItem(R.id.ascending_sorting)
                GoConstants.DESCENDING_SORTING -> menu.findItem(R.id.descending_sorting)
                GoConstants.DATE_ADDED_SORTING -> menu.findItem(R.id.date_added_sorting)
                else -> menu.findItem(R.id.default_sorting)
            }
        } else {
            return when (sorting) {
                GoConstants.DEFAULT_SORTING -> menu.findItem(R.id.default_sorting)
                GoConstants.ASCENDING_SORTING -> menu.findItem(R.id.ascending_sorting)
                GoConstants.DESCENDING_SORTING -> menu.findItem(R.id.descending_sorting)
                else -> menu.findItem(R.id.default_sorting)
            }
        }
    }

    @JvmStatic
    fun getSortedMusicList(
        id: Int,
        list: MutableList<Music>?
    ) : MutableList<Music>? {

        fun getSortByForTitle(song: Music) = if (goPreferences.songsVisualization != GoConstants.TITLE) {
            song.displayName
        } else {
            song.title
        }

        return when (id) {

            GoConstants.ASCENDING_SORTING -> {
                list?.sortBy { getSortByForTitle(it) }
                list
            }

            GoConstants.DESCENDING_SORTING -> {
                list?.sortBy { getSortByForTitle(it) }
                list?.asReversed()
            }

            GoConstants.TRACK_SORTING -> {
                list?.sortBy { it.track }
                list
            }

            GoConstants.TRACK_SORTING_INVERTED -> {
                list?.sortBy { it.track }
                list?.asReversed()
            }

            GoConstants.DATE_ADDED_SORTING -> {
                list?.sortBy { it.dateAdded }
                list?.asReversed()
            }
            else -> list
        }
    }

    @JvmStatic
    fun getSongsSorting(currentSorting: Int) = when (currentSorting) {
        GoConstants.TRACK_SORTING -> GoConstants.TRACK_SORTING_INVERTED
        GoConstants.TRACK_SORTING_INVERTED -> GoConstants.ASCENDING_SORTING
        GoConstants.ASCENDING_SORTING -> GoConstants.DESCENDING_SORTING
        GoConstants.DESCENDING_SORTING -> GoConstants.DATE_ADDED_SORTING
        else -> GoConstants.TRACK_SORTING
    }

    @JvmStatic
    fun getSongsDisplayNameSorting(currentSorting: Int) = if (currentSorting == GoConstants.ASCENDING_SORTING) {
        GoConstants.DESCENDING_SORTING
    } else {
        GoConstants.ASCENDING_SORTING
    }

    @JvmStatic
    fun addToHiddenItems(item: String) {
        val hiddenArtistsFolders = goPreferences.filters?.toMutableList()
        hiddenArtistsFolders?.add(item)
        goPreferences.filters = hiddenArtistsFolders?.toSet()
    }

    @JvmStatic
    fun addToLovedSongs(
        song: Music?,
        playerPosition: Int,
        launchedBy: String
    ) {
        val lovedSongs = if (goPreferences.lovedSongs != null) {
            goPreferences.lovedSongs?.toMutableList()
        } else {
            mutableListOf()
        }

        val songToSave = song?.toSavedMusic(playerPosition, launchedBy)

        songToSave?.let { savedSong ->
            if (!lovedSongs?.contains(songToSave)!!) {
                lovedSongs.add(savedSong)
            }
            goPreferences.lovedSongs = lovedSongs
        }
    }

    @JvmStatic
    fun addOrRemoveFromLovedSongs(
        song: Music?,
        playerPosition: Int,
        launchedBy: String
    ) {
        val lovedSongs =
            if (goPreferences.lovedSongs != null) {
                goPreferences.lovedSongs?.toMutableList()
            } else {
                mutableListOf()
            }
        val songToSave = song?.toSavedMusic(playerPosition, launchedBy)

        songToSave?.let { savedSong ->
            if (lovedSongs?.contains(songToSave)!!) {
                lovedSongs.remove(songToSave)
            } else {
                lovedSongs.add(savedSong)
            }
            goPreferences.lovedSongs = lovedSongs
        }
    }
}
