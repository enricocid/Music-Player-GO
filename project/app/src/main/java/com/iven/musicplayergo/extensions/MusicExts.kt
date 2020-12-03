package com.iven.musicplayergo.extensions

import android.content.ContentUris
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

fun MediaPlayerHolder.startSongFromQueue(song: Music?) {
    if (isSongRestoredFromPrefs) {
        isSongRestoredFromPrefs = false
    }
    isPlay = true
    if (!isQueueStarted) {
        isQueueStarted = true
        mediaPlayerInterface.onQueueStartedOrEnded(true)
    }
    setCurrentSong(
        song,
        queueSongs,
        isFromQueue = true,
        isFolderAlbum = GoConstants.ARTIST_VIEW
    )
    initMediaPlayer(song)
}

fun Long.toContentUri(): Uri = ContentUris.withAppendedId(
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
    this
)

fun Uri.toBitrate(context: Context): Pair<Int, Int>? {
    val mediaExtractor = MediaExtractor()
    return try {

        mediaExtractor.setDataSource(context, this, null)

        val mediaFormat = mediaExtractor.getTrackFormat(0)

        val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        // Get bitrate in bps, divide by 1000 to get Kbps
        val bitrate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
        Pair(sampleRate, bitrate)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        mediaExtractor.release()
    }
}

fun Music.getCover(context: Context): Bitmap? {
    val contentUri = id?.toContentUri()
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, contentUri)

        val picture = retriever.embeddedPicture

        if (picture != null) {
            BitmapFactory
                .decodeByteArray(picture, 0, picture.size)
        } else {
            null
        }
    } finally {
        retriever.release()
    }
}

fun IntRange.getRandom() = Random.nextInt(start, endInclusive + 1)

fun Long.toFormattedDuration(isAlbum: Boolean, isSeekBar: Boolean) = try {

    val defaultFormat = if (isAlbum) {
        "%02dm:%02ds"
    } else {
        "%02d:%02d"
    }

    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this)

    if (minutes < 60) {
        String.format(
            Locale.getDefault(), defaultFormat,
            minutes,
            seconds - TimeUnit.MINUTES.toSeconds(minutes)
        )
    } else {
        // https://stackoverflow.com/a/9027379
        when {
            isSeekBar -> String.format(
                "%02d:%02d:%02d",
                hours,
                minutes - TimeUnit.HOURS.toMinutes(hours),
                seconds - TimeUnit.MINUTES.toSeconds(minutes)
            )
            else -> String.format(
                "%02dh:%02dm",
                hours,
                minutes - TimeUnit.HOURS.toMinutes(hours)
            )
        }
    }

} catch (e: Exception) {
    e.printStackTrace()
    ""
}

fun Int.toFormattedTrack() = try {
    if (this >= 1000) {
        this % 1000
    } else {
        this
    }
} catch (e: Exception) {
    e.printStackTrace()
    0
}

fun Int.toFormattedYear(resources: Resources) =
    if (this != 0) {
        toString()
    } else {
        resources.getString(R.string.unknown_year)
    }

fun Music.toSavedMusic(playerPosition: Int, savedLaunchedBy: String) =
    Music(
        artist,
        year,
        track,
        title,
        displayName,
        duration,
        album,
        relativePath,
        id,
        savedLaunchedBy,
        playerPosition
    )
