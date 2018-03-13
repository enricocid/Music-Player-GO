package com.iven.musicplayergo;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.iven.musicplayergo.adapters.AlbumsAdapter;
import com.iven.musicplayergo.adapters.ArtistsAdapter;
import com.iven.musicplayergo.adapters.ColorsAdapter;
import com.iven.musicplayergo.adapters.SongsAdapter;
import com.iven.musicplayergo.fastscroller.FastScrollerRecyclerView;
import com.iven.musicplayergo.fastscroller.FastScrollerView;
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
import com.iven.musicplayergo.slidinguppanel.SlidingUpPanelLayout;
import com.iven.musicplayergo.utils.AndroidVersion;
import com.iven.musicplayergo.utils.PermissionDialog;
import com.iven.musicplayergo.utils.SettingsUtils;

import java.util.List;

public class MainActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks, SongsAdapter.songSelectedListener, ColorsAdapter.onAccentChangedListener, AlbumsAdapter.albumSelectedListener, ArtistsAdapter.artistSelectedListener {

    LinearLayoutManager mArtistsLayoutManager;
    ArtistsAdapter mArtistsAdapter;
    private int mAccent;
    private FastScrollerRecyclerView mArtistsRecyclerView;
    private RecyclerView mAlbumsRecyclerView, mSongsRecyclerView;
    private AlbumsAdapter mAlbumsAdapter;
    private SongsAdapter mSongsAdapter;
    private TextView mPlayingAlbum, mPlayingSong, mDuration, mSongPosition;
    private SeekBar mSeekBarAudio;
    private LinearLayout mControlsContainer;
    private View mSettingsView;
    private SlidingUpPanelLayout mSlidingUpPanel;
    private ImageButton mPlayPauseButton, mResetButton, mEqButton, mArrowUp, mSettingsButton, mThemeButton;
    private int mThemeContrast;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;
    private Song mSelectedSong;
    private String mSelectedArtist;
    private boolean sExpandPanel = false;
    private MusicService mMusicService;
    private PlaybackListener mPlaybackListener;
    private MusicNotificationManager mMusicNotificationManager;
    private boolean mIsBound;

    private Parcelable savedRecyclerLayoutState;

    private PopupWindow mSettingsPopup;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            mMusicService = ((MusicService.LocalBinder) iBinder).getInstance();
            mPlayerAdapter = mMusicService.getMediaPlayerHolder();
            mMusicNotificationManager = mMusicService.getMusicNotificationManager();
            mMusicNotificationManager.setAccentColor(mAccent);

