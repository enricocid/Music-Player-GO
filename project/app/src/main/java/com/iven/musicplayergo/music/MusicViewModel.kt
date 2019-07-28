package com.iven.musicplayergo.music

import android.database.Cursor
import android.os.AsyncTask
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MusicViewModel : ViewModel() {

    //Async task to load music from the device
    private class LoadMusicTask internal constructor(
        private val musicCursor: Cursor,
        private val musicLiveData: MusicLiveData
    ) :
        AsyncTask<Void, Void, Pair<MutableList<Music>, Map<String, Map<String, List<Music>>>>>() {

        override fun onPostExecute(result: Pair<MutableList<Music>, Map<String, Map<String, List<Music>>>>?) {
            super.onPostExecute(result)

            musicLiveData.value = result
        }

        override fun doInBackground(vararg params: Void?): Pair<MutableList<Music>, Map<String, Map<String, List<Music>>>>? {

            // Initialize an empty mutable list of music
            val mAllDeviceSongs = mutableListOf<Music>()

            try {
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
                        mAllDeviceSongs.add(
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

            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Finally, return the music files list
            return Pair(mAllDeviceSongs, categorizeMusicByArtistAndAlbums(mAllDeviceSongs))
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
    }

    class MusicLiveData(private val musicCursor: Cursor) :
        MutableLiveData<Pair<MutableList<Music>, Map<String, Map<String, List<Music>>>>>() {

        init {
            loadMusic()
        }

        // Extension method to get all music files list from external storage/sd card
        private fun loadMusic() {
            LoadMusicTask(musicCursor, this).execute()
        }
    }

    fun getMusic(musicCursor: Cursor): MutableLiveData<Pair<MutableList<Music>, Map<String, Map<String, List<Music>>>>> {
        return MusicLiveData(musicCursor)
    }
}
