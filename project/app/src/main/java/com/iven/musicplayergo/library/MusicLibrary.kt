package com.iven.musicplayergo.library

import android.content.Context
import com.iven.musicplayergo.models.Album
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.utils.MusicUtils

class MusicLibrary {

    var allSongs: MutableList<Music>? = null

    //keys: artist || value: its songs
    var allSongsByArtist: Map<String?, List<Music>>? = null

    //keys: artist || value: albums
    val allAlbumsByArtist: MutableMap<String, List<Album>>? = mutableMapOf()

    //keys: artist || value: songs contained in the folder
    var allSongsByFolder: Map<String, List<Music>>? = null

    val randomMusic get() = allSongs?.random()

    fun buildMusicLibrary(context: Context, queriedMusic: MutableList<Music>) = try {

        allSongs = queriedMusic

        allSongsByArtist = allSongs?.groupBy { it.artist }

        allSongsByArtist?.keys?.iterator()?.forEach {
            it?.let { artist ->
                allAlbumsByArtist.apply {
                    this?.set(
                        artist,
                        MusicUtils.buildSortedArtistAlbums(
                            context.resources,
                            allSongsByArtist?.getValue(artist)
                        )
                    )
                }
            }
        }

        allSongsByFolder = allSongs?.groupBy {
            it.relativePath!!
        }
        allSongsByFolder
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
