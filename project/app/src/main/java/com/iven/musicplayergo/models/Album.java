package com.iven.musicplayergo.models;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Album {

    public final List<Song> songs;

    public Album() {
        this.songs = new ArrayList<>();
    }

    public String getTitle() {
        return getFirstSong().albumName;
    }

    public final int getArtistId() {
        return getFirstSong().artistId;
    }

    public final String getArtistName() {
        return getFirstSong().artistName;
    }

    public final int getYear() {
        return getFirstSong().year;
    }

    public final int getSongCount() {
        return songs.size();
    }

    @NonNull
    private Song getFirstSong() {
        return songs.isEmpty() ? Song.EMPTY_SONG : songs.get(0);
    }
}
