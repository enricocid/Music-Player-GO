package com.iven.musicplayergo.models;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

public class Song {

    static final Song EMPTY_SONG = new Song("", -1, -1, -1, null, "", -1, "");

    private final String mTitle;
    private final int mTrackNumber;
    private final int mDuration;
    private final String mPath;
    private final String mAlbumName;
    private final int mArtistId;
    private final String mArtistName;
    private final int mYear;
    private Album mSongAlbum;

    public Song(@NonNull final String title, final int trackNumber, final int year, final int duration, final String path, final String albumName, final int artistId, final String artistName) {
        mTitle = title;
        mTrackNumber = trackNumber;
        mYear = year;
        mDuration = duration;
        mPath = path;
        mAlbumName = albumName;
        mArtistId = artistId;
        mArtistName = artistName;
    }

    @NonNull
    public static String formatDuration(final int duration) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
    }

    public static int formatTrack(final int trackNumber) {
        int formatted = trackNumber;
        if (trackNumber >= 1000) {
            formatted = trackNumber % 1000;
        }
        return formatted;
    }

    @NonNull
    public final String getSongTitle() {
        return mTitle;
    }

    public final int getTrackNumber() {
        return mTrackNumber;
    }

    public final int getSongDuration() {
        return mDuration;
    }

    @NonNull
    public final String getSongPath() {
        return mPath;
    }

    @NonNull
    public final String getAlbumName() {
        return mAlbumName;
    }

    public final int getArtistId() {
        return mArtistId;
    }

    @NonNull
    public final String getArtistName() {
        return mArtistName;
    }

    public final int getYear() {
        return mYear;
    }

    @NonNull
    public final Album getSongAlbum() {
        return mSongAlbum;
    }

    public void setSongAlbum(@NonNull final Album album) {
        mSongAlbum = album;
    }
}
