package com.iven.musicplayergo.player


import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.models.NotificationAction
import com.iven.musicplayergo.ui.MainActivity
import com.iven.musicplayergo.utils.Theming
import com.iven.musicplayergo.utils.Versioning


class MusicNotificationManager(private val playerService: PlayerService) {

    private val mMediaPlayerHolder get() = MediaPlayerHolder.getInstance()

    //notification manager/builder
    private lateinit var mNotificationBuilder: NotificationCompat.Builder
    private val mNotificationManagerCompat get() = NotificationManagerCompat.from(playerService)

    private val mNotificationActions
        @SuppressLint("RestrictedApi")
        get() = mNotificationBuilder.mActions

    private fun getPendingIntent(playerAction: String): PendingIntent {
        val intent = Intent().apply {
            action = playerAction
            component = ComponentName(playerService, PlayerService::class.java)
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Versioning.isMarshmallow()) {
            flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getService(playerService, GoConstants.NOTIFICATION_INTENT_REQUEST_CODE, intent, flags)
    }

    private val notificationActions: NotificationAction get() = GoPreferences.getPrefsInstance().notificationActions

    fun createNotification(onCreated: (Notification) -> Unit) {

        mNotificationBuilder =
            NotificationCompat.Builder(playerService, GoConstants.NOTIFICATION_CHANNEL_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(GoConstants.NOTIFICATION_CHANNEL_ID)
        }

        val openPlayerIntent = Intent(playerService, MainActivity::class.java)
        openPlayerIntent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        var flags = 0
        if (Versioning.isMarshmallow()) flags = PendingIntent.FLAG_IMMUTABLE or 0
        val contentIntent = PendingIntent.getActivity(
            playerService, GoConstants.NOTIFICATION_INTENT_REQUEST_CODE,
            openPlayerIntent, flags
        )

        mNotificationBuilder
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLargeIcon(null)
            .setOngoing(mMediaPlayerHolder.isPlaying)
            .setSmallIcon(R.drawable.ic_music_note)
            .addAction(getNotificationAction(notificationActions.first))
            .addAction(getNotificationAction(GoConstants.PREV_ACTION))
            .addAction(getNotificationAction(GoConstants.PLAY_PAUSE_ACTION))
            .addAction(getNotificationAction(GoConstants.NEXT_ACTION))
            .addAction(getNotificationAction(notificationActions.second))
            .setStyle(MediaStyle()
                .setMediaSession(playerService.getMediaSession()?.sessionToken)
                .setShowActionsInCompactView(1, 2, 3)
            )

        updateNotificationContent {
            onCreated(mNotificationBuilder.build())
        }
    }

    fun updateNotification() {
        if (::mNotificationBuilder.isInitialized) {
            mNotificationBuilder.setOngoing(mMediaPlayerHolder.isPlaying)
            updatePlayPauseAction()
            with(mNotificationManagerCompat) {
                notify(GoConstants.NOTIFICATION_ID, mNotificationBuilder.build())
            }
        }
    }

    fun cancelNotification() {
        with(mNotificationManagerCompat) {
            cancel(GoConstants.NOTIFICATION_ID)
        }
    }

    fun onHandleNotificationUpdate(isAdditionalActionsChanged: Boolean) {
        if (::mNotificationBuilder.isInitialized) {
            if (!isAdditionalActionsChanged) {
                updateNotificationContent {
                    updateNotification()
                }
                return
            }
            mNotificationActions[0] =
                getNotificationAction(notificationActions.first)
            mNotificationActions[4] =
                getNotificationAction(notificationActions.second)
            updateNotification()
        }
    }

    fun updateNotificationContent(onDone: (() -> Unit)? = null) {
        mMediaPlayerHolder.getMediaMetadataCompat()?.run {
            mNotificationBuilder
                .setContentText(getText(MediaMetadataCompat.METADATA_KEY_ARTIST))
                .setContentTitle(getText(MediaMetadataCompat.METADATA_KEY_TITLE))
                .setSubText(getText(MediaMetadataCompat.METADATA_KEY_ALBUM))
                .setLargeIcon(getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
        }
        onDone?.invoke()
    }

    fun updatePlayPauseAction() {
        if (::mNotificationBuilder.isInitialized) {
            mNotificationActions[2] =
                getNotificationAction(GoConstants.PLAY_PAUSE_ACTION)
        }
    }

    fun updateRepeatIcon() {
        if (::mNotificationBuilder.isInitialized) {
            mNotificationActions[0] =
                getNotificationAction(GoConstants.REPEAT_ACTION)
            updateNotification()
        }
    }

    fun updateFavoriteIcon() {
        if (::mNotificationBuilder.isInitialized) {
            mNotificationActions[0] =
                getNotificationAction(GoConstants.FAVORITE_ACTION)
            updateNotification()
        }
    }

    private fun getNotificationAction(action: String): NotificationCompat.Action {
        val icon = Theming.getNotificationActionIcon(action, isNotification = true)
        return NotificationCompat.Action.Builder(icon, action, getPendingIntent(action)).build()
    }

    @TargetApi(Build.VERSION_CODES.S)
    fun createNotificationForError() {

        val notificationBuilder =
            NotificationCompat.Builder(playerService, GoConstants.NOTIFICATION_CHANNEL_ERROR_ID)

        createNotificationChannel(GoConstants.NOTIFICATION_CHANNEL_ERROR_ID)

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
    private fun createNotificationChannel(id: String) {
        val name = playerService.getString(R.string.app_name)
        val channel = NotificationChannelCompat.Builder(id, NotificationManager.IMPORTANCE_LOW)
            .setName(name)
            .setLightsEnabled(false)
            .setVibrationEnabled(false)
            .setShowBadge(false)
            .build()

        // Register the channel with the system
        mNotificationManagerCompat.createNotificationChannel(channel)
    }
}
