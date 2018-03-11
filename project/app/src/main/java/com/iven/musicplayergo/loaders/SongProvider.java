package com.iven.musicplayergo.loaders;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.iven.musicplayergo.models.Song;

import java.util.ArrayList;
import java.util.List;

class SongProvider {

    private static final int TITLE = 0;
    private static final int TRACK = 1;
    private static final int YEAR = 2;
    private static final int DURATION = 3;
    private static final int PATH = 4;
    private static final int ALBUM = 5;
    private static final int ARTIST_ID = 6;
    private static final int ARTIST = 7;

    private static final String BASE_SELECTION = AudioColumns.IS_MUSIC + "=1" + " AND " + AudioColumns.TITLE + " != ''";
    private static final String[] BASE_PROJECTION = new String[]{
            AudioColumns.TITLE,// 0
            AudioColumns.TRACK,// 1
            AudioColumns.YEAR,// 2
            AudioColumns.DURATION,// 3
            AudioColumns.DATA,// 4
            AudioColumns.ALBUM,// 5
            AudioColumns.ARTIST_ID,// 6
            AudioColumns.ARTIST,// 7
    };

    @NonNull
    static List<Song> getSongs(@Nullable final Cursor cursor) {
        List<Song> songs = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                if (getSongFromCursorImpl(cursor).duration >= 5000) {
                    songs.add(getSongFromCursorImpl(cursor));
                }
            } while (cursor.moveToNext());
        }

        if (cursor != null)
            cursor.close();
        return songs;
    }

    @NonNull
    private static Song getSongFromCursorImpl(@NonNull Cursor cursor) {
        final String title = cursor.getString(TITLE);
        final int trackNumber = cursor.getInt(TRACK);
        final int year = cursor.getInt(YEAR);
        final int duration = cursor.getInt(DURATION);
        final String uri = cursor.getString(PATH);
        final String albumName = cursor.getString(ALBUM);
        final int artistId = cursor.getInt(ARTIST_ID);
        final String artistName = cursor.getString(ARTIST);

        return new Song(title, trackNumber, year, duration, uri, albumName, artistId, artistName);
    }

    @Nullable
    static Cursor makeSongCursor(@NonNull final Context context, @Nullable String selection, String[] selectionValues, final String sortOrder) {
        if (selection != null && !selection.trim().equals("")) {
            selection = BASE_SELECTION + " AND " + selection;
        } else {
            selection = BASE_SELECTION;
        }

        try {
            return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    BASE_PROJECTION, selection, selectionValues, sortOrder);
        } catch (SecurityException e) {
            return null;
        }
    }
}
