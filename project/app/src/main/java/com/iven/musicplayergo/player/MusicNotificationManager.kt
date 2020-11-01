package com.iven.musicplayergo.player

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.enums.LaunchedBy
import com.iven.musicplayergo.extensions.getCover
import com.iven.musicplayergo.extensions.toSpanned
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.helpers.VersioningHelper
import com.iven.musicplayergo.ui.MainActivity

// Notification params
private const val CHANNEL_ID = "CHANNEL_ID_GO"
private const val REQUEST_CODE = 100

class MusicNotificationManager(private val playerService: PlayerService) {

    //notification manager/builder
    private val mNotificationManager = NotificationManagerCompat.from(playerService)
    private lateinit var mNotificationBuilder: NotificationCompat.Builder

    private val mNotificationActions
        @SuppressLint("RestrictedApi")
        get() = mNotificationBuilder.mActions

    private fun playerAction(action: String): PendingIntent {

        val pauseIntent = Intent()
        pauseIntent.action = action

        return PendingIntent.getBroadcast(
                playerService,
                REQUEST_CODE,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun createNotification(): Notification {

        mNotificationBuilder = NotificationCompat.Builder(playerService, CHANNEL_ID)

        if (VersioningHelper.isOreo()) {
            createNotificationChannel()
        }

        val openPlayerIntent = Intent(playerService, MainActivity::class.java)
        openPlayerIntent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val contentIntent = PendingIntent.getActivity(
                playerService, REQUEST_CODE,
                openPlayerIntent, 0
        )

        mNotificationBuilder
                .setShowWhen(false)
                .setStyle(MediaStyle()
                        .setShowActionsInCompactView(1, 2, 3)
                        .setMediaSession(playerService.getMediaSession().sessionToken)
                )
                .setContentIntent(contentIntent)
                .addAction(notificationAction(GoConstants.REPEAT_ACTION))
                .addAction(notificationAction(GoConstants.PREV_ACTION))
                .addAction(notificationAction(GoConstants.PLAY_PAUSE_ACTION))
                .addAction(notificationAction(GoConstants.NEXT_ACTION))
                .addAction(notificationAction(GoConstants.CLOSE_ACTION))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        updateNotificationContent()
        return mNotificationBuilder.build()
    }

    fun updateNotification() {
        mNotificationManager
                .notify(
                        GoConstants.NOTIFICATION_ID,
                        mNotificationBuilder.build()
                )
    }

    fun onCoverPrefChanged() {
        if (::mNotificationBuilder.isInitialized) {
            updateNotificationContent()
            updateNotification()
        }
    }

    fun updateNotificationContent() {
        val mediaPlayerHolder = playerService.mediaPlayerHolder
        mediaPlayerHolder.currentSong.first?.let { song ->

            val stockCover =
                    BitmapFactory.decodeResource(playerService.resources, R.drawable.default_cover)
            val cover = if (goPreferences.isCovers) {
                song.getCover(playerService) ?: stockCover
            } else {
                stockCover
            }

            mNotificationBuilder.setContentText(
                    playerService.getString(
                            R.string.artist_and_album,
                            song.artist,
                            song.album
                    )
            )
                    .setContentTitle(
                            playerService.getString(
                                    R.string.song_title_notification,
                                    song.title
                            ).toSpanned()
                    )
                    .setLargeIcon(cover)
                    .setColorized(true)
                    .setSmallIcon(getNotificationSmallIcon(mediaPlayerHolder))
        }
    }

    private fun getNotificationSmallIcon(mediaPlayerHolder: MediaPlayerHolder) = when (mediaPlayerHolder.launchedBy) {
        LaunchedBy.FolderView -> R.drawable.ic_folder
        LaunchedBy.AlbumView -> R.drawable.ic_library_music
        else -> R.drawable.ic_music_note
    }

    fun updatePlayPauseAction() {
        if (::mNotificationBuilder.isInitialized) {
            mNotificationActions[2] =
                    notificationAction(GoConstants.PLAY_PAUSE_ACTION)
        }
    }

    fun updateRepeatIcon() {
        if (::mNotificationBuilder.isInitialized) {
            mNotificationActions[0] =
                    notificationAction(GoConstants.REPEAT_ACTION)
            updateNotification()
        }
    }

    private fun notificationAction(action: String): NotificationCompat.Action {
        var icon =
                if (playerService.mediaPlayerHolder.state != GoConstants.PAUSED) {
                    R.drawable.ic_pause
                } else {
                    R.drawable.ic_play
                }
        when (action) {
            GoConstants.REPEAT_ACTION -> icon =
                    ThemeHelper.getRepeatIcon(playerService.mediaPlayerHolder)
            GoConstants.PREV_ACTION -> icon = R.drawable.ic_skip_previous
            GoConstants.NEXT_ACTION -> icon = R.drawable.ic_skip_next
            GoConstants.CLOSE_ACTION -> icon = R.drawable.ic_close
        }
        return NotificationCompat.Action.Builder(icon, action, playerAction(action)).build()
    }

    @RequiresApi(26)
    private fun createNotificationChannel() {
        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel(
                    CHANNEL_ID,
                    playerService.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = playerService.getString(R.string.app_name)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                mNotificationManager.createNotificationChannel(this)
            }
        }
    }
}
