package com.iven.musicplayergo.playback;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Song;

import java.util.List;

public class PlayingInfoProvider {

    private static Song mPlayedSong;
    private static List<Song> mSongsForPlayedArtist;
    private static String mNavigationArtist;
    private static Album mPlayedAlbum, mNavigationAlbum;

    public static Song getPlayedSong() {
        return mPlayedSong;
    }

    public static List<Song> getSongsForPlayedArtist() {
        return mSongsForPlayedArtist;
    }

    public static Album getPlayedAlbum() {
        return mPlayedAlbum;
    }

    public static void setPlayedAlbum(@NonNull final Album album) {
        mPlayedAlbum = album;
    }

    public static String getNavigationArtist() {
        return mNavigationArtist;
    }

    public static void setNavigationArtist(@NonNull final String navigationArtist) {
        mNavigationArtist = navigationArtist;
    }

    public static Album getNavigationAlbum() {
        return mNavigationAlbum;
    }

    public static void setNavigationAlbum(@Nullable final Album navigationAlbum) {
        mNavigationAlbum = navigationAlbum;
    }

    public static void setPlayedSong(@NonNull final Song playedSong, @NonNull final List<Song> songsForPlayedArtist) {
        mPlayedSong = playedSong;
        mSongsForPlayedArtist = songsForPlayedArtist;
    }
}
