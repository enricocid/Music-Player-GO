package com.iven.musicplayergo.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.text.Html;
import android.text.Spanned;

import com.iven.musicplayergo.MainActivity;
import com.iven.musicplayergo.R;
import com.iven.musicplayergo.models.Song;
import com.iven.musicplayergo.utils.AndroidVersion;

public class MusicNotificationManager {

    public static final int NOTIFICATION_ID = 101;
    static final String PLAY_PAUSE_ACTION = "com.iven.musicplayergo.PLAYPAUSE";
    static final String NEXT_ACTION = "com.iven.musicplayergo.NEXT";
    static final String PREV_ACTION = "com.iven.musicplayergo.PREV";
    private final String CHANNEL_ID = "com.iven.musicplayergo.CHANNEL_ID";
    private final int REQUEST_CODE = 100;
    private final NotificationManager mNotificationManager;
    private final MusicService mMusicService;
    private NotificationCompat.Builder mNotificationBuilder;
    private int mAccent;

    MusicNotificationManager(@NonNull final MusicService musicService) {

        mMusicService = musicService;
        mNotificationManager = (NotificationManager) mMusicService.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void setAccentColor(int color) {
        mAccent = ContextCompat.getColor(mMusicService, color);
    }

    public NotificationManager getNotificationManager() {
        return mNotificationManager;
    }

    public NotificationCompat.Builder getNotificationBuilder() {
        return mNotificationBuilder;
    }

    private PendingIntent playerAction(String action) {

        Intent pauseIntent = new Intent();
        pauseIntent.setAction(action);

        return PendingIntent.getBroadcast(mMusicService, REQUEST_CODE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public Notification createNotification() {

        Song song = mMusicService.getMediaPlayerHolder().getCurrentSong();

        mNotificationBuilder = new NotificationCompat.Builder(mMusicService, CHANNEL_ID);

        if (AndroidVersion.isOreo()) {
            createNotificationChannel();
        }

        Intent openPlayerIntent = new Intent(mMusicService, MainActivity.class);
        openPlayerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(mMusicService, REQUEST_CODE,
                openPlayerIntent, 0);

        String artist = song.artistName;
        String songTitle = song.title;

        Spanned spanned = AndroidVersion.isNougat() ?
                Html.fromHtml(mMusicService.getString(R.string.playing_song, artist, songTitle), Html.FROM_HTML_MODE_LEGACY) :
                Html.fromHtml(mMusicService.getString(R.string.playing_song, artist, songTitle));

        mNotificationBuilder
                .setShowWhen(false)
                .setSmallIcon(R.drawable.music_notification)
                .setLargeIcon(getLargeIcon(mMusicService.getDrawable(R.drawable.music_notification)))
                .setColor(mAccent)
                .setContentTitle(spanned)
                .setContentText(song.albumName)
                .setContentIntent(contentIntent)
                .addAction(notificationAction(PREV_ACTION))
                .addAction(notificationAction(PLAY_PAUSE_ACTION))
                .addAction(notificationAction(NEXT_ACTION))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        mNotificationBuilder.setStyle(new MediaStyle().setShowActionsInCompactView(0, 1, 2));
        return mNotificationBuilder.build();
    }

    @NonNull
    private NotificationCompat.Action notificationAction(String action) {

        int icon;

        switch (action) {
            default:
            case PREV_ACTION:
                icon = R.drawable.ic_skip_previous_notification;
                break;
            case PLAY_PAUSE_ACTION:

                icon = mMusicService.getMediaPlayerHolder().getState() != PlaybackInfoListener.State.PAUSED ? R.drawable.ic_pause_notification : R.drawable.ic_play_notification;
                break;
            case NEXT_ACTION:
                icon = R.drawable.ic_skip_next_notification;
                break;
        }
        return new NotificationCompat.Action.Builder(icon, action, playerAction(action)).build();
    }

    @RequiresApi(26)
    private void createNotificationChannel() {

        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(CHANNEL_ID,
                            mMusicService.getString(R.string.app_name),
                            NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription(
                    mMusicService.getString(R.string.app_name));

            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setShowBadge(false);

            mNotificationManager.createNotificationChannel(notificationChannel);
        }
    }

    //https://gist.github.com/Gnzlt/6ddc846ef68c587d559f1e1fcd0900d3
    private Bitmap getLargeIcon(@NonNull final Drawable drawable) {

        VectorDrawable vectorDrawable = (VectorDrawable) drawable;
        int largeIconSize = mMusicService.getResources().getDimensionPixelSize(R.dimen.notification_large_dim);
        Bitmap bitmap = Bitmap.createBitmap(largeIconSize,
                largeIconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.setTint(mAccent);
        vectorDrawable.setAlpha(100);
        vectorDrawable.draw(canvas);
        return bitmap;
    }
}
