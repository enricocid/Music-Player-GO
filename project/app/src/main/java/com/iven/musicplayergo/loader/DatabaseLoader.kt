package com.iven.musicplayergo.loader

import android.annotation.SuppressLint
import android.content.Context
import android.provider.MediaStore
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.library.MusicDatabase
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.musicLibrary
import com.iven.musicplayergo.utils.MusicUtils
import com.iven.musicplayergo.utils.Utils
import java.io.File

class DatabaseLoader(
    context: Context
) : WrappedAsyncTaskLoader<Any?>(context) {

    private val queriedMusic = mutableListOf<Music>()

    // This is where background code is executed
    @SuppressLint("InlinedApi")
    override fun loadInBackground(): MusicDatabase? {

        return MusicDatabase.getDatabase(
            context
        ).apply {

            val music = musicDao().getAll()

            if (!music.isNullOrEmpty() && !goPreferences.reloadDB)
                musicLibrary.buildMusicLibrary(context, music)
            else

                try {

                    val musicCursor =
                        MusicUtils.getMusicCursor(
                            context.contentResolver
                        )

                    // Query the storage for music files
                    // If query result is not empty
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
                            cursor.getColumnIndexOrThrow(MusicUtils.getPathColumn())

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
                                if (Utils.isAndroidQ()) {
                                    audioRelativePath ?: context.getString(R.string.slash)
                                } else {
                                    val returnedPath = File(audioRelativePath).parentFile?.name
                                        ?: context.getString(R.string.slash)
                                    if (returnedPath != "0") returnedPath else context.getString(
                                        R.string.slash
                                    )
                                }

                            // Add the current music to the database
                            queriedMusic.add(
                                Music(
                                    audioId,
                                    audioArtist,
                                    audioYear,
                                    audioTrack,
                                    audioTitle,
                                    audioDisplayName,
                                    audioDuration,
                                    audioAlbum,
                                    audioFolderName
                                )
                            )
                        }
                    }

                    musicDao().updateData(queriedMusic)

                    musicLibrary.buildMusicLibrary(context, queriedMusic)

                } catch (e: Throwable) {
                    e.printStackTrace()
                    return null
                }
        }
    }
}
