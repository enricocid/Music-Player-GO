package com.iven.musicplayergo.music

import android.database.Cursor
import android.provider.MediaStore.Audio.AudioColumns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MusicViewModel : ViewModel() {

    //first: all device songs, second: Map with key: artist, value: Map with key: album, value: songs
    private val musicLiveData: MutableLiveData<Pair<MutableList<Music>, Map<String, Map<String?, List<Music>>>>> =
        MutableLiveData()

    private val allDeviceSongs: MutableList<Music> = mutableListOf()

    //key: artist || key: album, value: album songs
    private val categorizedMusicByAlbum = hashMapOf<String, Map<String?, List<Music>>>()

    //Load music from the device
    fun getMusic(musicCursor: Cursor): MutableLiveData<Pair<MutableList<Music>, Map<String, Map<String?, List<Music>>>>> {
        return loadMusic(musicCursor)
    }

    //Build a Map with key: artist, value: Map with key: album, value: songs
    private fun categorizeMusicByArtistAndAlbums(music: List<Music>): Map<String, Map<String?, List<Music>>> {

        val musicSortedByArtist = music.groupBy { it.artist }

        musicSortedByArtist.keys.iterator().forEach {
            val albums = musicSortedByArtist[it]?.groupBy { song -> song.album }
            categorizedMusicByAlbum[it.toString()] = albums!!
        }

        return categorizedMusicByAlbum
    }

    // Extension method to get all music files list from external storage/sd card
    private fun loadMusic(musicCursor: Cursor): MutableLiveData<Pair<MutableList<Music>, Map<String, Map<String?, List<Music>>>>> {

        // Query the external storage for music files

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

        // Finally, return the music files list
        musicLiveData.value = Pair(filteredSongs, categorizeMusicByArtistAndAlbums(filteredSongs))

        return musicLiveData
    }
}
