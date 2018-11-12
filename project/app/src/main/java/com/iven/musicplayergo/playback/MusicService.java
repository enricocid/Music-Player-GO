package com.iven.musicplayergo.playback;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.NonNull;

public class MusicService extends Service {

    private final IBinder mIBinder = new LocalBinder();

    private MediaPlayerHolder mMediaPlayerHolder;

    private MusicNotificationManager mMusicNotificationManager;

    private boolean sRestoredFromPause = false;

    public final boolean isRestoredFromPause() {
        return sRestoredFromPause;
    }

    public void setRestoredFromPause(boolean restore) {
        sRestoredFromPause = restore;
    }

    public final MediaPlayerHolder getMediaPlayerHolder() {
        return mMediaPlayerHolder;
    }

    public MusicNotificationManager getMusicNotificationManager() {
        return mMusicNotificationManager;
    }

    @Override
    public int onStartCommand(@NonNull final Intent intent, final int flags, final int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mMediaPlayerHolder.registerNotificationActionsReceiver(false);
        mMusicNotificationManager = null;
        mMediaPlayerHolder.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(@NonNull final Intent intent) {
        if (mMediaPlayerHolder == null) {
            mMediaPlayerHolder = new MediaPlayerHolder(this);
            mMusicNotificationManager = new MusicNotificationManager(this);
            mMediaPlayerHolder.registerNotificationActionsReceiver(true);
        }
        return mIBinder;
    }

    public class LocalBinder extends Binder {
        public MusicService getInstance() {
            return MusicService.this;
        }
    }
}