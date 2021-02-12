package com.iven.musicplayergo.player


import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.parseAsHtml
import androidx.media.app.NotificationCompat.MediaStyle
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.getCoverFromPFD
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.ui.MainActivity


class MusicNotificationManager(private val playerService: PlayerService) {

    //notification manager/builder
    private lateinit var mNotificationBuilder: NotificationCompat.Builder

    private val mNotificationActions
        @SuppressLint("RestrictedApi")
        get() = mNotificationBuilder.mActions

    private val sFastSeekingActions get() = goPreferences.isFastSeekingActions

    private var mAlbumArt = ContextCompat.getDrawable(playerService, R.drawable.album_art)?.toBitmap()

    private fun getPlayerAction(playerAction: String): PendingIntent {
        val intent = Intent().apply {
            action = playerAction
            component = ComponentName(playerService, PlayerService::class.java)
        }
        return PendingIntent.getService(playerService, GoConstants.NOTIFICATION_INTENT_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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

    fun createNotification(): Notification {

        mNotificationBuilder =
            NotificationCompat.Builder(playerService, GoConstants.NOTIFICATION_CHANNEL_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            mNotificationBuilder.priority = NotificationCompat.PRIORITY_LOW
        }

        val openPlayerIntent = Intent(playerService, MainActivity::class.java)
        openPlayerIntent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val contentIntent = PendingIntent.getActivity(
                playerService, GoConstants.NOTIFICATION_INTENT_REQUEST_CODE,
                openPlayerIntent, 0
        )

        updateNotificationContent()

        mNotificationBuilder
            .setContentIntent(contentIntent)
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
        return mNotificationBuilder.build()
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

    fun onUpdateDefaultAlbumArt(bitmapRes: Bitmap?, updateNotification: Boolean) {
        mAlbumArt = bitmapRes
        if (updateNotification) {
            onHandleNotificationUpdate(false)
        }
    }

    fun onHandleNotificationUpdate(isAdditionalActionsChanged: Boolean) {
        if (::mNotificationBuilder.isInitialized) {
            if (!isAdditionalActionsChanged) {
                updateNotificationContent()
                updateNotification()
            } else {
                mNotificationActions[0] =
                    getNotificationAction(getFirstAdditionalAction())
                mNotificationActions[4] =
                    getNotificationAction(getSecondAdditionalAction())
                updateNotification()
            }
        }
    }

    fun updateNotificationContent() {
        val mediaPlayerHolder = playerService.mediaPlayerHolder
        mediaPlayerHolder.currentSong.first?.let { song ->

            val cover = if (goPreferences.isCovers) {
                song.albumId?.getCoverFromPFD(playerService) ?: mAlbumArt
            } else {
                mAlbumArt
            }

            mNotificationBuilder
                    .setContentText(song.artist)
                    .setContentTitle(
                            playerService.getString(
                                    R.string.song_title_notification,
                                    song.title
                            ).parseAsHtml()
                    )
                    .setSubText(song.album)
                    .setLargeIcon(cover)
                    .setColorized(true)
                    .setSmallIcon(getNotificationSmallIcon(mediaPlayerHolder))
        }
    }

    private fun getNotificationSmallIcon(mediaPlayerHolder: MediaPlayerHolder) =
        when (mediaPlayerHolder.launchedBy) {
            GoConstants.FOLDER_VIEW -> R.drawable.ic_folder_music
            GoConstants.ALBUM_VIEW -> R.drawable.ic_library_music
            else -> R.drawable.ic_music_note
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
                    ThemeHelper.getRepeatIcon(playerService.mediaPlayerHolder)
            GoConstants.PREV_ACTION -> icon = R.drawable.ic_skip_previous
            GoConstants.NEXT_ACTION -> icon = R.drawable.ic_skip_next
            GoConstants.CLOSE_ACTION -> icon = R.drawable.ic_close
            GoConstants.FAST_FORWARD_ACTION -> icon = R.drawable.ic_fast_forward
            GoConstants.REWIND_ACTION -> icon = R.drawable.ic_fast_rewind
        }
        return NotificationCompat.Action.Builder(icon, action, getPlayerAction(action)).build()
    }

    @TargetApi(26)
    private fun createNotificationChannel() {
        with(playerService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            if (getNotificationChannel(GoConstants.NOTIFICATION_CHANNEL_ID) == null) {
                NotificationChannel(
                        GoConstants.NOTIFICATION_CHANNEL_ID,
                        playerService.getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = playerService.getString(R.string.app_name)
                    enableLights(false)
                    enableVibration(false)
                    setShowBadge(false)
                    createNotificationChannel(this)
                }
            }
        }
    }
}
