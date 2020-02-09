package com.iven.musicplayergo.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.res.Resources
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import com.iven.musicplayergo.musicLibrary
import com.iven.musicplayergo.musicloadutils.Album
import com.iven.musicplayergo.musicloadutils.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.toFormattedYear


object MusicUtils {

    @JvmStatic
    fun getContentUri(audioId: Long?) = audioId?.let {
        ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            audioId
        )
    }

    @JvmStatic
    private fun getAudioFileDescriptor(
        contentUri: Uri,
        contentResolver: ContentResolver
    ) = contentResolver.openFileDescriptor(contentUri, "r")

    //returns the position in list of the current played album
    //pass selected artist from artists adapter and not from current song
    //so when played artist is selected the album position will be returned
    //if selected artist differs from played artist -1 will be returned
    @JvmStatic
    fun getPlayingAlbumPosition(
        selectedArtist: String?,
        mediaPlayerHolder: MediaPlayerHolder
    ) = try {
        val currentSong = mediaPlayerHolder.currentSong.first
        val album = getAlbumFromList(
            selectedArtist,
            currentSong?.album
        )
        album.second
    } catch (e: Exception) {
        e.printStackTrace()
        -1
    }

    @JvmStatic
    //returns a pair of album and its position given a list of albums
    fun getAlbumFromList(artist: String?, album: String?): Pair<Album, Int> {
        val albums = musicLibrary.allAlbumsByArtist?.get(artist)
        return try {
            val position = albums?.indexOfFirst { it.title == album }!!
            Pair(albums[position], position)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(albums?.get(0)!!, 0)
        }
    }

    @JvmStatic
    fun getAlbumSongs(artist: String?, album: String?) = getAlbumFromList(
        artist,
        album
    ).first.music

    @JvmStatic
    fun getSongForIntent(
        displayName: String?
    ): Music? = musicLibrary.allSongsUnfiltered.firstOrNull { s -> s.displayName == displayName }

    @JvmStatic
    fun buildSortedArtistAlbums(
        resources: Resources,
        artistSongs: List<Music>?
    ): List<Album> {

        val sortedAlbums = mutableListOf<Album>()

        artistSongs?.let {

            try {

                val groupedSongs = it.groupBy { song -> song.album }

                groupedSongs.keys.iterator().forEach { album ->

                    val albumSongs = groupedSongs.getValue(album).toMutableList()
                    albumSongs.sortBy { song -> song.track }

                    sortedAlbums.add(
                        Album(
                            album,
                            albumSongs[0].year.toFormattedYear(resources),
                            albumSongs,
                            albumSongs.map { song -> song.duration }.sum()
                        )
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
            sortedAlbums.sortBy { album -> album.year }
        }

        return sortedAlbums
    }

    @JvmStatic
    @SuppressLint("InlinedApi")
    private val COLUMNS = arrayOf(
        AudioColumns.ARTIST, // 0
        AudioColumns.YEAR, // 1
        AudioColumns.TRACK, // 2
        AudioColumns.TITLE, // 3
        AudioColumns.DISPLAY_NAME, // 4,
        AudioColumns.DURATION, //5,
        AudioColumns.ALBUM, // 6
        getPathColumn(), // 7
        AudioColumns._ID //8
    )

    @JvmStatic
    fun getMusicCursor(contentResolver: ContentResolver) = contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        COLUMNS, AudioColumns.IS_MUSIC + "=1", null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER
    )

    @JvmStatic
    @Suppress("DEPRECATION")
    fun getPathColumn() =
        if (Utils.isAndroidQ()) AudioColumns.BUCKET_DISPLAY_NAME else AudioColumns.DATA

    @JvmStatic
    fun getBitrate(contentUri: Uri?, contentResolver: ContentResolver): Pair<Int, Int>? {
        val mediaExtractor = MediaExtractor()
        return try {

            contentUri?.let { uri ->
                getAudioFileDescriptor(
                    uri,
                    contentResolver
                )?.use { pfd ->
                    mediaExtractor.setDataSource(pfd.fileDescriptor)
                }
            }

            val mediaFormat = mediaExtractor.getTrackFormat(0)

            val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            //get bitrate in bps, divide by 1000 to get Kbps
            val bitrate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
            Pair(sampleRate, bitrate)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
