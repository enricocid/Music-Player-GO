package com.iven.musicplayergo.models;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class Artist {

    private final List<Album> mAlbums;

    public Artist() {
        mAlbums = new ArrayList<>();
    }

    @NonNull
    public final List<Album> getAlbums() {
        return mAlbums;
    }

    @NonNull
    public final String getName() {
        return getFirstAlbum().getArtistName();
    }

    @NonNull
    private Album getFirstAlbum() {
        return mAlbums.isEmpty() ? new Album() : mAlbums.get(0);
    }

    public final int getSongCount() {
        int songCount = 0;
        for (Album album : mAlbums) {
            songCount += album.getSongCount();
        }
        return songCount;
    }
}
