package com.iven.musicplayergo;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.Pair;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.iven.musicplayergo.adapters.AlbumsAdapter;
import com.iven.musicplayergo.adapters.ArtistsAdapter;
import com.iven.musicplayergo.adapters.ColorsAdapter;
import com.iven.musicplayergo.adapters.SongsAdapter;
import com.iven.musicplayergo.loaders.AlbumProvider;
import com.iven.musicplayergo.loaders.ArtistProvider;
import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Artist;
import com.iven.musicplayergo.models.Song;
import com.iven.musicplayergo.playback.EqualizerUtils;
import com.iven.musicplayergo.playback.MusicNotificationManager;
import com.iven.musicplayergo.playback.MusicService;
import com.iven.musicplayergo.playback.PlaybackInfoListener;
import com.iven.musicplayergo.playback.PlayerAdapter;
import com.iven.musicplayergo.utils.AndroidVersion;
import com.iven.musicplayergo.utils.PermissionUtils;
import com.iven.musicplayergo.utils.SettingsUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.List;

public class MainActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks, SongsAdapter.songSelectedListener, ColorsAdapter.onAccentChangedListener, AlbumsAdapter.albumSelectedListener, ArtistsAdapter.artistSelectedListener {

    private int mAccent;
    private FastScrollRecyclerView mArtistsRecyclerView;
    private RecyclerView mColorsRecyclerView, mAlbumsRecyclerView, mSongsRecyclerView;
    private AlbumsAdapter mAlbumsAdapter;
    private SongsAdapter mSongsAdapter;

    private TextView mPlayingAlbum, mPlayingSong, mDuration, mSongPosition;
    private SeekBar mSeekBarAudio;
    private LinearLayout mControlsContainer;
    private SlidingUpPanelLayout mSlidingUpPanel;
    private ImageButton mPlayPauseButton, mResetButton, mImmersiveButton;
    private View mSettingsView;
    private boolean sThemeDark;

    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;

    private Song mSelectedSong;
    private String mSelectedArtist;
    private boolean sExpandPanel = false;

    private MusicService mMusicService;
    private PlaybackListener mPlaybackListener;
    private boolean mIsBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            mMusicService = ((MusicService.LocalBinder) iBinder).getInstance();
            mPlayerAdapter = mMusicService.getMediaPlayerHolder();

