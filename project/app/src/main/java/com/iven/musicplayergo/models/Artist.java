package com.iven.musicplayergo.models;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Artist {

    public final List<Album> albums;

    public Artist(List<Album> albums) {
        this.albums = albums;
    }

    public Artist() {
        this.albums = new ArrayList<>();
    }

    public int getId() {
        return getFirstAlbum().getArtistId();
    }

    public String getName() {
        return getFirstAlbum().getArtistName();
    }

    @NonNull
    private Album getFirstAlbum() {
        return albums.isEmpty() ? new Album() : albums.get(0);
    }
}
