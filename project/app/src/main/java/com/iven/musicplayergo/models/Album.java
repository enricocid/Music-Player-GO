package com.iven.musicplayergo.models;

import android.content.Context;
import android.support.annotation.NonNull;

import com.iven.musicplayergo.R;

import java.util.ArrayList;
import java.util.List;

public class Album {

    private final List<Song> mSongs;

    public Album() {
        mSongs = new ArrayList<>();
    }

    public static String getYearForAlbum(@NonNull final Context context, final int year) {
        return year != 0 && year != -1 ? String.valueOf(year) : context.getString(R.string.unknown_year);
    }

    public final List<Song> getSongs() {
        return mSongs;
    }

    public final String getTitle() {
        return getFirstSong().getAlbumName();
    }

    public final int getArtistId() {
        return getFirstSong().getArtistId();
    }

    final String getArtistName() {
        return getFirstSong().getArtistName();
    }

    public final int getYear() {
        return getFirstSong().getYear();
    }

    final int getSongCount() {
        return mSongs.size();
    }

    @NonNull
    private Song getFirstSong() {
        return mSongs.isEmpty() ? Song.EMPTY_SONG : mSongs.get(0);
    }
}