            if (mPlaybackListener == null) {
                mPlaybackListener = new PlaybackListener();
                mPlayerAdapter.setPlaybackInfoListener(mPlaybackListener);
            }
            restorePlayerStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mMusicService = null;
        }
    };

    @Override
    public void onAccentChanged(int color) {
        if (mMusicService != null) {
            MusicNotificationManager musicNotificationManager = mMusicService.getMusicNotificationManager();
            musicNotificationManager.setAccentColor(color);
            if (mMusicService.getMediaPlayerHolder().isMediaPlayer()) {
                musicNotificationManager.getNotificationManager().notify(MusicNotificationManager.NOTIFICATION_ID, musicNotificationManager.createNotification());
            }
        }
        SettingsUtils.setThemeAccent(this, color);
    }

    @Override
    public void onBackPressed() {

        //if the sliding up panel is expanded collapse it
        if (mSlidingUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            mSlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    private void setImmersiveDrawable(boolean isImmersive) {

        int drawable = isImmersive ? R.drawable.ic_fullscreen_exit_24dp : R.drawable.ic_fullscreen_24dp;
        mImmersiveButton.setImageResource(drawable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sThemeDark = SettingsUtils.isThemeDark(this);
        mAccent = SettingsUtils.getAccent(this);

        SettingsUtils.setTheme(this, sThemeDark, mAccent);

        setContentView(R.layout.main_activity);

        getViews();

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        boolean isImmersive = (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
                        setImmersiveDrawable(isImmersive);
                        if (!isImmersive && AndroidVersion.isMarshmallow()) {
                            //check light status bar if immersive mode is disabled
                            SettingsUtils.enableLightStatusBar(MainActivity.this, ContextCompat.getColor(MainActivity.this, mAccent));
                        }
                    }
                });

        boolean isImmersive = SettingsUtils.isImmersive(this);
        if (isImmersive) {
            SettingsUtils.toggleHideyBar(this, true);
            setImmersiveDrawable(true);
        }

        initializeSeekBar();

        mSeekBarAudio.setEnabled(false);

        initializeColorsSettings();

        setSlidingUpPanelHeight();

        doBindService();

        Intent startNotStickyIntent = new Intent(this, MusicService.class);

        startService(startNotStickyIntent);

        if (AndroidVersion.isMarshmallow()) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                PermissionUtils.requestReadPermission(this);

            } else {

                onPermissionGranted();
            }
        } else {
            onPermissionGranted();
        }
    }

    private void getViews() {

        mSlidingUpPanel = findViewById(R.id.sliding_panel);

        mControlsContainer = findViewById(R.id.controls_container);

        mPlayPauseButton = findViewById(R.id.play_pause);
        mResetButton = findViewById(R.id.replay);

        ImageButton eqButton = findViewById(R.id.eq);
        if (!EqualizerUtils.hasEqualizer(this)) {
            mControlsContainer.removeView(eqButton);
        }

        mImmersiveButton = findViewById(R.id.immersive);

        mSettingsView = findViewById(R.id.settings_view);
        mColorsRecyclerView = findViewById(R.id.colors_rv);

        mSeekBarAudio = findViewById(R.id.seekTo);

        mPlayingSong = findViewById(R.id.playing_song);
        mPlayingAlbum = findViewById(R.id.playing_album);
        mDuration = findViewById(R.id.duration);
        mSongPosition = findViewById(R.id.song_position);

        mArtistsRecyclerView = findViewById(R.id.artists_rv);
        LinearLayoutManager artistsLayoutManager = new LinearLayoutManager(this);
        mArtistsRecyclerView.setLayoutManager(artistsLayoutManager);

        mAlbumsRecyclerView = findViewById(R.id.albums_rv);
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mAlbumsRecyclerView.setLayoutManager(horizontalLayoutManager);

        mSongsRecyclerView = findViewById(R.id.songs_rv);
        LinearLayoutManager songsLayoutManager = new LinearLayoutManager(this);
        mSongsRecyclerView.setLayoutManager(songsLayoutManager);
    }

    private void setSlidingUpPanelHeight() {

        final ViewTreeObserver observer = mControlsContainer.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mSlidingUpPanel.setPanelHeight(mControlsContainer.getHeight());
                mControlsContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    void setArtistsRecyclerView(List<Artist> data) {

        ArtistsAdapter artistsAdapter = new ArtistsAdapter(this, data);
        mArtistsRecyclerView.setAdapter(artistsAdapter);
    }

    private void initializeSeekBar() {
        mSeekBarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if (fromUser) {
                            userSelectedPosition = progress;
                        }
                        mSongPosition.setText(Song.formatDuration(progress));
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    public void reset(View v) {
        if (checkIsPlayer()) {
            mPlayerAdapter.reset();
            updateResetStatus(false);
        }
    }

    public void skipPrev(View v) {
        if (checkIsPlayer()) {
            mPlayerAdapter.instantReset();
        }
    }

    public void resumeOrPause(View v) {
        if (checkIsPlayer()) {
            mPlayerAdapter.resumeOrPause();
        }
    }

    public void skipNext(View v) {
        if (checkIsPlayer()) {
            mPlayerAdapter.skip(true);
        }
    }

    public void openEqualizer(View v) {
        if (checkIsPlayer()) {
            mPlayerAdapter.openEqualizer(MainActivity.this);
            SettingsUtils.openOrCloseSettings(mSettingsView, mColorsRecyclerView, false);
        }
    }

    public void openGitPage(View v) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/enricocid/Music-Player-GO")));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.no_browser), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private boolean checkIsPlayer() {

        boolean isPlayer = mPlayerAdapter.isMediaPlayer();
        if (!isPlayer) {
            EqualizerUtils.notifyNoSessionId(MainActivity.this);
        }
        return isPlayer;
    }

    public void openSettings(View v) {
        SettingsUtils.openOrCloseSettings(mSettingsView, mColorsRecyclerView, true);
    }

    public void invertUI(View v) {
        //avoid service killing when the player is in paused state
        if (mPlayerAdapter != null && mPlayerAdapter.getState() == PlaybackInfoListener.State.PAUSED) {
            mMusicService.startForeground(MusicNotificationManager.NOTIFICATION_ID, mMusicService.getMusicNotificationManager().createNotification());
            mMusicService.setRestoredFromPause(true);
        }

        SettingsUtils.setThemeDark(MainActivity.this);
    }

    public void closeSettings(View v) {
        SettingsUtils.openOrCloseSettings(mSettingsView, mColorsRecyclerView, false);
    }

    public void immersePlayer(View v) {
        SettingsUtils.toggleHideyBar(MainActivity.this, false);
    }

    private void initializeColorsSettings() {

        mColorsRecyclerView = findViewById(R.id.colors_rv);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mColorsRecyclerView.setLayoutManager(linearLayoutManager);
        mColorsRecyclerView.setAdapter(new ColorsAdapter(this, mColorsRecyclerView, mAccent));
    }

    private void onPermissionGranted() {
        getSupportLoaderManager().initLoader(ArtistProvider.ARTISTS_LOADER, null, this);
    }

    private void updateResetStatus(boolean onPlaybackCompletion) {

        int themeColor = sThemeDark ? Color.WHITE : Color.BLACK;
        int color = onPlaybackCompletion ? themeColor : mPlayerAdapter.isReset() ? ContextCompat.getColor(this, mAccent) : themeColor;
        mResetButton.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private void updatePlayingStatus() {

        int drawable = mPlayerAdapter.getState() != PlaybackInfoListener.State.PAUSED ? R.drawable.ic_pause_24dp : R.drawable.ic_play_arrow_24dp;
        mPlayPauseButton.setImageResource(drawable);
    }

    private void updatePlayingInfo() {

        mSelectedSong = mPlayerAdapter.getCurrentSong();
        mSelectedArtist = mSelectedSong.artistName;

        Spanned spanned = AndroidVersion.isNougat() ?
                Html.fromHtml(getString(R.string.playing_song, mSelectedArtist, mSelectedSong.title), Html.FROM_HTML_MODE_LEGACY) :
                Html.fromHtml(getString(R.string.playing_song, mSelectedArtist, mSelectedSong.title));
        mPlayingSong.setText(spanned);
        mPlayingAlbum.setText(mSelectedSong.albumName);
    }

    private void restorePlayerStatus() {

        //if we are playing and the activity was restarted
        //update the controls panel
        if (mPlayerAdapter.isMediaPlayer()) {

            updatePlayingInfo();
            updatePlayingStatus();
            updateResetStatus(false);

            setSeekBarEnabled();
            int duration = mSelectedSong.duration;
            mSeekBarAudio.setMax(duration);
            mSeekBarAudio.setProgress(mPlayerAdapter.getPlayerPosition());
            mDuration.setText(Song.formatDuration(duration));

            //stop foreground if coming from pause state
            if (mMusicService.isRestoredFromPause()) {
                mMusicService.stopForeground(false);
                mMusicService.getMusicNotificationManager().getNotificationManager().notify(MusicNotificationManager.NOTIFICATION_ID, mMusicService.getMusicNotificationManager().getNotificationBuilder().build());
                mMusicService.setRestoredFromPause(false);
            }
        }

        //restore sliding panel content (albums and songs)
        if (mPlayerAdapter.getCurrentArtist() != null) {
            mSelectedArtist = mPlayerAdapter.getCurrentArtist();
            getSupportLoaderManager().initLoader(AlbumProvider.ALBUMS_LOADER, null, this);
        }
    }

    private void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this,
                MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return id != AlbumProvider.ALBUMS_LOADER ? new ArtistProvider.AsyncArtistLoader(this) : new AlbumProvider.AsyncAlbumsForArtistLoader(this, mSelectedArtist);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadFinished(Loader loader, Object data) {

        switch (loader.getId()) {
            case ArtistProvider.ARTISTS_LOADER:

                //get loaded artist list and set the artists recycler view
                List<Artist> artists = (List<Artist>) data;
                setArtistsRecyclerView(artists);

                //load the details of the first artist on list
                mSelectedArtist = artists.get(0).getName();
                getSupportLoaderManager().initLoader(AlbumProvider.ALBUMS_LOADER, null, this);
                break;

            case AlbumProvider.ALBUMS_LOADER:

                //get loaded albums for artist
                Pair<Artist, List<Album>> albumsForArtist = (Pair<Artist, List<Album>>) data;

                if (mAlbumsAdapter != null) {
                    //only notify recycler view of item changed if an adapter already exists
                    mAlbumsAdapter.swapArtist(albumsForArtist);
                } else {
                    mAlbumsAdapter = new AlbumsAdapter(this, albumsForArtist);
                    mAlbumsRecyclerView.setAdapter(mAlbumsAdapter);
                }
                if (sExpandPanel) {
                    mSlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        boolean isImmersive = SettingsUtils.isImmersive(this);
        if (hasFocus && isImmersive) {
            SettingsUtils.toggleHideyBar(this, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlaybackListener = null;
        doUnbindService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            onPermissionGranted();

        } else {
            PermissionUtils.notifyFail(this);
        }
    }

    private void setSeekBarEnabled() {
        if (!mSeekBarAudio.isEnabled()) {
            mSeekBarAudio.setEnabled(true);
        }
    }

    @Override
    public void onSongSelected(Song song, List<Song> songs) {

        setSeekBarEnabled();
        mSelectedSong = song;
        if (mMusicService != null) {
            mMusicService.getMusicNotificationManager().setAccentColor(mAccent);
            mPlayerAdapter.setCurrentSong(song, songs);
            mPlayerAdapter.play();
        }
    }

    @Override
    public void onArtistSelected(String artist) {

        sExpandPanel = true;

        if (!mSelectedArtist.equals(artist)) {
            //load artist albums only if not already loaded
            mSelectedArtist = artist;
            mPlayerAdapter.setCurrentArtist(mSelectedArtist);
            getSupportLoaderManager().restartLoader(AlbumProvider.ALBUMS_LOADER, null, this);
        } else {
            //if already loaded expand the panel
            mSlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        }
    }

    @Override
    public void onAlbumSelected(Album album) {
        if (mSongsAdapter != null) {
            mSongsAdapter.swapSongs(album.songs);

        } else {
            mSongsAdapter = new SongsAdapter(this, album.songs);
            mSongsRecyclerView.setAdapter(mSongsAdapter);
        }
    }

    public class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                if (AndroidVersion.isNougat()) {
                    mSeekBarAudio.setProgress(position, true);
                } else {
                    mSeekBarAudio.setProgress(position);
                }
            }
        }

        @Override
        public void onStateChanged(@PlaybackStateCompat.State int state) {

            updatePlayingStatus();
            if (mPlayerAdapter.getState() != State.RESUMED && mPlayerAdapter.getState() != State.PAUSED) {
                updatePlayingInfo();
                mSeekBarAudio.setMax(mSelectedSong.duration);
                mDuration.setText(Song.formatDuration(mSelectedSong.duration));
            }
        }

        @Override
        public void onPlaybackCompleted() {
            updateResetStatus(true);
        }
    }
}
