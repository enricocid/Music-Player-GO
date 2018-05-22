package com.iven.musicplayergo.playback;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class MusicService extends Service {

    private final IBinder mIBinder = new LocalBinder();

    private MediaPlayerHolder mMediaPlayerHolder;

    private MusicNotificationManager mMusicNotificationManager;

    private boolean sRestoredFromPause = false;

    public boolean isRestoredFromPause() {
        return sRestoredFromPause;
    }

    public void setRestoredFromPause(boolean restore) {
        sRestoredFromPause = restore;
    }

    public MediaPlayerHolder getMediaPlayerHolder() {
        return mMediaPlayerHolder;
    }

    public MusicNotificationManager getMusicNotificationManager() {
        return mMusicNotificationManager;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
    public IBinder onBind(Intent intent) {
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