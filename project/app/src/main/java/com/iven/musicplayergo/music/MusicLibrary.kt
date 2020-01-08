package com.iven.musicplayergo.music

import android.annotation.SuppressLint
import android.content.Context
import android.provider.MediaStore


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
            musicCursor?.use { cursor ->

                val artist =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)
                val year =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.YEAR)
                val track =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TRACK)
                val title =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
                val duration =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
                val album =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM)
                val path =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)
                val albumId =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID)

                while (cursor.moveToNext()) {

                    // Now loop through the music files
                    val audioArtist = cursor.getString(artist)
                    val audioYear = cursor.getInt(year)
                    val audioTrack = cursor.getInt(track)
                    val audioTitle = cursor.getString(title)
                    val audioDuration = cursor.getLong(duration)
                    val audioAlbum = cursor.getString(album)
                    val audioPath = cursor.getString(path)
                    val audioAlbumId = cursor.getString(albumId)

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
                }
            }
        } catch (e: Exception) {
            allSongsUnfiltered = null
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
