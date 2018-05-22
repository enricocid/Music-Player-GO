package com.iven.musicplayergo.loaders;

import android.content.Context;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Artist;
import com.iven.musicplayergo.models.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ArtistProvider {

    public static final int ARTISTS_LOADER = 0;

    private static String getSongLoaderSortOrder() {
        return MediaStore.Audio.Artists.DEFAULT_SORT_ORDER + ", " + MediaStore.Audio.Albums.DEFAULT_SORT_ORDER + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
    }

    private static void sortArtists(List<Artist> artists) {
        Collections.sort(artists, new Comparator<Artist>() {
            public int compare(Artist obj1, Artist obj2) {
                return obj1.getName().compareToIgnoreCase(obj2.getName());
            }
        });
    }

    @NonNull
    private static List<Artist> getAllArtists(@NonNull final Context context) {
        List<Song> songs = SongProvider.getSongs(SongProvider.makeSongCursor(
                context, getSongLoaderSortOrder())
        );
        List<Artist> artists = retrieveArtists(AlbumProvider.retrieveAlbums(songs));
        sortArtists(artists);
        return artists;
    }

    public static Artist getArtist(List<Artist> artists, String selectedArtist) {
        Artist returnerArtist = null;
        for (Artist artist : artists) {
            if (artist.getName().equals(selectedArtist)) {
                returnerArtist = artist;
            }
        }
        return returnerArtist;
    }

    @NonNull
    private static List<Artist> retrieveArtists(@Nullable final List<Album> albums) {
        ArrayList<Artist> artists = new ArrayList<>();
        if (albums != null) {
            for (Album album : albums) {
                getOrCreateArtist(artists, album.getArtistId()).albums.add(album);
            }
        }
        return artists;
    }

    private static Artist getOrCreateArtist(List<Artist> artists, int artistId) {
        for (Artist artist : artists) {
            if (!artist.albums.isEmpty() && !artist.albums.get(0).songs.isEmpty() && artist.albums.get(0).songs.get(0).artistId == artistId) {
                return artist;
            }
        }
        Artist artist = new Artist();
        artists.add(artist);
        return artist;
    }

    public static class AsyncArtistLoader extends WrappedAsyncTaskLoader<List<Artist>> {

        public AsyncArtistLoader(Context context) {
            super(context);
        }

        @Override
        public List<Artist> loadInBackground() {
            return getAllArtists(getContext());
        }
    }
}
