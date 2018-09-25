package com.iven.musicplayergo;

import android.Manifest;
import android.animation.Animator;
import android.annotation.TargetApi;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.card.MaterialCardView;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.iven.musicplayergo.adapters.AlbumsAdapter;
import com.iven.musicplayergo.adapters.ArtistsAdapter;
import com.iven.musicplayergo.adapters.ColorsAdapter;
import com.iven.musicplayergo.adapters.SongsAdapter;
import com.iven.musicplayergo.indexbar.IndexBarRecyclerView;
import com.iven.musicplayergo.indexbar.IndexBarView;
import com.iven.musicplayergo.loaders.ArtistProvider;
import com.iven.musicplayergo.loaders.SongProvider;
import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Artist;
import com.iven.musicplayergo.models.Song;
import com.iven.musicplayergo.playback.EqualizerUtils;
import com.iven.musicplayergo.playback.MusicNotificationManager;
import com.iven.musicplayergo.playback.MusicService;
import com.iven.musicplayergo.playback.PlaybackInfoListener;
import com.iven.musicplayergo.playback.PlayerAdapter;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Artist>>, SongsAdapter.SongSelectedListener, ColorsAdapter.AccentChangedListener, AlbumsAdapter.AlbumSelectedListener, ArtistsAdapter.ArtistSelectedListener {

    private LinearLayoutManager mArtistsLayoutManager, mAlbumsLayoutManager, mSongsLayoutManager;
    private int mAccent;
    private boolean sThemeInverted;
    private IndexBarRecyclerView mArtistsRecyclerView;
    private RecyclerView mAlbumsRecyclerView, mSongsRecyclerView;
    private ArtistsAdapter mArtistsAdapter;
    private SongsAdapter mSongsAdapter;
    private TextView mPlayingAlbum, mPlayingSong, mDuration, mSongPosition, mSelectedDiscographyArtist, mSelectedArtistDiscCount, mSelectedDiscographyDisc, mSelectedDiscographyDiscYear;

    private SeekBar mSeekBarAudio;
    private LinearLayout mControlsContainer;
    private BottomSheetBehavior mBottomSheetBehaviour;
    private View mSettingsView, mPlayerInfoView, mArtistDetails;
    private ImageView mPlayPauseButton, mSkipPrevButton;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;
    private List<Artist> mArtists;
    private String mNavigationArtist;
    private boolean sExpandArtistDiscography = false;
    private boolean sPlayerInfoLongPressed = false;
    private boolean sArtistDiscographyDiscLongPressed = false;
    private boolean sArtistDiscographyExpanded = false;
    private MusicService mMusicService;
    private PlaybackListener mPlaybackListener;
    private List<Song> mSelectedArtistSongs;
    private MusicNotificationManager mMusicNotificationManager;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(@NonNull final ComponentName componentName, @NonNull final IBinder iBinder) {
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
        public void onServiceDisconnected(@NonNull final ComponentName componentName) {
            mMusicService = null;
        }
    };
    private boolean mIsBound;
    private Parcelable mSavedArtistRecyclerLayoutState;
    private Parcelable mSavedAlbumsRecyclerLayoutState;
    private Parcelable mSavedSongRecyclerLayoutState;

    @Override
    public void onPause() {
        super.onPause();
        if (mArtistsLayoutManager != null && mAlbumsLayoutManager != null && mSongsLayoutManager != null) {
            mSavedArtistRecyclerLayoutState = mArtistsLayoutManager.onSaveInstanceState();
            mSavedAlbumsRecyclerLayoutState = mAlbumsLayoutManager.onSaveInstanceState();
            mSavedSongRecyclerLayoutState = mSongsLayoutManager.onSaveInstanceState();
        }
        if (mPlayerAdapter != null && mPlayerAdapter.isMediaPlayer()) {
            mPlayerAdapter.onPauseActivity();
        }
    }

    @Override
    public void onAccentChanged(final int color) {
        mMusicNotificationManager.setAccentColor(color);
        if (mPlayerAdapter.isMediaPlayer()) {
            mMusicNotificationManager.getNotificationManager().notify(MusicNotificationManager.NOTIFICATION_ID, mMusicNotificationManager.createNotification());
        }
        Utils.setThemeAccent(this, color);
    }

    @Override
    public void onBackPressed() {
        //if the bottom sheet is expanded collapse it
        if (mBottomSheetBehaviour.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehaviour.setState(BottomSheetBehavior.STATE_COLLAPSED);
            //then collapse the artist discography view
        } else if (sArtistDiscographyExpanded) {
            revealView(mArtistDetails, mArtistsRecyclerView, false);
        } else {
            super.onBackPressed();
        }
    }

    private void checkReadStoragePermissions() {
        if (Utils.isMarshmallow()) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                showPermissionRationale();
            } else {
                onPermissionGranted();
            }
        } else {
            onPermissionGranted();
        }
    }

    @TargetApi(23)
    private void showPermissionRationale() {
        final AlertDialog builder = new AlertDialog.Builder(this).create();
        View view = View.inflate(this, R.layout.dialog_one_button, null);
        builder.setIcon(R.drawable.ic_folder);
        builder.setView(view);
        if (builder.getWindow() != null) {
            builder.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        TextView title, message;
        title = view.findViewById(R.id.title);
        message = view.findViewById(R.id.message);
        title.setText(getString(R.string.app_name));
        message.setText(getString(R.string.perm_rationale));

        Button positiveButton = view.findViewById(R.id.dlg_one_button_btn_ok);
        positiveButton.setOnClickListener((View v) -> {
            builder.dismiss();
            final int READ_FILES_CODE = 2588;
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
                    , READ_FILES_CODE);
        });
        builder.setCanceledOnTouchOutside(false);
        try {
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            showPermissionRationale();
        } else {
            onPermissionGranted();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sThemeInverted = Utils.isThemeInverted(this);
        mAccent = Utils.getAccent(this);

        Utils.setTheme(this, sThemeInverted, mAccent);

        setContentView(R.layout.main_activity);

        getViews();

        initializeSettings();

        setupViewParams();

        initializeSeekBar();

        doBindService();
    }

    private void getViews() {

        mControlsContainer = findViewById(R.id.controls_container);
        findViewById(R.id.context_view).setBackgroundColor(ColorUtils.setAlphaComponent(Utils.getColorFromResource(this, mAccent, R.color.blue), sThemeInverted ? 10 : 40));

        MaterialCardView bottomSheetLayout = findViewById(R.id.design_bottom_sheet);
        mBottomSheetBehaviour = BottomSheetBehavior.from(bottomSheetLayout);
        mArtistDetails = findViewById(R.id.artist_details);
        mPlayerInfoView = findViewById(R.id.player_info);
        mPlayingSong = findViewById(R.id.playing_song);
        mPlayingAlbum = findViewById(R.id.playing_album);

        setupPlayerInfoTouchBehaviour();

        mPlayPauseButton = findViewById(R.id.play_pause);

        mSkipPrevButton = findViewById(R.id.skip_prev);
        mSkipPrevButton.setOnLongClickListener(v -> {
            setRepeat();
            return false;
        });
        mSeekBarAudio = findViewById(R.id.seekTo);

        mDuration = findViewById(R.id.duration);
        mSongPosition = findViewById(R.id.song_position);
        mSelectedDiscographyArtist = findViewById(R.id.selected_discography_artist);
        mSelectedArtistDiscCount = findViewById(R.id.selected_artist_album_count);
        mSelectedDiscographyDisc = findViewById(R.id.selected_disc);
        setupArtistDiscographyDiscBehaviour();
        mSelectedDiscographyDiscYear = findViewById(R.id.selected_disc_year);

        mArtistsRecyclerView = findViewById(R.id.artists_rv);
        mAlbumsRecyclerView = findViewById(R.id.albums_rv);
        mSongsRecyclerView = findViewById(R.id.songs_rv);

        mSettingsView = findViewById(R.id.settings_view);
    }

    //https://stackoverflow.com/questions/6183874/android-detect-end-of-long-press
    private void setupPlayerInfoTouchBehaviour() {
        mPlayerInfoView.setOnLongClickListener(v -> {
            if (!sPlayerInfoLongPressed) {
                mPlayingSong.setSelected(true);
                mPlayingAlbum.setSelected(true);
                sPlayerInfoLongPressed = true;
            }
            return true;
        });
        mPlayerInfoView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_OUTSIDE || event.getAction() == MotionEvent.ACTION_MOVE) {
                if (sPlayerInfoLongPressed) {
                    mPlayingSong.setSelected(false);
                    mPlayingAlbum.setSelected(false);
                    sPlayerInfoLongPressed = false;
                }
            }
            return false;
        });
    }

    private void setupArtistDiscographyDiscBehaviour() {
        mSelectedDiscographyDisc.setOnLongClickListener(v -> {
            if (!sArtistDiscographyDiscLongPressed) {
                mSelectedDiscographyDisc.setSelected(true);
                sArtistDiscographyDiscLongPressed = true;
            }
            return true;
        });
        mSelectedDiscographyDisc.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_OUTSIDE || event.getAction() == MotionEvent.ACTION_MOVE) {
                if (sArtistDiscographyDiscLongPressed) {
                    mSelectedDiscographyDisc.setSelected(false);
                    sArtistDiscographyDiscLongPressed = false;
                }
            }
            return false;
        });
    }

    private void setupViewParams() {
        final ViewTreeObserver observer = mControlsContainer.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int controlsContainerHeight = mControlsContainer.getHeight();

                //add bottom margin to those recycler view to avoid they are covered by bottom sheet
                FrameLayout.LayoutParams artistsLayoutParams = (FrameLayout.LayoutParams) mArtistsRecyclerView.getLayoutParams();
                artistsLayoutParams.bottomMargin = controlsContainerHeight;

                LinearLayout.LayoutParams songsLayoutParams = (LinearLayout.LayoutParams) mSongsRecyclerView.getLayoutParams();
                songsLayoutParams.bottomMargin = controlsContainerHeight;

                mBottomSheetBehaviour.setPeekHeight(controlsContainerHeight);
                mControlsContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void initializeSettings() {
        if (!EqualizerUtils.hasEqualizer(this)) {
            final ImageView eqButton = findViewById(R.id.eq);
            eqButton.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        }
        initializeColorsSettings();
    }

    public void shuffleSongs(@NonNull final View v) {
        final List<Song> songs = sArtistDiscographyExpanded ? mSelectedArtistSongs : SongProvider.getAllDeviceSongs();
        Collections.shuffle(songs);
        onSongSelected(songs.get(0), songs);
    }

    private void setArtistsRecyclerView(@NonNull final List<Artist> data) {
        mArtistsLayoutManager = new LinearLayoutManager(this);
        mArtistsRecyclerView.setLayoutManager(mArtistsLayoutManager);
        mArtistsAdapter = new ArtistsAdapter(this, data);
        mArtistsRecyclerView.setAdapter(mArtistsAdapter);
        // Set the FastScroller only if the RecyclerView is scrollable;
        setScrollerIfRecyclerViewScrollable();
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
                    IndexBarView indexBarView = new IndexBarView(mArtistsRecyclerView, mArtistsAdapter, mArtistsLayoutManager, sThemeInverted);
                    mArtistsRecyclerView.setFastScroller(indexBarView);
                }
            }
        });
    }

    private void initializeSeekBar() {
        mSeekBarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    final int currentPositionColor = mSongPosition.getCurrentTextColor();
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(@NonNull final SeekBar seekBar) {
                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(@NonNull final SeekBar seekBar, final int progress, final boolean fromUser) {
                        if (fromUser) {
                            userSelectedPosition = progress;
                            mSongPosition.setTextColor(Utils.getColorFromResource(MainActivity.this, mAccent, R.color.blue));
                        }
                        mSongPosition.setText(Song.formatDuration(progress));
                    }

                    @Override
                    public void onStopTrackingTouch(@NonNull final SeekBar seekBar) {
                        if (mUserIsSeeking) {
                            mSongPosition.setTextColor(currentPositionColor);
                        }
                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    private void setRepeat() {
        if (checkIsPlayer()) {
            mPlayerAdapter.reset();
            updateResetStatus(false);
        }
    }

    public void skipPrev(@NonNull final View v) {
        if (checkIsPlayer()) {
            mPlayerAdapter.instantReset();
            if (mPlayerAdapter.isReset()) {
                mPlayerAdapter.reset();
                updateResetStatus(false);
            }
        }
    }

    public void resumeOrPause(@NonNull final View v) {
        if (checkIsPlayer()) {
            mPlayerAdapter.resumeOrPause();
        }
    }

    public void skipNext(@NonNull final View v) {
        if (checkIsPlayer()) {
            mPlayerAdapter.skip(true);
        }
    }

    public void openEqualizer(@NonNull final View v) {
        if (EqualizerUtils.hasEqualizer(this)) {
            if (checkIsPlayer()) {
                mPlayerAdapter.openEqualizer(MainActivity.this);
            }
        } else {
            Toast.makeText(this, getString(R.string.no_eq), Toast.LENGTH_SHORT).show();
        }
    }

    public void openGitPage(@NonNull final View v) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/enricocid/Music-Player-GO")));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.no_browser), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void switchTheme(@NonNull final View v) {
        //avoid service killing when the player is in paused state
        if (mPlayerAdapter != null && mPlayerAdapter.getState() == PlaybackInfoListener.State.PAUSED) {
            mMusicService.startForeground(MusicNotificationManager.NOTIFICATION_ID, mMusicService.getMusicNotificationManager().createNotification());
            mMusicService.setRestoredFromPause(true);
        }
        Utils.invertTheme(this);
    }

    private boolean checkIsPlayer() {

        boolean isPlayer = mPlayerAdapter.isMediaPlayer();
        if (!isPlayer) {
            EqualizerUtils.notifyNoSessionId(this);
        }
        return isPlayer;
    }

    private void initializeColorsSettings() {
        final RecyclerView colorsRecyclerView = mSettingsView.findViewById(R.id.colors_rv);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        colorsRecyclerView.setLayoutManager(linearLayoutManager);
        colorsRecyclerView.setAdapter(new ColorsAdapter(this, mAccent));
    }

    @SuppressWarnings("deprecation")
    private void onPermissionGranted() {
        getSupportLoaderManager().initLoader(ArtistProvider.ARTISTS_LOADER, null, this);
    }

    private void updateResetStatus(final boolean onPlaybackCompletion) {
        final int themeColor = sThemeInverted ? R.color.white : R.color.black;
        final int color = onPlaybackCompletion ? themeColor : mPlayerAdapter.isReset() ? mAccent : themeColor;
        mSkipPrevButton.post(() -> mSkipPrevButton.setColorFilter(Utils.getColorFromResource(MainActivity.this, color, onPlaybackCompletion ? themeColor : mPlayerAdapter.isReset() ? R.color.blue : themeColor), PorterDuff.Mode.SRC_IN));
    }

    private void updatePlayingStatus() {
        final int drawable = mPlayerAdapter.getState() != PlaybackInfoListener.State.PAUSED ? R.drawable.ic_pause : R.drawable.ic_play;
        mPlayPauseButton.post(() -> mPlayPauseButton.setImageResource(drawable));
    }

    private void updatePlayingInfo(final boolean restore, final boolean startPlay) {

        if (startPlay) {
            mPlayerAdapter.getMediaPlayer().start();
            new Handler().postDelayed(() -> mMusicService.startForeground(MusicNotificationManager.NOTIFICATION_ID, mMusicNotificationManager.createNotification()), 250);
        }

        final Song selectedSong = mPlayerAdapter.getCurrentSong();

        final int duration = selectedSong.duration;
        mSeekBarAudio.setMax(duration);
        Utils.updateTextView(mDuration, Song.formatDuration(duration));

        final Spanned spanned = Utils.buildSpanned(getString(R.string.playing_song, selectedSong.artistName, selectedSong.title));

        mPlayingSong.post(() -> mPlayingSong.setText(spanned));

        Utils.updateTextView(mPlayingAlbum, selectedSong.albumName);

        if (restore) {
            mSeekBarAudio.setProgress(mPlayerAdapter.getPlayerPosition());
            updatePlayingStatus();
            updateResetStatus(false);

            new Handler().postDelayed(() -> {
                //stop foreground if coming from pause state
                if (mMusicService.isRestoredFromPause()) {
                    mMusicService.stopForeground(false);
                    mMusicService.getMusicNotificationManager().getNotificationManager().notify(MusicNotificationManager.NOTIFICATION_ID, mMusicService.getMusicNotificationManager().getNotificationBuilder().build());
                    mMusicService.setRestoredFromPause(false);
                }
            }, 250);
        }
    }

    private void restorePlayerStatus() {

        mSeekBarAudio.setEnabled(mPlayerAdapter.isMediaPlayer());

        //if we are playing and the activity was restarted
        //update the controls panel
        if (mPlayerAdapter != null && mPlayerAdapter.isMediaPlayer()) {
            mPlayerAdapter.onResumeActivity();
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

        final Intent startNotStickyIntent = new Intent(this, MusicService.class);
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
    public Loader<List<Artist>> onCreateLoader(final int id, final Bundle args) {
        return new ArtistProvider.AsyncArtistLoader(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Artist>> loader, @NonNull final List<Artist> artists) {

        if (artists.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_no_music), Toast.LENGTH_SHORT)
                    .show();
            finish();

        } else {
            mArtists = artists;
            setArtistsRecyclerView(mArtists);

            mNavigationArtist = mPlayerAdapter.getNavigationArtist() != null ? mPlayerAdapter.getNavigationArtist() : mArtists.get(0).getName();

            setArtistDetails(ArtistProvider.getArtist(mArtists, mNavigationArtist).albums, false, false);
            restorePlayerStatus();

            if (mSavedArtistRecyclerLayoutState != null && mSavedAlbumsRecyclerLayoutState != null && mSavedSongRecyclerLayoutState != null) {
                mArtistsLayoutManager.onRestoreInstanceState(mSavedArtistRecyclerLayoutState);
                mAlbumsLayoutManager.onRestoreInstanceState(mSavedAlbumsRecyclerLayoutState);
                mSongsLayoutManager.onRestoreInstanceState(mSavedSongRecyclerLayoutState);
            }
        }
    }

    private void setArtistDetails(@NonNull List<Album> albums, final boolean isNewArtist, final boolean showPlayedArtist) {
        if (showPlayedArtist) {
            Artist artist = ArtistProvider.getArtist(mArtists, mPlayerAdapter.getCurrentSong().artistName);
            albums = artist.albums;
        }
        if (isNewArtist) {
            mAlbumsRecyclerView.scrollToPosition(0);
        }
        mAlbumsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mAlbumsRecyclerView.setLayoutManager(mAlbumsLayoutManager);
        AlbumsAdapter albumsAdapter = new AlbumsAdapter(this, mPlayerAdapter, albums, showPlayedArtist);
        mAlbumsRecyclerView.setAdapter(albumsAdapter);

        mSelectedArtistSongs = SongProvider.getAllArtistSongs(albums);
        Utils.updateTextView(mSelectedDiscographyArtist, mNavigationArtist);
        Utils.updateTextView(mSelectedArtistDiscCount, getString(R.string.albums, albums.size()));

        if (sExpandArtistDiscography) {
            revealView(mArtistDetails, mArtistsRecyclerView, true);
            sExpandArtistDiscography = false;
        }
    }

    @Override
    public void onLoaderReset(@NonNull final Loader loader) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlaybackListener = null;
        doUnbindService();
    }

    @Override
    public void onSongSelected(@NonNull final Song playedSong, @NonNull final List<Song> songsForPlayedArtist) {
        if (!mSeekBarAudio.isEnabled()) {
            mSeekBarAudio.setEnabled(true);
        }
        mPlayerAdapter.setCurrentSong(playedSong, songsForPlayedArtist);
        mPlayerAdapter.initMediaPlayer(playedSong);
    }

    @Override
    public void onArtistSelected(@NonNull final String selectedArtist, final boolean showPlayedArtist) {

        if (!mNavigationArtist.equals(selectedArtist)) {
            //make the panel expandable
            sExpandArtistDiscography = true;

            //load artist albums only if not already loaded
            mNavigationArtist = selectedArtist;
            mPlayerAdapter.setNavigationArtist(selectedArtist);
            mPlayerAdapter.setNavigationAlbum(null);
            setArtistDetails(ArtistProvider.getArtist(mArtists, selectedArtist).albums, true, showPlayedArtist);
        } else {
            //if already loaded expand the panel
            revealView(mArtistDetails, mArtistsRecyclerView, true);
        }
    }

    @Override
    public void onAlbumSelected(@NonNull final Album album) {
        mPlayerAdapter.setNavigationAlbum(album);
        Utils.updateTextView(mSelectedDiscographyDisc, album.getTitle());
        Utils.updateTextView(mSelectedDiscographyDiscYear, Album.getYearForAlbum(this, album.getYear()));

        if (mSongsAdapter != null) {
            mSongsRecyclerView.scrollToPosition(0);
            mSongsAdapter.swapSongs(album);
        } else {
            mSongsLayoutManager = new LinearLayoutManager(this);
            mSongsRecyclerView.setLayoutManager(mSongsLayoutManager);
            mSongsAdapter = new SongsAdapter(this, album);
            mSongsRecyclerView.setAdapter(mSongsAdapter);
        }
    }

    private Pair<Album, List<Album>> scrollToPlayedAlbumPosition(final boolean setArtistDetails) {
        List<Album> playedArtistAlbums = ArtistProvider.getArtist(mArtists, mPlayerAdapter.getCurrentSong().artistName).albums;
        Pair<Album, Integer> playedAlbumPosition = playedAlbumPosition(playedArtistAlbums);
        if (setArtistDetails) {
            setArtistDetails(playedArtistAlbums, false, true);
        }
        mAlbumsRecyclerView.scrollToPosition(playedAlbumPosition.second);
        return new Pair<>(playedAlbumPosition.first, playedArtistAlbums);
    }

    private Pair<Album, Integer> playedAlbumPosition(@NonNull final List<Album> playedArtistAlbums) {
        Album playedAlbum = mPlayerAdapter.getCurrentSong().getSongAlbum();
        return new Pair<>(playedAlbum, playedArtistAlbums.indexOf(playedAlbum));
    }

    public void expandArtistDetails(@NonNull final View v) {

        if (mPlayerAdapter.getCurrentSong() != null && !mPlayerAdapter.getCurrentSong().artistName.equals(mNavigationArtist)) {
            // if we come from different artist show the played artist page
            onArtistSelected(mPlayerAdapter.getCurrentSong().artistName, true);
            if (mPlayerAdapter.getCurrentSong() != null) {
                scrollToPlayedAlbumPosition(false);
            }
        } else if (sArtistDiscographyExpanded) {
            if (mPlayerAdapter.getCurrentSong() != null && mPlayerAdapter.getCurrentSong().artistName.equals(mNavigationArtist) && !mPlayerAdapter.getNavigationAlbum().equals(mPlayerAdapter.getCurrentSong().getSongAlbum())) {
                // if the played artist details are already expanded (but not on played album)
                // and we are playing one of his albums, the show the played album
                mPlayerAdapter.setNavigationAlbum(scrollToPlayedAlbumPosition(true).first);
            } else {
                // if the the played artist details are already expanded
                // but the navigation album equals the played album:
                if (mPlayerAdapter.getCurrentSong() != null && mPlayerAdapter.getCurrentSong().artistName.equals(mNavigationArtist) && mPlayerAdapter.getCurrentSong().artistName.equals(mNavigationArtist)) {
                    List<Album> playedArtistAlbums = ArtistProvider.getArtist(mArtists, mPlayerAdapter.getCurrentSong().artistName).albums;
                    Pair<Album, Integer> playedAlbumPosition = playedAlbumPosition(playedArtistAlbums);
                    // if we are scrolling the albums recycler view and the played album
                    // is visible then close the artist details
                    if ((playedAlbumPosition.second >= mAlbumsLayoutManager.findFirstVisibleItemPosition() && playedAlbumPosition.second <= mAlbumsLayoutManager.findLastVisibleItemPosition())) {
                        revealView(mArtistDetails, mArtistsRecyclerView, false);
                    } else {
                        // else scroll to the album position!
                        scrollToPlayedAlbumPosition(false);
                    }
                } else {
                    // else else else do whatever You think is right with the reveal animation!
                    // we are not playing anything in this phase
                    revealView(mArtistDetails, mArtistsRecyclerView, !sArtistDiscographyExpanded);
                }
            }
        } else {
            if (mPlayerAdapter.getCurrentSong() != null && mPlayerAdapter.getCurrentSong().artistName.equals(mNavigationArtist)) {
                scrollToPlayedAlbumPosition(false);
            }
            revealView(mArtistDetails, mArtistsRecyclerView, true);
        }
    }

    public void closeArtistDetails(@NonNull final View v) {
        if (sArtistDiscographyExpanded) {
            revealView(mArtistDetails, mArtistsRecyclerView, false);
        }
    }

    private void revealView(@NonNull final View viewToReveal, @NonNull final View viewToHide, final boolean show) {

        final int ANIMATION_DURATION = 500;
        final int viewToRevealHeight = viewToReveal.getHeight();
        final int viewToRevealWidth = viewToReveal.getWidth();
        final int viewToRevealHalfWidth = viewToRevealWidth / 2;
        final int radius = (int) Math.hypot(viewToRevealWidth, viewToRevealHeight);
        final int fromY = viewToHide.getTop() / 2;

        if (show) {
            final Animator anim = ViewAnimationUtils.createCircularReveal(viewToReveal, viewToRevealHalfWidth, fromY, 0, radius);
            anim.setDuration(ANIMATION_DURATION);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    viewToReveal.setVisibility(View.VISIBLE);
                    viewToHide.setVisibility(View.INVISIBLE);
                    viewToReveal.setClickable(false);
                }

                @Override
                public void onAnimationEnd(@NonNull final Animator animator) {
                    sArtistDiscographyExpanded = true;
                }

                @Override
                public void onAnimationCancel(@NonNull final Animator animator) {
                }

                @Override
                public void onAnimationRepeat(@NonNull final Animator animator) {
                }
            });
            anim.start();

        } else {

            final Animator anim = ViewAnimationUtils.createCircularReveal(viewToReveal, viewToRevealHalfWidth, fromY, radius, 0);
            anim.setDuration(ANIMATION_DURATION);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(@NonNull final Animator animator) {
                    sArtistDiscographyExpanded = false;
                }

                @Override
                public void onAnimationEnd(@NonNull final Animator animator) {
                    viewToReveal.setVisibility(View.INVISIBLE);
                    viewToHide.setVisibility(View.VISIBLE);
                    viewToReveal.setClickable(true);
                    sArtistDiscographyExpanded = false;
                }

                @Override
                public void onAnimationCancel(@NonNull final Animator animator) {
                }

                @Override
                public void onAnimationRepeat(@NonNull final Animator animator) {
                }
            });
            anim.start();
        }
    }

    class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                mSeekBarAudio.setProgress(position);
            }
        }

        @Override
        public void onStateChanged(@State int state) {

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
