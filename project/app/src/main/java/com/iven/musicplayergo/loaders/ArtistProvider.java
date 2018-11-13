package com.iven.musicplayergo.loaders;

import android.content.Context;
import android.provider.MediaStore;

import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Artist;
import com.iven.musicplayergo.models.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ArtistProvider {

    private static String getSongLoaderSortOrder() {
        return MediaStore.Audio.Artists.DEFAULT_SORT_ORDER + ", " + MediaStore.Audio.Albums.DEFAULT_SORT_ORDER + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
    }

    private static void sortArtists(@NonNull final List<Artist> artists) {
        Collections.sort(artists, (obj1, obj2) -> obj1.getName().compareToIgnoreCase(obj2.getName()));
    }

    @NonNull
    static List<Artist> getAllArtists(@NonNull final Context context) {
        final List<Song> songs = SongProvider.getSongs(SongProvider.makeSongCursor(
                context, getSongLoaderSortOrder())
        );
        final List<Artist> artists = retrieveArtists(AlbumProvider.retrieveAlbums(songs));
        sortArtists(artists);
        return artists;
    }

    public static Artist getArtist(@NonNull final List<Artist> artists, @NonNull final String selectedArtist) {
        Artist returnerArtist = null;
        try {
            for (Artist artist : artists) {
                if (artist.getName().equals(selectedArtist)) {
                    returnerArtist = artist;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnerArtist;
    }

    @NonNull
    private static List<Artist> retrieveArtists(@Nullable final List<Album> albums) {
        final List<Artist> artists = new ArrayList<>();
        try {
            if (albums != null) {
                for (Album album : albums) {
                    getOrCreateArtist(artists, album.getArtistId()).getAlbums().add(album);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return artists;
    }

    private static Artist getOrCreateArtist(@NonNull final List<Artist> artists, final int artistId) {
        for (Artist artist : artists) {
            if (!artist.getAlbums().isEmpty() && !artist.getAlbums().get(0).getSongs().isEmpty() && artist.getAlbums().get(0).getSongs().get(0).getArtistId() == artistId) {
                return artist;
            }
        }
        final Artist artist = new Artist();
        artists.add(artist);
        return artist;
    }
}
