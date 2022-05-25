package com.iven.musicplayergo.player


import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.text.parseAsHtml
import androidx.media.app.NotificationCompat.MediaStyle
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.waitForCover
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.utils.Theming
import com.iven.musicplayergo.utils.Versioning
import com.iven.musicplayergo.ui.MainActivity


class MusicNotificationManager(private val playerService: PlayerService) {

    //notification manager/builder
    private lateinit var mNotificationBuilder: NotificationCompat.Builder

    private var mNotificationColor = Color.BLACK

    private val mNotificationActions
        @SuppressLint("RestrictedApi")
        get() = mNotificationBuilder.mActions

    private val sFastSeekingActions get() = goPreferences.notificationActions != GoConstants.NOTIF_REPEAT_CLOSE

    private fun getPlayerAction(playerAction: String): PendingIntent {
        val intent = Intent().apply {
            action = playerAction
            component = ComponentName(playerService, PlayerService::class.java)
        }
        val flags = if (Versioning.isMarshmallow()) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getService(playerService, GoConstants.NOTIFICATION_INTENT_REQUEST_CODE, intent, flags)
    }

    private fun getFirstAdditionalAction() = if (sFastSeekingActions) {
        GoConstants.REWIND_ACTION
    } else {
        GoConstants.REPEAT_ACTION
    }

    private fun getSecondAdditionalAction() = if (sFastSeekingActions) {
        GoConstants.FAST_FORWARD_ACTION
    } else {
        GoConstants.CLOSE_ACTION
    }