            if (mPlaybackListener == null) {
                mPlaybackListener = new PlaybackListener();
                mPlayerAdapter.setPlaybackInfoListener(mPlaybackListener);
            }
            checkReadStoragePermissions();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mMusicService = null;
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (mArtistsLayoutManager != null) {
            savedRecyclerLayoutState = mArtistsLayoutManager.onSaveInstanceState();
        }
        if (mSettingsPopup.isShowing()) {
            mSettingsPopup.dismiss();
        }
    }

    @Override
    public void onAccentChanged(int color) {
        mMusicNotificationManager.setAccentColor(color);
        if (mPlayerAdapter.isMediaPlayer()) {
            mMusicNotificationManager.getNotificationManager().notify(MusicNotificationManager.NOTIFICATION_ID, mMusicNotificationManager.createNotification());
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

    private void checkReadStoragePermissions() {
        if (AndroidVersion.isMarshmallow()) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                PermissionDialog.showPermissionDialog(getSupportFragmentManager());
            } else {

                onPermissionGranted();
            }
        } else {
            onPermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            PermissionDialog.showPermissionDialog(getSupportFragmentManager());
        } else {
            onPermissionGranted();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mThemeContrast = SettingsUtils.getContrast(this);
        mAccent = SettingsUtils.getAccent(this);

        SettingsUtils.retrieveTheme(this, mThemeContrast, mAccent);

        setContentView(R.layout.main_activity);

        getViews();

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if (AndroidVersion.isMarshmallow()) {
                            //check if we have to enable light status bar
                            SettingsUtils.enableLightStatusBar(MainActivity.this, ContextCompat.getColor(MainActivity.this, mAccent));
                        }
                    }
                });

        mSlidingUpPanel.setSlidingUpPanel(mSongsRecyclerView, mArtistsRecyclerView, mArrowUp);
        mSlidingUpPanel.setGravity(Gravity.BOTTOM);

        initializeSettings();

        setSlidingUpPanelHeight();

        initializeSeekBar();

        doBindService();
    }

    private void getViews() {

        mSlidingUpPanel = findViewById(R.id.sliding_panel);

        mControlsContainer = findViewById(R.id.controls_container);

        mArrowUp = findViewById(R.id.arrow_up);
        mPlayPauseButton = findViewById(R.id.play_pause);
        mResetButton = findViewById(R.id.replay);
        mSettingsButton = findViewById(R.id.settings);
        mSeekBarAudio = findViewById(R.id.seekTo);

        mPlayingSong = findViewById(R.id.playing_song);
        mPlayingAlbum = findViewById(R.id.playing_album);
        mDuration = findViewById(R.id.duration);
        mSongPosition = findViewById(R.id.song_position);

        mArtistsRecyclerView = findViewById(R.id.artists_rv);
        mAlbumsRecyclerView = findViewById(R.id.albums_rv);
        mSongsRecyclerView = findViewById(R.id.songs_rv);

        mSettingsView = View.inflate(this, R.layout.settings_popup, null);

        mEqButton = mSettingsView.findViewById(R.id.eq);
        mThemeButton = mSettingsView.findViewById(R.id.theme_button);
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

    private void initializeSettings() {
        if (!EqualizerUtils.hasEqualizer(this)) {
            mControlsContainer.removeView(mEqButton);
        }

        initializeColorsSettings();

        mSettingsPopup = new PopupWindow(mSettingsView, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true); // Creation of popup
        mSettingsPopup.setOutsideTouchable(true);
        mSettingsPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mSettingsButton.setImageResource(R.drawable.ic_settings);
            }
        });
        mSettingsPopup.setElevation(6);

        if (mThemeContrast != SettingsUtils.THEME_NIGHT) {
            mThemeButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    SettingsUtils.setNightTheme(MainActivity.this);
                    return false;
                }
            });
        }
    }

    public void showSettingsPopup(View v) {

        mSettingsPopup.setAnimationStyle(android.R.style.Animation_Translucent);
        if (!mSettingsPopup.isShowing()) {
            mSettingsButton.setImageResource(R.drawable.ic_close);
            mSettingsPopup.showAtLocation(mSettingsView, Gravity.CENTER, 0, 0);
        } else {
            mSettingsPopup.dismiss();
        }
    }

    private void setScrollerIfRecyclerViewScrollable() {

        // ViewTreeObserver allows us to measure the layout params
        final ViewTreeObserver observer = mArtistsRecyclerView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                int h = mArtistsRecyclerView.getHeight();
                mArtistsRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (mArtistsRecyclerView.computeVerticalScrollRange() > h) {
                    FastScrollerView fastScrollerView = new FastScrollerView(mArtistsRecyclerView, mArtistsAdapter, mArtistsLayoutManager, ContextCompat.getColor(MainActivity.this, mAccent), mThemeContrast);
                    mArtistsRecyclerView.setFastScroller(fastScrollerView);
                }
            }
        });
    }

    void setArtistsRecyclerView(List<Artist> data) {

        mArtistsLayoutManager = new LinearLayoutManager(this);
        mArtistsRecyclerView.setLayoutManager(mArtistsLayoutManager);
        mArtistsAdapter = new ArtistsAdapter(this, data);
        mArtistsRecyclerView.setAdapter(mArtistsAdapter);

        if (savedRecyclerLayoutState != null) {
            mArtistsLayoutManager.onRestoreInstanceState(savedRecyclerLayoutState);
        }

        // Set the FastScroller only if the RecyclerView is scrollable;
        setScrollerIfRecyclerViewScrollable();
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

    public void expandPanel(View v) {
        SlidingUpPanelLayout.PanelState panelState = mSlidingUpPanel.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED ? SlidingUpPanelLayout.PanelState.EXPANDED : SlidingUpPanelLayout.PanelState.COLLAPSED;
        mSlidingUpPanel.setPanelState(panelState);
    }

    private boolean checkIsPlayer() {

        boolean isPlayer = mPlayerAdapter.isMediaPlayer();
        if (!isPlayer) {
            EqualizerUtils.notifyNoSessionId(MainActivity.this);
        }
        return isPlayer;
    }

    public void switchTheme(View v) {
        //avoid service killing when the player is in paused state
        if (mPlayerAdapter != null && mPlayerAdapter.getState() == PlaybackInfoListener.State.PAUSED) {
            mMusicService.startForeground(MusicNotificationManager.NOTIFICATION_ID, mMusicService.getMusicNotificationManager().createNotification());
            mMusicService.setRestoredFromPause(true);
        }
        SettingsUtils.setTheme(MainActivity.this);
    }

    private void initializeColorsSettings() {

        RecyclerView colorsRecyclerView = mSettingsView.findViewById(R.id.colors_rv);
        LinearLayoutManager linearLayoutManager = new GridLayoutManager(this, 5);
        colorsRecyclerView.setLayoutManager(linearLayoutManager);
        colorsRecyclerView.setAdapter(new ColorsAdapter(this, mAccent));
    }

    private void onPermissionGranted() {
        getSupportLoaderManager().initLoader(ArtistProvider.ARTISTS_LOADER, null, this);
    }

    private void updateResetStatus(boolean onPlaybackCompletion) {

        int themeColor = mThemeContrast != SettingsUtils.THEME_LIGHT ? ContextCompat.getColor(this, R.color.grey_200) : ContextCompat.getColor(this, R.color.grey_900_darker);
        int color = onPlaybackCompletion ? themeColor : mPlayerAdapter.isReset() ? ContextCompat.getColor(this, mAccent) : themeColor;
        mResetButton.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private void updatePlayingStatus() {
        int drawable = mPlayerAdapter.getState() != PlaybackInfoListener.State.PAUSED ? R.drawable.ic_pause : R.drawable.ic_play;
        mPlayPauseButton.setImageResource(drawable);
    }

    private void updatePlayingInfo(boolean restore, boolean startPlay) {

        if (startPlay) {
            mPlayerAdapter.getMediaPlayer().start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mMusicService.startForeground(MusicNotificationManager.NOTIFICATION_ID, mMusicNotificationManager.createNotification());
                }
            }, 250);
        }
        if (restore) {
            mSelectedSong = mPlayerAdapter.getCurrentSong();
        }
        mSelectedArtist = mSelectedSong.artistName;
        final int duration = mSelectedSong.duration;
        mSeekBarAudio.setMax(duration);
        mDuration.setText(Song.formatDuration(duration));

        Spanned spanned = AndroidVersion.isNougat() ?
                Html.fromHtml(getString(R.string.playing_song, mSelectedArtist, mSelectedSong.title), Html.FROM_HTML_MODE_LEGACY) :
                Html.fromHtml(getString(R.string.playing_song, mSelectedArtist, mSelectedSong.title));
        mPlayingSong.setText(spanned);
        mPlayingAlbum.setText(mSelectedSong.albumName);

        if (restore) {
            mPlayingAlbum.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    mSeekBarAudio.setProgress(mPlayerAdapter.getPlayerPosition());

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            updatePlayingStatus();
                            updateResetStatus(false);
                            //stop foreground if coming from pause state
                            if (mMusicService.isRestoredFromPause()) {
                                mMusicService.stopForeground(false);
                                mMusicService.getMusicNotificationManager().getNotificationManager().notify(MusicNotificationManager.NOTIFICATION_ID, mMusicService.getMusicNotificationManager().getNotificationBuilder().build());
                                mMusicService.setRestoredFromPause(false);
                            }
                        }
                    }, 250);
                }
            });
        }
    }

    private void restorePlayerStatus() {

        mSeekBarAudio.setEnabled(mPlayerAdapter.isMediaPlayer());

        //if we are playing and the activity was restarted
        //update the controls panel
        if (mPlayerAdapter.isMediaPlayer()) {

            updatePlayingInfo(true, false);
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

        Intent startNotStickyIntent = new Intent(this, MusicService.class);
        startService(startNotStickyIntent);
    }

    private void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    @NonNull
    public Loader onCreateLoader(int id, Bundle args) {
        return id != AlbumProvider.ALBUMS_LOADER ? new ArtistProvider.AsyncArtistLoader(this) : new AlbumProvider.AsyncAlbumsForArtistLoader(this, mSelectedArtist);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadFinished(@NonNull Loader loader, Object data) {

        switch (loader.getId()) {
            case ArtistProvider.ARTISTS_LOADER:

                //get loaded artist list and set the artists recycler view
                List<Artist> artists = (List<Artist>) data;

                if (artists.isEmpty()) {

                    Toast.makeText(this, getString(R.string.error_no_music), Toast.LENGTH_SHORT)
                            .show();
                    finish();

                } else {
                    setArtistsRecyclerView(artists);

                    //load the details of the first artist on list
                    mSelectedArtist = mPlayerAdapter.isMediaPlayer() ? mPlayerAdapter.getCurrentSong().artistName : artists.get(0).getName();
                    getSupportLoaderManager().initLoader(AlbumProvider.ALBUMS_LOADER, null, this);
                }
                break;

            case AlbumProvider.ALBUMS_LOADER:

                //get loaded albums for artist
                Pair<Artist, List<Album>> albumsForArtist = (Pair<Artist, List<Album>>) data;

                if (mAlbumsAdapter != null) {
                    //only notify recycler view of item changed if an adapter already exists
                    mAlbumsAdapter.swapArtist(albumsForArtist);
                } else {
                    LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                    mAlbumsRecyclerView.setLayoutManager(horizontalLayoutManager);
                    mAlbumsAdapter = new AlbumsAdapter(this, mAlbumsRecyclerView, albumsForArtist, mPlayerAdapter);
                    mAlbumsRecyclerView.setAdapter(mAlbumsAdapter);
                }
                if (sExpandPanel) {
                    mSlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                    sExpandPanel = false;
                } else {
                    restorePlayerStatus();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader loader) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlaybackListener = null;
        doUnbindService();
    }

    @Override
    public void onSongSelected(Song song, Album album) {

        if (!mSeekBarAudio.isEnabled()) {
            mSeekBarAudio.setEnabled(true);
        }
        mSelectedSong = song;
        mPlayerAdapter.setCurrentSong(mSelectedSong, album.songs);
        mPlayerAdapter.setPlayingAlbum(album);
        mPlayerAdapter.initMediaPlayer();
    }

    @Override
    public void onArtistSelected(String artist) {

        if (!mSelectedArtist.equals(artist)) {

            //make the panel expandable
            sExpandPanel = true;

            mPlayerAdapter.setSelectedAlbum(null);

            //load artist albums only if not already loaded
            mSelectedArtist = artist;
            getSupportLoaderManager().restartLoader(AlbumProvider.ALBUMS_LOADER, null, this);
        } else {
            //if already loaded expand the panel
            mSlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        }
    }

    @Override
    public void onAlbumSelected(Album album) {

        if (mSongsAdapter != null) {
            mSongsAdapter.swapSongs(album);
        } else {
            LinearLayoutManager songsLayoutManager = new LinearLayoutManager(this);
            mSongsRecyclerView.setLayoutManager(songsLayoutManager);
            mSongsAdapter = new SongsAdapter(this, album);
            mSongsRecyclerView.setAdapter(mSongsAdapter);
        }
    }

    public class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                mSeekBarAudio.setProgress(position);
            }
        }

        @Override
        public void onStateChanged(@PlaybackStateCompat.State int state) {

            updatePlayingStatus();
            if (mPlayerAdapter.getState() != State.RESUMED && mPlayerAdapter.getState() != State.PAUSED) {
                updatePlayingInfo(false, true);
            }
        }

        @Override
        public void onPlaybackCompleted() {
            updateResetStatus(true);
        }
    }
}
