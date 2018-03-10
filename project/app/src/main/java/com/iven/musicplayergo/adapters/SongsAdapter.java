package com.iven.musicplayergo.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iven.musicplayergo.R;
import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Song;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.SimpleViewHolder> {

    private List<Song> mSongs;
    private Album mAlbum;
    private songSelectedListener mSongSelectedListener;

    public SongsAdapter(Activity activity, Album album) {

        mAlbum = album;
        mSongs = mAlbum.songs;

        sortSongsByTrack();
        mSongSelectedListener = (songSelectedListener) activity;
    }

    public void swapSongs(Album album) {
        mAlbum = album;
        mSongs = mAlbum.songs;
        sortSongsByTrack();
        notifyDataSetChanged();
    }

    private void sortSongsByTrack() {
        if (getItemCount() > 1) {
            Collections.sort(mSongs, new Comparator<Song>() {
                public int compare(Song obj1, Song obj2) {
                    return Long.compare(obj1.trackNumber, obj2.trackNumber);
                }
            });
        }
    }

    @Override
    @NonNull
    public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_item, parent, false);

        return new SimpleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position) {

        Song song = mSongs.get(holder.getAdapterPosition());
        String songTitle = song.title;
        holder.title.setText(songTitle);
        holder.number.setText(Song.formatTrack(song.trackNumber));
        holder.duration.setText(Song.formatDuration(song.duration));
    }

    @Override
    public int getItemCount() {

        return mSongs.size();
    }

    public interface songSelectedListener {
        void onSongSelected(Song song, Album album);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView number, title, duration;

        SimpleViewHolder(View itemView) {
            super(itemView);

            number = itemView.findViewById(R.id.track);
            title = itemView.findViewById(R.id.title);
            duration = itemView.findViewById(R.id.duration);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Song song = mSongs.get(getAdapterPosition());
            mSongSelectedListener.onSongSelected(song, mAlbum);
        }
    }
}