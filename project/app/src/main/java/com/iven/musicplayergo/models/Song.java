package com.iven.musicplayergo.models;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Song {

    static final Song EMPTY_SONG = new Song("", -1, -1, -1, null, "", -1, "");

    public final String title;
    public final int trackNumber;
    public final int duration;
    public final String path;
    public final String albumName;
    public final int artistId;
    public final String artistName;
    final int year;

    public Song(String title, int trackNumber, int year, int duration, String path, String albumName, int artistId, String artistName) {
        this.title = title;
        this.trackNumber = trackNumber;
        this.year = year;
        this.duration = duration;
        this.path = path;
        this.albumName = albumName;
        this.artistId = artistId;
        this.artistName = artistName;
    }

    public static String formatDuration(int duration) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
    }

    public static int formatTrack(int trackNumber) {

        int formatted = trackNumber;
        if (trackNumber >= 1000) {
            formatted = trackNumber % 1000;
        }
        return formatted;
    }
}
