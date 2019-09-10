package com.iven.musicplayergo

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iven.musicplayergo.music.Music
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    //first: all device songs, second: Map with key: artist, value: Map with key: album, value: songs
    private val musicLiveData: MutableLiveData<Pair<MutableList<Music>, Map<String, Map<String?, List<Music>>>>> =
        MutableLiveData()

    private val allDeviceSongs: MutableList<Music> = mutableListOf()

    //key: artist || key: album, value: album songs
    private val categorizedMusicByAlbum = hashMapOf<String, Map<String?, List<Music>>>()

    //Load music from the device
    fun getMusic(context: Context): MutableLiveData<Pair<MutableList<Music>, Map<String, Map<String?, List<Music>>>>> {

        GlobalScope.launch(Dispatchers.Main) {

            musicLiveData.value = async(Dispatchers.Main) {
                return@async loadMusic(context)

            }.await()
        }
        return musicLiveData
    }

    // Extension method to get all music files list from external storage/sd card
    @Suppress("DEPRECATION")
    @SuppressLint("InlinedApi")
    private fun loadMusic(context: Context): Pair<MutableList<Music>, Map<String, Map<String?, List<Music>>>> {

        val musicCursor = getMusicCursor(context.contentResolver)!!

        return try {
            // Query the storage for music files

            // If query result is not empty
            if (musicCursor.moveToFirst()) {
                val artist = musicCursor.getColumnIndex(AudioColumns.ARTIST)
                val year = musicCursor.getColumnIndex(AudioColumns.YEAR)
                val track = musicCursor.getColumnIndex(AudioColumns.TRACK)
                val title = musicCursor.getColumnIndex(AudioColumns.TITLE)
                val duration = musicCursor.getColumnIndex(AudioColumns.DURATION)
                val album = musicCursor.getColumnIndex(AudioColumns.ALBUM)
                val path = musicCursor.getColumnIndex(AudioColumns.DATA)
                val albumId = musicCursor.getColumnIndex(AudioColumns.ALBUM_ID)
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
                    allDeviceSongs.add(
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
            val filteredSongs =
                allDeviceSongs.distinctBy { it.artist to it.year to it.track to it.title to it.duration to it.album to it.albumId }
                    .toMutableList()

            val musicSortedByArtist = filteredSongs.groupBy { it.artist }

            musicSortedByArtist.keys.iterator().forEach {
                val albums = musicSortedByArtist[it]?.groupBy { song -> song.album }
                categorizedMusicByAlbum[it.toString()] = albums!!
            }

            Pair(allDeviceSongs, categorizedMusicByAlbum)

        } catch (e: Exception) {
            Utils.makeUnknownErrorToast(context, R.string.error_unknown)
            e.printStackTrace()
            Pair(mutableListOf(), mapOf())
        }
    }

    @Suppress("DEPRECATION")
    companion object {

        @JvmStatic
        @SuppressLint("InlinedApi")
        private val COLUMNS = arrayOf(
            AudioColumns.ARTIST, // 0
            AudioColumns.YEAR, // 1
            AudioColumns.TRACK, // 2
            AudioColumns.TITLE, // 3
            AudioColumns.DURATION, // 4
            AudioColumns.ALBUM, // 5
            AudioColumns.DATA, // 6
            AudioColumns.ALBUM_ID //7
        )

        @JvmStatic
        private fun getSongLoaderSortOrder(): String {
            return MediaStore.Audio.Artists.DEFAULT_SORT_ORDER + ", " + MediaStore.Audio.Albums.DEFAULT_SORT_ORDER + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        }

        @JvmStatic
        private fun getMusicCursor(contentResolver: ContentResolver): Cursor? {
            return contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                COLUMNS, null, null,
                getSongLoaderSortOrder()
            )
        }
    }
}
