package com.iven.musicplayergo.music

import android.annotation.SuppressLint
import android.content.Context
import android.provider.MediaStore
import android.widget.Toast
import com.iven.musicplayergo.R
import com.iven.musicplayergo.ui.Utils


class MusicLibrary {

    private var allSongsUnfiltered: MutableList<Music>? = mutableListOf()

    var allSongsFiltered: MutableList<Music>? = null

    //keys: artist || value: its songs
    lateinit var allSongsByArtist: Map<String?, List<Music>>

    //keys: artist || value: albums
    val allAlbumsByArtist = hashMapOf<String, List<Album>>()

    //keys: artist || value: songs contained in the folder
    var allSongsByFolder: Map<String, List<Music>>? = null

    val randomMusic get() = allSongsFiltered?.random()

    @Suppress("DEPRECATION")
    @SuppressLint("InlinedApi")
    fun queryForMusic(context: Context): MutableList<Music>? {

        try {

            val musicCursor = MusicUtils.getMusicCursor(context.contentResolver)

            // Query the storage for music files
            // If query result is not empty
            musicCursor?.use {
                if (it.moveToFirst()) {

                    val artist =
                        it.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
                    val year =
                        it.getColumnIndex(MediaStore.Audio.AudioColumns.YEAR)
                    val track =
                        it.getColumnIndex(MediaStore.Audio.AudioColumns.TRACK)
                    val title =
                        it.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)
                    val duration =
                        it.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)
                    val album =
                        it.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)
                    val path =
                        it.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)
                    val albumId =
                        it.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID)

                    // Now loop through the music files
                    do {
                        val audioArtist = it.getString(artist)
                        val audioYear = it.getInt(year)
                        val audioTrack = it.getInt(track)
                        val audioTitle = it.getString(title)
                        val audioDuration = it.getLong(duration)
                        val audioAlbum = it.getString(album)
                        val audioPath = it.getString(path)
                        val audioAlbumId = it.getString(albumId)

                        // Add the current music to the list
                        allSongsUnfiltered?.add(
                            Music(
                                audioArtist,
                                audioYear,
                                audioTrack,
                                audioTitle,
                                audioDuration,
                                audioAlbum,
                                audioPath,
                                audioAlbumId
                            )
                        )

                    } while (it.moveToNext())
                    it.close()
                }
            }
        } catch (e: Exception) {
            allSongsUnfiltered = null
            Utils.makeToast(
                context,
                context.getString(R.string.error_unknown),
                Toast.LENGTH_LONG
            )
            e.printStackTrace()
        }
        return allSongsUnfiltered
    }

    // Extension method to sort the device music
    @Suppress("DEPRECATION")
    @SuppressLint("InlinedApi")
    fun buildLibrary(
        context: Context,
        loadedSongs: MutableList<Music>?
    ): HashMap<String, List<Album>> {


        // Removing duplicates by comparing everything except path which is different
        // if the same song is hold in different paths
        allSongsFiltered =
            loadedSongs?.distinctBy { it.artist to it.year to it.track to it.title to it.duration to it.album to it.albumId }
                ?.toMutableList()

        allSongsByArtist = allSongsFiltered?.groupBy { it.artist }!!

        allSongsByArtist.keys.iterator().forEach {

            allAlbumsByArtist[it!!] = MusicUtils.buildSortedArtistAlbums(
                context.resources,
                allSongsByArtist.getValue(it)
            )
        }

        allSongsByFolder = allSongsFiltered?.groupBy {
            MusicUtils.getFolderName(it.path!!)
        }

        return allAlbumsByArtist
    }
}
