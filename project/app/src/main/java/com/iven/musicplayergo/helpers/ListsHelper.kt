package com.iven.musicplayergo.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toSavedMusicWithoutPosition
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
        // In real app you'd have it instantiated just once
        val filteredSongs = mutableListOf<Music>()

        return try {
            // Case insensitive search
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
        GoConstants.DESCENDING_SORTING -> {
            list?.apply {
                Collections.sort(this, String.CASE_INSENSITIVE_ORDER)
            }
            list
        }

        GoConstants.ASCENDING_SORTING -> {
            list?.apply {
                Collections.sort(this, String.CASE_INSENSITIVE_ORDER)
            }
            list?.asReversed()
        }
        else -> list
    }

    @JvmStatic
    fun getSelectedSorting(sorting: Int, menu: Menu): MenuItem = when (sorting) {
        GoConstants.DEFAULT_SORTING -> menu.findItem(R.id.default_sorting)
        GoConstants.ASCENDING_SORTING -> menu.findItem(R.id.ascending_sorting)
        else -> menu.findItem(R.id.descending_sorting)
    }

    @JvmStatic
    fun getSortedMusicList(
        id: Int,
        list: MutableList<Music>?
    ) = when (id) {

        GoConstants.DESCENDING_SORTING -> {
            list?.sortBy { it.title }
            list
        }

        GoConstants.ASCENDING_SORTING -> {
            list?.sortBy { it.title }
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
        else -> list
    }

    @JvmStatic
    fun getSongsSorting(currentSorting: Int) = when (currentSorting) {
        GoConstants.TRACK_SORTING -> GoConstants.TRACK_SORTING_INVERTED
        GoConstants.TRACK_SORTING_INVERTED -> GoConstants.ASCENDING_SORTING
        GoConstants.ASCENDING_SORTING -> GoConstants.DESCENDING_SORTING
        else -> GoConstants.TRACK_SORTING
    }

    @JvmStatic
    fun addToHiddenItems(item: String) {
        val hiddenArtistsFolders = goPreferences.filters?.toMutableList()
        hiddenArtistsFolders?.add(item)
        goPreferences.filters = hiddenArtistsFolders?.toSet()
    }

    @JvmStatic
    fun addOrRemoveFromLovedSongs(
        context: Context,
        song: Music?,
        playerPosition: Int,
        isPlayingFromFolder: Boolean
    ) {
        val lovedSongs =
            if (goPreferences.lovedSongs != null) goPreferences.lovedSongs else mutableListOf()

        val songToSave = song?.toSavedMusicWithoutPosition(isPlayingFromFolder)

        songToSave?.let { savedSong ->
            if (lovedSongs?.contains(savedSong)!!) {
                lovedSongs.remove(
                    savedSong
                )
            }else {
                lovedSongs.add(
                    savedSong
                )
            }
            goPreferences.lovedSongs = lovedSongs
        }
    }
}
