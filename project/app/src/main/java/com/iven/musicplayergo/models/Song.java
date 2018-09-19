package com.iven.musicplayergo.models;

import android.support.annotation.NonNull;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Song {

    static final Song EMPTY_SONG = new Song("", -1, -1, -1, null, "", -1, "");

    public final String title;
    public final int trackNumber;
    public final int duration;
    public final String path;
    public final String albumName;
    public final int artistId;
    public final String artistName;
    final int year;
    private Album mSongAlbum;

    public Song(@NonNull final String title, final int trackNumber, final int year, final int duration, final String path, final String albumName, final int artistId, final String artistName) {
        this.title = title;
        this.trackNumber = trackNumber;
        this.year = year;
        this.duration = duration;
        this.path = path;
        this.albumName = albumName;
        this.artistId = artistId;
        this.artistName = artistName;
    }

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

    public Album getSongAlbum() {
        return mSongAlbum;
    }

    public void setSongAlbum(@NonNull final Album album) {
        mSongAlbum = album;
    }
}
