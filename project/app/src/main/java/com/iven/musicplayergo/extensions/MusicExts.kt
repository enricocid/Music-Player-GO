package com.iven.musicplayergo.extensions

import android.content.ContentUris
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil.Coil
import coil.request.ImageRequest
import com.iven.musicplayergo.R
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


fun MediaPlayerHolder.startSongFromQueue(song: Music?, launchedBy: String) {
    if (isSongRestoredFromPrefs) {
        isSongRestoredFromPrefs = false
    }
    isPlay = true

    if (isQueue == null) {
        isQueue = currentSong
    }
    if (!isQueueStarted) {
        isQueueStarted = true
        mediaPlayerInterface.onQueueStartedOrEnded(started = true)
    }

    setCurrentSong(
        song,
        null,
        launchedBy
    )
    initMediaPlayer(song)
}

//https://codereview.stackexchange.com/a/97819
fun String?.toFilenameWithoutExtension() = try {
    Pattern.compile("(?<=.)\\.[^.]+$").matcher(this!!).replaceAll("")
} catch (e: Exception) {
    e.printStackTrace()
    this
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

fun Long.toAlbumArtURI(): Uri {
    return ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), this)
}

fun Long.waitForCover(context: Context, onDone: (Bitmap?) -> Unit) {
    val defaultAlbumArt = ContextCompat.getDrawable(context, R.drawable.album_art)?.toBitmap()
    Coil.imageLoader(context).enqueue(
        ImageRequest.Builder(context)
            .data(toAlbumArtURI())
            .target(
                onSuccess = { onDone(it.toBitmap()) },
                onError = { onDone(defaultAlbumArt) }
            )
            .build()
    )
}

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

fun Int.toFormattedDate(): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val netDate = Date(this.toLong() * 1000)
        sdf.format(netDate)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
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
        albumId,
        relativePath,
        id,
        savedLaunchedBy,
        playerPosition,
        dateAdded
    )

fun List<Music>.savedSongIsAvailable(first: Music?) : Music? {
    first?.let { song ->
        return find { song.title == it.title && song.displayName == it.displayName && song.track == it.track && song.albumId == it.albumId && song.album == it.album }
    }
    return null
}

