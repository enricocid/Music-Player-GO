package com.iven.musicplayergo.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toSpanned
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.helpers.VersioningHelper
import com.iven.musicplayergo.ui.MainActivity

// Notification params
private const val CHANNEL_ID = "CHANNEL_ID_GO"
private const val REQUEST_CODE = 100

class MusicNotificationManager(private val playerService: PlayerService) {

    //notification manager/builder
    val notificationManager = NotificationManagerCompat.from(playerService)
    lateinit var notificationBuilder: NotificationCompat.Builder

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

        val mediaPlayerHolder = playerService.mediaPlayerHolder
        mediaPlayerHolder.currentSong.first.let { song ->
            notificationBuilder = NotificationCompat.Builder(playerService, CHANNEL_ID)

            if (VersioningHelper.isOreo()) createNotificationChannel()

            val openPlayerIntent = Intent(playerService, MainActivity::class.java)
            openPlayerIntent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val contentIntent = PendingIntent.getActivity(
                playerService, REQUEST_CODE,
                openPlayerIntent, 0
            )

            val style = MediaStyle().setShowActionsInCompactView(1, 2, 3)

            if (VersioningHelper.isQ()) style.setMediaSession(playerService.getMediaSession().sessionToken)

            notificationBuilder.let {
                it.apply {
                    setShowWhen(false)
                    setStyle(style)
                    setSmallIcon(if (mediaPlayerHolder.isPlayingFromFolder) R.drawable.ic_library_music else R.drawable.ic_music_note)
                    color = ThemeHelper.resolveThemeAccent(playerService)
                    setContentTitle(
                        playerService.getString(
                            R.string.song_title_notification,
                            song?.title
                        ).toSpanned()
                    )
                    setContentText(
                        playerService.getString(
                            R.string.artist_and_album,
                            song?.artist,
                            song?.album
                        )
                    )
                    setContentIntent(contentIntent)
                    addAction(notificationAction(GoConstants.REPEAT_ACTION))
                    addAction(notificationAction(GoConstants.PREV_ACTION))
                    addAction(notificationAction(GoConstants.PLAY_PAUSE_ACTION))
                    addAction(notificationAction(GoConstants.NEXT_ACTION))
                    addAction(notificationAction(GoConstants.CLOSE_ACTION))
                    setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                }
            }
        }
        return notificationBuilder.build()
    }

    private fun notificationAction(action: String): NotificationCompat.Action {
        var icon =
            if (playerService.mediaPlayerHolder.state != GoConstants.PAUSED) R.drawable.ic_pause else R.drawable.ic_play

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
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel(
                CHANNEL_ID,
                playerService.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = playerService.getString(R.string.app_name)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                notificationManager.createNotificationChannel(this)
            }
        }
    }
}