    fun createNotification(onCreated: (Notification) -> Unit) {

        mNotificationBuilder =
            NotificationCompat.Builder(playerService, GoConstants.NOTIFICATION_CHANNEL_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(GoConstants.NOTIFICATION_CHANNEL_ID)
        }

        val openPlayerIntent = Intent(playerService, MainActivity::class.java)
        openPlayerIntent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val flags = if (Versioning.isMarshmallow()) {
            PendingIntent.FLAG_IMMUTABLE or 0
        } else {
            0
        }
        val contentIntent = PendingIntent.getActivity(
            playerService, GoConstants.NOTIFICATION_INTENT_REQUEST_CODE,
            openPlayerIntent, flags
        )

        mNotificationBuilder
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setShowWhen(false)
            .setOngoing(playerService.mediaPlayerHolder.isPlaying)
            .addAction(getNotificationAction(getFirstAdditionalAction()))
            .addAction(getNotificationAction(GoConstants.PREV_ACTION))
            .addAction(getNotificationAction(GoConstants.PLAY_PAUSE_ACTION))
            .addAction(getNotificationAction(GoConstants.NEXT_ACTION))
            .addAction(getNotificationAction(getSecondAdditionalAction()))
            .setStyle(
                MediaStyle()
                    .setMediaSession(playerService.getMediaSession().sessionToken)
                    .setShowActionsInCompactView(1, 2, 3)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .priority = NotificationCompat.PRIORITY_DEFAULT

        updateNotificationContent {
            onCreated(mNotificationBuilder.build())
        }
    }

    fun cancelNotification() {
        with(NotificationManagerCompat.from(playerService)) {
            cancel(GoConstants.NOTIFICATION_ID)
        }
    }

    fun updateNotification() {
        if (::mNotificationBuilder.isInitialized) {
            mNotificationBuilder.setOngoing(playerService.mediaPlayerHolder.isPlaying)
            with(NotificationManagerCompat.from(playerService)) {
                notify(GoConstants.NOTIFICATION_ID, mNotificationBuilder.build())
            }
        }
    }

    fun onHandleNotificationUpdate(isAdditionalActionsChanged: Boolean) {
        if (::mNotificationBuilder.isInitialized) {
            if (!isAdditionalActionsChanged) {
                updateNotificationContent {
                    updateNotification()
                }
            } else {
                mNotificationActions[0] =
                    getNotificationAction(getFirstAdditionalAction())
                mNotificationActions[4] =
                    getNotificationAction(getSecondAdditionalAction())
                updateNotification()
            }
        }
    }

    fun onSetNotificationColor(color: Int) {
        mNotificationColor = color
        onHandleNotificationUpdate(isAdditionalActionsChanged = false)
    }

    fun updateNotificationContent(onDone: (() -> Unit)? = null) {
        val mediaPlayerHolder = playerService.mediaPlayerHolder

        mediaPlayerHolder.currentSong?.let { song ->
            mNotificationBuilder
                .setContentText(song.artist)
                .setContentTitle(
                    playerService.getString(
                        R.string.song_title_notification,
                        song.title
                    ).parseAsHtml()
                )
                .setSubText(song.album)
                .setColor(mNotificationColor)
                .setColorized(true)
                .setSmallIcon(getNotificationSmallIcon(mediaPlayerHolder))

            song.albumId?.waitForCover(playerService) { bitmap, _ ->
                mNotificationBuilder.setLargeIcon(bitmap)
                onDone?.invoke()
            }
        }
    }

    private fun getNotificationSmallIcon(mediaPlayerHolder: MediaPlayerHolder) = if (mediaPlayerHolder.isQueue != null && mediaPlayerHolder.isQueueStarted) {
        R.drawable.ic_music_note
    } else {
        when (mediaPlayerHolder.launchedBy) {
            GoConstants.FOLDER_VIEW -> R.drawable.ic_folder_music
            GoConstants.ALBUM_VIEW -> R.drawable.ic_library_music
            else -> R.drawable.ic_music_note
        }
    }

    fun updatePlayPauseAction() {
        if (::mNotificationBuilder.isInitialized) {
            mNotificationActions[2] =
                getNotificationAction(GoConstants.PLAY_PAUSE_ACTION)
        }
    }

    fun updateRepeatIcon() {
        if (::mNotificationBuilder.isInitialized && !sFastSeekingActions) {
            mNotificationActions[0] =
                getNotificationAction(GoConstants.REPEAT_ACTION)
            updateNotification()
        }
    }

    private fun getNotificationAction(action: String): NotificationCompat.Action {
        var icon =
            if (playerService.mediaPlayerHolder.state != GoConstants.PAUSED) {
                R.drawable.ic_pause
            } else {
                R.drawable.ic_play
            }
        when (action) {
            GoConstants.REPEAT_ACTION -> icon =
                Theming.getRepeatIcon(playerService.mediaPlayerHolder)
            GoConstants.PREV_ACTION -> icon = R.drawable.ic_skip_previous
            GoConstants.NEXT_ACTION -> icon = R.drawable.ic_skip_next
            GoConstants.CLOSE_ACTION -> icon = R.drawable.ic_close
            GoConstants.FAST_FORWARD_ACTION -> icon = R.drawable.ic_fast_forward
            GoConstants.REWIND_ACTION -> icon = R.drawable.ic_fast_rewind
        }
        return NotificationCompat.Action.Builder(icon, action, getPlayerAction(action)).build()
    }

    fun createNotificationForError() {

        val notificationBuilder =
            NotificationCompat.Builder(playerService, GoConstants.NOTIFICATION_CHANNEL_ERROR_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(GoConstants.NOTIFICATION_CHANNEL_ERROR_ID)
        }

        notificationBuilder.setSmallIcon(R.drawable.ic_report)
            .setSilent(true)
            .setContentTitle(playerService.getString(R.string.error_fs_not_allowed_sum))
            .setContentText(playerService.getString(R.string.error_fs_not_allowed))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(playerService.getString(R.string.error_fs_not_allowed)))
            .priority = NotificationCompat.PRIORITY_DEFAULT
        with(NotificationManagerCompat.from(playerService)) {
            notify(GoConstants.NOTIFICATION_ERROR_ID, notificationBuilder.build())
        }
    }

    @TargetApi(26)
    private fun createNotificationChannel(channelId: String) {
        with(NotificationManagerCompat.from(playerService)) {
            val channel = NotificationChannel(
                channelId,
                playerService.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            createNotificationChannel(channel)
        }
    }
}
