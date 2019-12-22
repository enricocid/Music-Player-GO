package com.iven.musicplayergo.music

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.widget.Toast
import com.iven.musicplayergo.R
import com.iven.musicplayergo.musicLibrary
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.Utils
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


@Suppress("DEPRECATION")
object MusicUtils {

    //returns the position in list of the current played album
    //pass selected artist from artists adapter and not from current song
    //so when played artist is selected the album position will be returned
    //if selected artist differs from played artist -1 will be returned
    @JvmStatic
    fun getPlayingAlbumPosition(selectedArtist: String, mediaPlayerHolder: MediaPlayerHolder): Int {
        return try {
            val currentSong = mediaPlayerHolder.currentSong.first
            val albumsForArtist = musicLibrary.allAlbumsByArtist.getValue(selectedArtist)
            val album = getAlbumFromList(currentSong.album, albumsForArtist)
            album.second
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    @JvmStatic
    //returns a pair of album and its position given a list of albums
    fun getAlbumFromList(album: String?, albums: List<Album>): Pair<Album, Int> {
        return try {
            val position = albums.indexOfFirst { it.title == album }
            Pair(albums[position], position)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(albums[0], 0)
        }
    }

    @JvmStatic
    fun getSongForIntent(
        path: String?,
        allDeviceSongs: List<Music>
    ): Music? {
        return allDeviceSongs.firstOrNull { s -> s.path == path }
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
    fun formatSongDuration(duration: Long, isAlbum: Boolean): String {
        val defaultFormat = if (isAlbum) "%02dm:%02ds" else "%02d:%02d"

        val hours = TimeUnit.MILLISECONDS.toHours(duration)
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
        AudioColumns.DURATION, // 4
        AudioColumns.ALBUM, // 5
        AudioColumns.DATA, // 6
        AudioColumns.ALBUM_ID //7
    )

    @JvmStatic
    fun getMusicCursor(contentResolver: ContentResolver): Cursor? {
        return contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            COLUMNS, AudioColumns.IS_MUSIC + "=1", null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        )
    }

    @JvmStatic
    fun getBitrate(path: String): Pair<Int, Int>? {
        val mediaExtractor = MediaExtractor()
        return try {
            mediaExtractor.setDataSource(path)
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
    fun getFolderName(path: String?): String {
        val file = File(path!!).parentFile
        return file?.name!!
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     * https://gist.github.com/tatocaster/32aad15f6e0c50311626
     */
    @JvmStatic
    fun getRealPathFromURI(context: Context, uri: Uri): String? {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {

                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(
                        type,
                        ignoreCase = true
                    )
                ) return Environment.getExternalStorageDirectory().toString() + "/" + split[1]

            } else if (isMediaDocument(uri)) {

                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }// MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {

            // Return the remote address
            return getDataColumn(context, uri, null, null)

        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    @JvmStatic
    private fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {

        var returnedString: String? = null

        try {

            val column = "_data"
            val projection = arrayOf(column)

            val cursor =
                context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)

            cursor?.let {
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    do {
                        returnedString = cursor.getString(index)
                    } while (cursor.moveToNext())
                    cursor.close()
                }
            }


        } catch (e: Exception) {
            Utils.makeToast(
                context,
                context.getString(R.string.error_unknown_unsupported),
                Toast.LENGTH_LONG
            )
            e.printStackTrace()
        }
        return returnedString
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    @JvmStatic
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    @JvmStatic
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
}
