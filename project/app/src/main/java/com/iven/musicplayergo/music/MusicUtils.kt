package com.iven.musicplayergo.music

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.res.Resources
import android.database.Cursor
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.musicLibrary
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.Utils
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


object MusicUtils {

    @JvmStatic
    fun getContentUri(audioId: Long): Uri {
        return ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            audioId
        )
    }

    @JvmStatic
    private fun getAudioFileDescriptor(
        contentUri: Uri,
        contentResolver: ContentResolver
    ): ParcelFileDescriptor? {
        val readOnlyMode = "r"
        return contentResolver.openFileDescriptor(contentUri, readOnlyMode)
    }

    @JvmStatic
    fun getSavedSong(): Music? {
        return try {
            goPreferences.latestPlayedSong?.first
        } catch (e: Exception) {
            e.printStackTrace()
            musicLibrary.randomMusic
        }
    }

    //returns the position in list of the current played album
    //pass selected artist from artists adapter and not from current song
    //so when played artist is selected the album position will be returned
    //if selected artist differs from played artist -1 will be returned
    @JvmStatic
    fun getPlayingAlbumPosition(
        selectedArtist: String?,
        mediaPlayerHolder: MediaPlayerHolder
    ): Int {
        return try {
            val currentSong = mediaPlayerHolder.currentSong.first
            val album = getAlbumFromList(selectedArtist, currentSong?.album)
            album.second
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    @JvmStatic
    //returns a pair of album and its position given a list of albums
    fun getAlbumFromList(artist: String?, album: String?): Pair<Album, Int> {
        val albums = musicLibrary.allAlbumsByArtist[artist]
        return try {
            val position = albums?.indexOfFirst { it.title == album }!!
            Pair(albums[position], position)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(albums?.get(0)!!, 0)
        }
    }

    @JvmStatic
    fun getAlbumSongs(artist: String?, album: String?): List<Music>? {
        return getAlbumFromList(artist, album).first.music
    }

    @JvmStatic
    fun getSongForIntent(
        displayName: String?,
        allDeviceSongs: List<Music>?
    ): Music? {
        return allDeviceSongs?.firstOrNull { s -> s.displayName == displayName }
    }

    @JvmStatic
    fun getYearForAlbum(resources: Resources, year: Int): String {
        return if (year != 0) year.toString() else resources.getString(R.string.unknown_year)
    }

    @JvmStatic
    fun buildSortedArtistAlbums(
        resources: Resources,
        artistSongs: List<Music>
    ): List<Album> {

        val sortedAlbums = mutableListOf<Album>()

        try {

            val groupedSongs = artistSongs.groupBy { song -> song.album }

            groupedSongs.keys.iterator().forEach {

                val albumSongs = groupedSongs.getValue(it).toMutableList()
                albumSongs.sortBy { song -> song.track }

                sortedAlbums.add(
                    Album(
                        it,
                        getYearForAlbum(resources, albumSongs[0].year),
                        albumSongs,
                        albumSongs.map { song -> song.duration }.sum()
                    )
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        sortedAlbums.sortBy { it.year }
        return sortedAlbums
    }

    @JvmStatic
    fun formatSongDuration(duration: Long?, isAlbum: Boolean): String {
        val defaultFormat = if (isAlbum) "%02dm:%02ds" else "%02d:%02d"

        val hours = TimeUnit.MILLISECONDS.toHours(duration!!)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration)

        return if (minutes < 60) String.format(
            Locale.getDefault(), defaultFormat,
            minutes,
            seconds - TimeUnit.MINUTES.toSeconds(minutes)
        ) else
        //https://stackoverflow.com/a/9027379
            String.format(
                "%02dh:%02dm",
                hours,
                minutes - TimeUnit.HOURS.toMinutes(hours), // The change is in this line
                seconds - TimeUnit.MINUTES.toSeconds(minutes)
            )
    }

    @JvmStatic
    fun formatSongTrack(trackNumber: Int): Int {
        var formatted = trackNumber
        if (trackNumber >= 1000) formatted = trackNumber % 1000
        return formatted
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
        AudioColumns.ALBUM_ID, //8
        AudioColumns._ID //9
    )

    @JvmStatic
    fun getMusicCursor(contentResolver: ContentResolver): Cursor? {
        return contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            COLUMNS, AudioColumns.IS_MUSIC + "=1", null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        )
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun getPathColumn(): String {
        return if (Utils.isAndroidQ()) AudioColumns.RELATIVE_PATH else AudioColumns.DATA
    }

    @JvmStatic
    fun getBitrate(contentUri: Uri, contentResolver: ContentResolver): Pair<Int, Int>? {
        val mediaExtractor = MediaExtractor()
        return try {

            getAudioFileDescriptor(contentUri, contentResolver)?.use { pfd ->
                mediaExtractor.setDataSource(pfd.fileDescriptor)
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

    @JvmStatic
    fun getFolderName(path: String?): String? {
        return File(path!!).parentFile?.name
    }
}
