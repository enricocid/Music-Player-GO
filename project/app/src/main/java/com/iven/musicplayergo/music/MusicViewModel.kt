package com.iven.musicplayergo.music

import android.database.Cursor
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MusicViewModel : ViewModel() {

    //Load music from the device

    private lateinit var music: MutableLiveData<Pair<MutableList<Music>, Map<String, Map<String, List<Music>>>>>
    private lateinit var allDeviceSongs: MutableList<Music>


    fun getMusic(musicCursor: Cursor): MutableLiveData<Pair<MutableList<Music>, Map<String, Map<String, List<Music>>>>> {

        if (!::music.isInitialized) {
            music = MutableLiveData()
            loadMusic(musicCursor)
        }
        return music
    }

    private fun categorizeMusicByArtistAndAlbums(music: List<Music>): Map<String, Map<String, List<Music>>> {

        val musicSortedByArtist = music.groupBy { it.artist }

        val categorizedMusicByAlbum = hashMapOf<String, Map<String, List<Music>>>()
        val artists = musicSortedByArtist.keys.toMutableList()

        artists.forEachIndexed { _, artist ->
            val songsForArtist = musicSortedByArtist[artist]?.toMutableList()
            val albums = songsForArtist?.groupBy { it.album!! }
            categorizedMusicByAlbum[artist.toString()] = albums!!
        }
        return categorizedMusicByAlbum
    }

    // Extension method to get all music files list from external storage/sd card
    private fun loadMusic(musicCursor: Cursor) {
        // Initialize an empty mutable list of music
        allDeviceSongs = mutableListOf()

        // Query the external storage for music files

        // If query result is not empty
        if (musicCursor.moveToFirst()) {
            val artist = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val year = musicCursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val track = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val title = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val duration = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val album = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val path = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            // Now loop through the music files
            do {
                val audioArtist = musicCursor.getString(artist)
                val audioYear = musicCursor.getInt(year)
                val audioTrack = musicCursor.getInt(track)
                val audioTitle = musicCursor.getString(title)
                val audioDuration = musicCursor.getLong(duration)
                val audioAlbum = musicCursor.getString(album)
                val audioPath = musicCursor.getString(path)

                // Add the current music to the list
                allDeviceSongs.add(
                    Music(
                        audioArtist,
                        audioYear,
                        audioTrack,
                        audioTitle,
                        audioDuration,
                        audioAlbum,
                        audioPath
                    )
                )
            } while (musicCursor.moveToNext())
            musicCursor.close()
        }

        // Finally, return the music files list
        music.value = Pair(allDeviceSongs, categorizeMusicByArtistAndAlbums(allDeviceSongs))
    }
}
