package com.iven.musicplayergo

import android.annotation.SuppressLint
import android.app.Application
import android.content.res.Resources
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.iven.musicplayergo.helpers.MusicOrgHelper
import com.iven.musicplayergo.helpers.VersioningHelper
import com.iven.musicplayergo.models.Album
import com.iven.musicplayergo.models.Music
import kotlinx.coroutines.*
import java.io.File

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * This is the job for all coroutines started by this ViewModel.
     * Cancelling this job will cancel all coroutines started by this ViewModel.
     */
    private val viewModelJob = SupervisorJob()

    private val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
        deviceMusic.value = null
    }

    private val uiDispatcher = Dispatchers.Main
    private val ioDispatcher = Dispatchers.IO + viewModelJob + handler
    private val uiScope = CoroutineScope(uiDispatcher)

    val deviceMusic = MutableLiveData<MutableList<Music>?>()

    private var deviceMusicList = mutableListOf<Music>()

    fun getSongFromIntent(queriedDisplayName: String) =
        deviceMusicList.firstOrNull { s -> s.displayName == queriedDisplayName }

    var deviceMusicFiltered: MutableList<Music>? = null

    //keys: artist || value: its songs
    var deviceSongsByArtist: Map<String?, List<Music>>? = null

    //keys: album || value: its songs
    var deviceMusicByAlbum: Map<String?, List<Music>>? = null

    //keys: artist || value: albums
    var deviceAlbumsByArtist: MutableMap<String, List<Album>>? = mutableMapOf()

    //keys: artist || value: songs contained in the folder
    var deviceMusicByFolder: Map<String, List<Music>>? = null

    val randomMusic get() = deviceMusicList.random()

    val musicDatabaseSize get() = deviceMusicFiltered?.size

    /**
     * Cancel all coroutines when the ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun cancel() {
        onCleared()
    }

    fun getDeviceMusic() {
        uiScope.launch {
            withContext(ioDispatcher) {
                val music = getMusic(getApplication()) // get music from MediaStore on IO thread
                withContext(uiDispatcher) {
                    deviceMusic.value = music // post values on Main thread
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    fun queryForMusic(application: Application) =

        try {

            val musicCursor =
                MusicOrgHelper.getMusicCursor(
                    application.contentResolver
                )

            // Query the storage for music files
            musicCursor?.use { cursor ->

                val idIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
                val artistIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)
                val yearIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.YEAR)
                val trackIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TRACK)
                val titleIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
                val displayNameIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
                val durationIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
                val albumIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM)
                val relativePathIndex =
                    cursor.getColumnIndexOrThrow(MusicOrgHelper.getPathColumn())

                while (cursor.moveToNext()) {

                    // Now loop through the music files
                    val audioId = cursor.getLong(idIndex)
                    val audioArtist = cursor.getString(artistIndex)
                    val audioYear = cursor.getInt(yearIndex)
                    val audioTrack = cursor.getInt(trackIndex)
                    var audioTitle = cursor.getString(titleIndex)
                    val audioDisplayName = cursor.getString(displayNameIndex)
                    val audioDuration = cursor.getLong(durationIndex)
                    val audioAlbum = cursor.getString(albumIndex)
                    val audioRelativePath = cursor.getString(relativePathIndex)

                    val audioFolderName =
                        if (VersioningHelper.isQ()) {
                            audioRelativePath ?: application.getString(R.string.slash)
                        } else {
                            val returnedPath = File(audioRelativePath).parentFile?.name
                                ?: application.getString(R.string.slash)
                            if (returnedPath != "0") {
                                returnedPath
                            } else {
                                application.getString(
                                    R.string.slash
                                )
                            }
                        }

                    if (audioTitle.isEmpty()) {
                        audioTitle = audioFolderName
                    }

                    // Add the current music to the list
                    deviceMusicList.add(
                        Music(
                            audioArtist,
                            audioYear,
                            audioTrack,
                            audioTitle,
                            audioDisplayName,
                            audioDuration,
                            audioAlbum,
                            audioFolderName,
                            audioId,
                            GoConstants.ARTIST_VIEW,
                            0
                        )
                    )
                }
            }

            deviceMusicList

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    private fun getMusic(application: Application): MutableList<Music> {
        queryForMusic(application)?.let { fm ->
            deviceMusicList = fm
        }
        buildLibrary(application.resources)
        return deviceMusicList
    }

    private fun buildLibrary(resources: Resources) {
        // Removing duplicates by comparing everything except path which is different
        // if the same song is hold in different paths
        deviceMusicFiltered =
            deviceMusicList.distinctBy { it.artist to it.year to it.track to it.title to it.duration to it.album }
                .toMutableList()

        deviceMusicFiltered?.let { dsf ->
            // group music by artist
            deviceSongsByArtist = dsf.groupBy { it.artist }
            deviceMusicByAlbum = dsf.groupBy { it.album }
            deviceMusicByFolder = dsf.groupBy { it.relativePath!! }
        }

        // group artists songs by albums
        deviceSongsByArtist?.keys?.iterator()?.let { iterate ->
            while (iterate.hasNext()) {
                iterate.next()?.let { artistKey ->
                    val album = deviceSongsByArtist?.getValue(artistKey)
                    deviceAlbumsByArtist?.set(
                        artistKey, MusicOrgHelper.buildSortedArtistAlbums(
                            resources,
                            album
                        )
                    )
                }
            }
        }
    }
}
