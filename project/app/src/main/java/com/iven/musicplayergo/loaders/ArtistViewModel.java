package com.iven.musicplayergo.loaders;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.support.annotation.NonNull;

import com.iven.musicplayergo.models.Artist;

import java.util.List;

public class ArtistViewModel extends ViewModel {
    private MutableLiveData<List<Artist>> artists;

    public LiveData<List<Artist>> getArtists(@NonNull final Context context) {
        if (artists == null) {
            artists = new MutableLiveData<>();
            loadUsers(context);
        }
        return artists;
    }

    private void loadUsers(@NonNull Context context) {
        artists.setValue(ArtistProvider.getAllArtists(context));
    }
}