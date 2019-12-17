package com.iven.musicplayergo.music

import android.annotation.SuppressLint
import android.content.Context
import android.provider.MediaStore
import com.iven.musicplayergo.R
import com.iven.musicplayergo.ui.Utils
import java.io.File


class MusicLibrary {

    val allSongsUnfiltered = mutableListOf<Music>()

    lateinit var allSongsFiltered: MutableList<Music>

    //keys: artist || value: its songs
    lateinit var allSongsForArtist: Map<String?, List<Music>>

    //keys: artist || value: albums
    val allAlbumsForArtist = hashMapOf<String, List<Album>>()

    //keys: artist || value: songs contained in the folder
    lateinit var allSongsForFolder: Map<String, List<Music>>

    val randomMusic get() = allSongsFiltered.random()

    // Extension method to get all music files list from external storage/sd card
    @Suppress("DEPRECATION")
    @SuppressLint("InlinedApi")
    fun build(context: Context): Boolean {

        var hasLoaded = false
        val musicCursor = MusicUtils.getMusicCursor(context.contentResolver)

        try {
            // Query the storage for music files
            // If query result is not empty

            musicCursor?.let {
                if (musicCursor.moveToFirst()) {

                    val artist = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
                    val year = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.YEAR)
                    val track = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TRACK)
                    val title = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)
                    val duration =
                        musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)
                    val album = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)
                    val path = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)
                    val albumId = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID)

                    // Now loop through the music files
                    do {
                        val audioArtist = musicCursor.getString(artist)
                        val audioYear = musicCursor.getInt(year)
                        val audioTrack = musicCursor.getInt(track)
                        val audioTitle = musicCursor.getString(title)
                        val audioDuration = musicCursor.getLong(duration)
                        val audioAlbum = musicCursor.getString(album)
                        val audioPath = musicCursor.getString(path)
                        val audioAlbumId = musicCursor.getString(albumId)

                        // Add the current music to the list
                        allSongsUnfiltered.add(
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

                    } while (musicCursor.moveToNext())
                }

                // Removing duplicates by comparing everything except path which is different
                // if the same song is hold in different paths
                allSongsFiltered =
                    allSongsUnfiltered.distinctBy { it.artist to it.year to it.track to it.title to it.duration to it.album to it.albumId }
                        .toMutableList()

                allSongsForArtist = allSongsFiltered.groupBy { it.artist }

                allSongsForArtist.keys.iterator().forEach {

                    allAlbumsForArtist[it!!] = MusicUtils.buildSortedArtistAlbums(
                        context.resources,
                        allSongsForArtist.getValue(it)
                    )
                }

                allSongsForFolder = allSongsFiltered.groupBy {
                    val file = File(it.path!!).parentFile
                    file!!.name
                }
                hasLoaded = true
            }

        } catch (e: Exception) {
            Utils.makeToast(context, context.getString(R.string.error_unknown))
            e.printStackTrace()

        } finally {
            musicCursor?.close()
        }
        return hasLoaded
    }
}
