package com.iven.musicplayergo.music

import android.annotation.SuppressLint
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.ui.Utils
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MusicViewModel : ViewModel(), CoroutineScope {

    private val musicLiveData: MutableLiveData<MutableList<Music>> = MutableLiveData()

    private val loadDeviceMusicJob = Job()

    override val coroutineContext: CoroutineContext
        get() = loadDeviceMusicJob + Dispatchers.Main + handler

    private val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    fun loadAllDeviceMusic(context: Context): MutableLiveData<MutableList<Music>> {
        return go(context)
    }

    // Extension method to get all music files list from external storage/sd card
    @Suppress("DEPRECATION")
    @SuppressLint("InlinedApi")
    fun go(context: Context): MutableLiveData<MutableList<Music>> {

        val allSongsUnfiltered = mutableListOf<Music>()

        launch {
            try {

                withContext(Dispatchers.Main) {

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

                            } while (it.moveToNext())
                            it.close()
                        }

                        musicLiveData.value = allSongsUnfiltered
                    }
                }
            } catch (e: Exception) {
                Utils.makeToast(context, context.getString(R.string.error_unknown))
                e.printStackTrace()
            }
        }
        return musicLiveData
    }
}
