package com.iven.musicplayergo.models;

import android.support.annotation.NonNull;

import java.util.ArrayList;

public class Album {

    public final ArrayList<Song> songs;

    public Album() {
        this.songs = new ArrayList<>();
    }

    public String getTitle() {
        return getFirstSong().albumName;
    }

    public int getArtistId() {
        return getFirstSong().artistId;
    }

    String getArtistName() {
        return getFirstSong().artistName;
    }

    public int getYear() {
        return getFirstSong().year;
    }

    @NonNull
    private Song getFirstSong() {
        return songs.isEmpty() ? Song.EMPTY_SONG : songs.get(0);
    }
}
