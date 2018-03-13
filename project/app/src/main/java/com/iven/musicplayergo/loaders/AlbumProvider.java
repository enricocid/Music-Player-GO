package com.iven.musicplayergo.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Artist;
import com.iven.musicplayergo.models.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AlbumProvider {

    public static final int ALBUMS_LOADER = 1;

    @NonNull
    static List<Album> retrieveAlbums(@Nullable final List<Song> songs) {
        List<Album> albums = new ArrayList<>();
        if (songs != null) {
            for (Song song : songs) {
                getAlbum(albums, song.albumName).songs.add(song);
            }
        }
        if (albums.size() > 1) {
            sortAlbums(albums);
        }

        for (int i = 0, size = albums.size(); i < size; i++) {
            albums.get(i).setPosition(i);
        }
        return albums;
    }

    private static void sortAlbums(List<Album> albums) {
        Collections.sort(albums, new Comparator<Album>() {
            public int compare(Album obj1, Album obj2) {
                return Integer.compare(obj1.getYear(), obj2.getYear());
            }
        });
    }

    private static Album getAlbum(List<Album> albums, String albumName) {
        for (Album album : albums) {
            if (!album.songs.isEmpty() && album.songs.get(0).albumName.equals(albumName)) {
                return album;
            }
        }
        Album album = new Album();
        albums.add(album);
        return album;
    }

    public static class AsyncAlbumsForArtistLoader extends WrappedAsyncTaskLoader<Pair<Artist, List<Album>>> {

        private String mSelectedArtist;

        public AsyncAlbumsForArtistLoader(Context context, String selectedArtist) {
            super(context);
            mSelectedArtist = selectedArtist;
        }

        @Override
        public Pair<Artist, List<Album>> loadInBackground() {
            Artist artist = ArtistProvider.getArtist(getContext(), mSelectedArtist);
            List<Album> albums = artist.albums;
            return new Pair<>(artist, albums);
        }
    }
}
