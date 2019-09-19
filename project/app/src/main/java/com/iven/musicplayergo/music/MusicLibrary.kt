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

    //keys: artist || keys: album, value: album songs
    val allCategorizedMusic = hashMapOf<String, Map<String?, List<Music>>>()

    lateinit var allCategorizedMusicByFolder: Map<String, List<Music>>

    //Build a Map with key: artist, value: Map with key: album, value: songs
    private fun categorizeMusicByArtistAndAlbums(music: List<Music>) {

        val musicSortedByArtist = music.groupBy { it.artist }

        musicSortedByArtist.keys.iterator().forEach {
            val albums = musicSortedByArtist[it]?.groupBy { song -> song.album }
            allCategorizedMusic[it.toString()] = albums!!
        }
    }

    //Build a Map with key: directory name, value: songs list
    private fun categorizeMusicByFolder(music: List<Music>) {

        allCategorizedMusicByFolder = music.groupBy {
            val file = File(it.path!!).parentFile
            file!!.name
        }
    }

    // Extension method to get all music files list from external storage/sd card
    @Suppress("DEPRECATION")
    @SuppressLint("InlinedApi")
    fun loadMusic(context: Context) {

        val musicCursor = MusicUtils.getMusicCursor(context.contentResolver)!!

        return try {
            // Query the storage for music files

            // If query result is not empty
            if (musicCursor.moveToFirst()) {
                val artist = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
                val year = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.YEAR)
                val track = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TRACK)
                val title = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)
                val duration = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)
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
                musicCursor.close()
            }

            // Removing duplicates by comparing everything except path which is different
            // if the same song is hold in different paths
            allSongsFiltered =
                allSongsUnfiltered.distinctBy { it.artist to it.year to it.track to it.title to it.duration to it.album to it.albumId }
                    .toMutableList()

            categorizeMusicByArtistAndAlbums(allSongsFiltered)

            categorizeMusicByFolder(allSongsFiltered)

        } catch (e: Exception) {
            Utils.makeUnknownErrorToast(
                context,
                R.string.error_unknown
            )
            e.printStackTrace()
        }
    }
}
