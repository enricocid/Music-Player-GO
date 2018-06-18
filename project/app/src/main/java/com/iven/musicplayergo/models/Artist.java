package com.iven.musicplayergo.models;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Artist {

    public final List<Album> albums;

    public Artist() {
        this.albums = new ArrayList<>();
    }

    public final int getId() {
        return getFirstAlbum().getArtistId();
    }

    public final String getName() {
        return getFirstAlbum().getArtistName();
    }

    @NonNull
    private Album getFirstAlbum() {
        return albums.isEmpty() ? new Album() : albums.get(0);
    }

    public final int getSongCount() {
        int songCount = 0;
        for (Album album : albums) {
            songCount += album.getSongCount();
        }
        return songCount;
    }
}
