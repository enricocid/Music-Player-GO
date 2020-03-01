package com.iven.musicplayergo

import android.annotation.SuppressLint
import android.app.Application
import android.content.res.Resources
import android.provider.MediaStore
import com.iven.musicplayergo.helpers.MusicOrgHelper
import com.iven.musicplayergo.helpers.VersioningHelper
import com.iven.musicplayergo.models.Album
import com.iven.musicplayergo.models.Music
import java.io.File

class MusicRepository {

    var deviceMusicList = mutableListOf<Music>()

    fun getSongFromIntent(queriedDisplayName: String) =
        deviceMusicList.firstOrNull { s -> s.displayName == queriedDisplayName }

    var deviceMusicFiltered: MutableList<Music>? = null

    //keys: artist || value: its songs
    var deviceSongsByArtist: Map<String?, List<Music>>? = null

    //keys: artist || value: albums
    var deviceAlbumsByArtist: MutableMap<String, List<Album>>? = null

    //keys: artist || value: songs contained in the folder
    var deviceMusicByFolder: Map<String, List<Music>>? = null

    val randomMusic get() = deviceMusicList.random()

    val musicDatabaseSize get() = deviceMusicFiltered?.size

    init {
        if (deviceAlbumsByArtist == null) deviceAlbumsByArtist = mutableMapOf()
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
                    val audioTitle = cursor.getString(titleIndex)
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
                            if (returnedPath != "0") returnedPath else application.getString(
                                R.string.slash
                            )
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
                            audioId
                        )
                    )
                }
            }

            deviceMusicList

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    fun getDeviceMusic(application: Application): MutableList<Music> {
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
            deviceMusicByFolder = dsf.groupBy { it.relativePath!! }
        }

        // group artists songs by albums
        deviceSongsByArtist?.keys?.iterator()?.forEach { artist ->
            artist?.let { artistKey ->
                deviceAlbumsByArtist?.set(
                    artistKey, MusicOrgHelper.buildSortedArtistAlbums(
                        resources,
                        deviceSongsByArtist?.getValue(artist)
                    )
                )
            }
        }
    }

    companion object {

        @Volatile
        private var INSTANCE: MusicRepository? = null

        fun getInstance(): MusicRepository {
            val tempInstance = INSTANCE
            if (tempInstance != null) return tempInstance
            synchronized(this) {
                val instance = MusicRepository()
                INSTANCE = instance
                return instance
            }
        }
    }
}
