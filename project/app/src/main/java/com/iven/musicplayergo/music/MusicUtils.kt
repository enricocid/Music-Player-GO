package com.iven.musicplayergo.music

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.text.Html
import android.text.Spanned
import com.iven.musicplayergo.R
import com.iven.musicplayergo.ui.Utils
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
object MusicUtils {

    @JvmStatic
    fun getArtists(music: Map<String, Map<String?, List<Music>>>): MutableList<String> {
        val artists = music.keys.toMutableList()
        artists.sort()
        return artists
    }

    @JvmStatic
    fun getArtistSongsCount(albums: Map<String?, List<Music>>): Int {
        var songsCount = 0
        try {
            albums.keys.iterator().forEach {
                songsCount += albums.getValue(it).size
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songsCount
    }

    @JvmStatic
    fun getAlbumPositionInList(album: String?, albums: List<Album>): Int {
        return try {
            albums.indexOfFirst { it.title == album }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    @JvmStatic
    fun getSongForIntent(
        path: String?,
        selectedArtistSongs: List<Music>,
        allDeviceSongs: List<Music>
    ): Music? {

        return try {
            selectedArtistSongs.first { s -> s.path == path }
        } catch (e: Exception) {
            allDeviceSongs.firstOrNull { s -> s.path == path }
        }
    }

    @JvmStatic
    fun getArtistSongs(albums: Map<String?, List<Music>>): MutableList<Music> {
        val artistSongs = mutableListOf<Music>()
        try {
            albums.keys.iterator().forEach {
                artistSongs.addAll(albums.getValue(it))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return artistSongs
    }

    @JvmStatic
    fun getYearForAlbum(resources: Resources, year: Int): String {
        return if (year == 0) resources.getString(R.string.unknown_year) else year.toString()
    }

    @JvmStatic
    fun buildSortedArtistAlbums(
        resources: Resources,
        albums: Map<String?, List<Music>>
    ): List<Album> {

        val sortedAlbums = mutableListOf<Album>()

        try {
            albums.keys.iterator().forEach {
                val albumSongs = albums.getValue(it)
                sortedAlbums.add(
                    Album(
                        it,
                        getYearForAlbum(resources, albumSongs[0].year),
                        albumSongs
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
    fun formatSongDuration(duration: Long): String {
        return String.format(
            Locale.getDefault(), "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(duration),
            TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(
                    duration
                )
            )
        )
    }

    @JvmStatic
    fun formatSongTrack(trackNumber: Int): Int {
        var formatted = trackNumber
        if (trackNumber >= 1000) formatted = trackNumber % 1000
        return formatted
    }

    @JvmStatic
    fun buildSpanned(res: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(res, Html.FROM_HTML_MODE_LEGACY)
        else
            Html.fromHtml(res)
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
    private fun getSongLoaderSortOrder(): String {
        return MediaStore.Audio.Artists.DEFAULT_SORT_ORDER + ", " + MediaStore.Audio.Albums.DEFAULT_SORT_ORDER + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER
    }

    @JvmStatic
    fun getMusicCursor(contentResolver: ContentResolver): Cursor? {
        return contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            COLUMNS, null, null, getSongLoaderSortOrder()
        )
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

            if (cursor!!.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                do {
                    returnedString = cursor.getString(index)
                } while (cursor.moveToNext())
                cursor.close()
            }
        } catch (e: Exception) {
            Utils.makeUnknownErrorToast(context, R.string.error_unknown)
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
