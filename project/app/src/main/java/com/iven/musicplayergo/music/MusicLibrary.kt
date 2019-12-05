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

    // Extension method to get all music files list from external storage/sd card
    @Suppress("DEPRECATION")
    @SuppressLint("InlinedApi")
    fun loadMusic(context: Context) {

        return try {
            // Query the storage for music files

            MusicUtils.getMusicCursor(context.contentResolver)?.apply {
                // If query result is not empty
                if (moveToFirst()) {

                    // Now loop through the music files
                    do {
                        // Add the current music to the list
                        allSongsUnfiltered.add(
                            Music(
                                getString(getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)),
                                getInt(getColumnIndex(MediaStore.Audio.AudioColumns.YEAR)),
                                getInt(getColumnIndex(MediaStore.Audio.AudioColumns.TRACK)),
                                getString(getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)),
                                getLong(getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)),
                                getString(getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)),
                                getString(getColumnIndex(MediaStore.Audio.AudioColumns.DATA)),
                                getString(getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID))
                            )
                        )

                    } while (moveToNext())
                    close()
                }
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

        } catch (e: Exception) {
            Utils.makeToast(context, context.getString(R.string.error_unknown))
            e.printStackTrace()
        }
    }
}
