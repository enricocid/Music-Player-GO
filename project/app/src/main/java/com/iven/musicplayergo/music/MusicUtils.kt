package com.iven.musicplayergo.music

import android.content.ContentResolver
import android.content.res.Resources
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.text.Html
import android.text.Spanned
import com.iven.musicplayergo.R
import java.util.*
import java.util.concurrent.TimeUnit

object MusicUtils {

    @JvmStatic
    fun getArtists(music: Map<String, Map<String, List<Music>>>): MutableList<String> {
        val artists = music.keys.toMutableList()
        artists.sort()
        return artists
    }

    @JvmStatic
    fun getArtistSongsCount(albums: Map<String, List<Music>>): Int {
        var songsCount = 0
        try {
            albums.keys.toMutableList().forEach {
                songsCount += albums.getValue(it).size
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songsCount
    }

    @JvmStatic
    fun getAlbumPositionInList(album: String, albums: List<Album>): Int {
        var returnedPosition = 0
        try {
            returnedPosition = albums.indexOfFirst { it.title == album }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return returnedPosition
    }

    @JvmStatic
    fun getArtistSongs(albums: Map<String, List<Music>>): MutableList<Music> {
        val artistSongs = mutableListOf<Music>()
        try {
            albums.keys.toMutableList().forEach {
                artistSongs.addAll(albums.getValue(it))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return artistSongs
    }

    @JvmStatic
    fun getYearForAlbum(resources: Resources, year: Int): String {
        return if (year == 0) resources.getString(R.string.unknown_year) else year.toString()
    }

    @JvmStatic
    fun buildSortedArtistAlbums(resources: Resources, albums: Map<String, List<Music>>): List<Album> {

        val sortedAlbums = mutableListOf<Album>()

        try {
            albums.keys.toMutableList().forEach {
                sortedAlbums.add(Album(it, getYearForAlbum(resources, albums.getValue(it)[0].year)))
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        sortedAlbums.sortBy { it.year }
        return sortedAlbums
    }

    @JvmStatic
    fun formatSongDuration(duration: Long): String {
        return String.format(
            Locale.getDefault(), "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(duration),
            TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(
                    duration
                )
            )
        )
    }

    @JvmStatic
    fun formatSongTrack(trackNumber: Int): Int {
        var formatted = trackNumber
        if (trackNumber >= 1000) {
            formatted = trackNumber % 1000
        }
        return formatted
    }

    @JvmStatic
    fun buildSpanned(res: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(res, Html.FROM_HTML_MODE_LEGACY)
        else
            Html.fromHtml(res)
    }

    @JvmStatic
    fun getMusicCursor(contentResolver: ContentResolver): Cursor? {
        return contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, // Uri
            null, // Projection
            null, // Selection
            null, // Selection arguments
            null // Sort order
        )
    }
}