package com.iven.musicplayergo.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.iven.musicplayergo.MainActivity
import com.iven.musicplayergo.PlayerService
import com.iven.musicplayergo.R
import com.iven.musicplayergo.music.MusicUtils

// Notification params
private const val CHANNEL_ID = "com.iven.musicplayergo.CHANNEL_ID"
private const val REQUEST_CODE = 100
const val NOTIFICATION_ID = 101

// Notification actions
const val PLAY_PAUSE_ACTION = "com.iven.musicplayergo.PLAYPAUSE"
const val NEXT_ACTION = "com.iven.musicplayergo.NEXT"
const val PREV_ACTION = "com.iven.musicplayergo.PREV"

class MusicNotificationManager(private val playerService: PlayerService) {

    //notification manager/builder
    val notificationManager: NotificationManager =
        playerService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    var notificationBuilder: NotificationCompat.Builder? = null

    //accent
    var mAccent: Int = R.color.blue

    //https://gist.github.com/Gnzlt/6ddc846ef68c587d559f1e1fcd0900d3
    private fun getLargeIcon(): Bitmap {

        val vectorDrawable = playerService.getDrawable(R.drawable.music_notification) as VectorDrawable

        val largeIconSize = playerService.resources.getDimensionPixelSize(R.dimen.notification_large_dim)
        val bitmap = Bitmap.createBitmap(largeIconSize, largeIconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.setTint(mAccent)
        vectorDrawable.alpha = 150
        vectorDrawable.draw(canvas)

        return bitmap
    }

    private fun playerAction(action: String): PendingIntent {

        val pauseIntent = Intent()
        pauseIntent.action = action

        return PendingIntent.getBroadcast(playerService, REQUEST_CODE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun createNotification(): Notification {

        val song = playerService.mediaPlayerHolder!!.currentSong

        notificationBuilder = NotificationCompat.Builder(playerService, CHANNEL_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val openPlayerIntent = Intent(playerService, MainActivity::class.java)
        openPlayerIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val contentIntent = PendingIntent.getActivity(
            playerService, REQUEST_CODE,
            openPlayerIntent, 0
        )

        val artist = song!!.artist
        val songTitle = song.title

        val spanned = MusicUtils.buildSpanned(playerService.getString(R.string.playing_song, artist, songTitle))

        notificationBuilder!!
            .setShowWhen(false)
            .setSmallIcon(R.drawable.music_notification)
            .setLargeIcon(getLargeIcon())
            .setColor(mAccent)
            .setContentTitle(spanned)
            .setContentText(song.album)
            .setContentIntent(contentIntent)
            .addAction(notificationAction(PREV_ACTION))
            .addAction(notificationAction(PLAY_PAUSE_ACTION))
            .addAction(notificationAction(NEXT_ACTION))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationBuilder!!.setStyle(MediaStyle().setShowActionsInCompactView(0, 1, 2))
        return notificationBuilder!!.build()
    }

    private fun notificationAction(action: String): NotificationCompat.Action {

        var icon: Int =
            if (playerService.mediaPlayerHolder!!.state != PAUSED) R.drawable.ic_pause_notification else R.drawable.ic_play_notification

        when (action) {
            PREV_ACTION -> icon = R.drawable.ic_skip_previous_notification
            NEXT_ACTION -> icon = R.drawable.ic_skip_next_notification
        }

        return NotificationCompat.Action.Builder(icon, action, playerAction(action)).build()
    }

    @RequiresApi(26)
    private fun createNotificationChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                playerService.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.description = playerService.getString(R.string.app_name)
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}
