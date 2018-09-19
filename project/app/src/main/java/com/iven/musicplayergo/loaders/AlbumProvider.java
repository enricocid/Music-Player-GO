package com.iven.musicplayergo.loaders;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class AlbumProvider {

    @NonNull
    static List<Album> retrieveAlbums(@Nullable final List<Song> songs) {
        final List<Album> albums = new ArrayList<>();
        if (songs != null) {
            for (Song song : songs) {
                Album album = getAlbum(albums, song.albumName);
                album.songs.add(song);
                song.setSongAlbum(album);
            }
        }
        if (albums.size() > 1) {
            sortAlbums(albums);
        }
        return albums;
    }

    private static void sortAlbums(@NonNull final List<Album> albums) {
        Collections.sort(albums, (obj1, obj2) -> Integer.compare(obj1.getYear(), obj2.getYear()));
    }

    private static Album getAlbum(@NonNull final List<Album> albums, final String albumName) {
        for (Album album : albums) {
            if (!album.songs.isEmpty() && album.songs.get(0).albumName.equals(albumName)) {
                return album;
            }
        }
        final Album album = new Album();
        albums.add(album);
        return album;
    }
}
