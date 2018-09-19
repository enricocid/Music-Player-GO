package com.iven.musicplayergo.adapters;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.iven.musicplayergo.R;
import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Song;
import com.iven.musicplayergo.playback.PlayingInfoProvider;

import java.util.List;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.SimpleViewHolder> {

    private final SongSelectedListener mSongSelectedListener;
    private final Activity mActivity;
    private List<Song> mSongs;
    private Album mAlbum;

    public SongsAdapter(@NonNull final Activity activity, @NonNull final Album album) {
        mActivity = activity;
        mAlbum = album;
        mSongs = mAlbum.songs;
        mSongSelectedListener = (SongSelectedListener) activity;
    }

    public void swapSongs(@NonNull final Album album) {
        mAlbum = album;
        mSongs = mAlbum.songs;
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public SimpleViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {

        final View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_item, parent, false);

        return new SimpleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final SimpleViewHolder holder, final int position) {

        final Song song = mSongs.get(holder.getAdapterPosition());
        final String songTitle = song.title;

        final int songTrack = Song.formatTrack(song.trackNumber);

        holder.track.setText(String.valueOf(songTrack));
        holder.title.setText(songTitle);
        holder.duration.setText(Song.formatDuration(song.duration));
        final int randomColor = ContextCompat.getColor(mActivity, ColorsAdapter.getRandomColor());
        holder.foreground.setColorFilter(randomColor, PorterDuff.Mode.SRC_IN);
        holder.duration.setTextColor(randomColor);
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }

    public interface SongSelectedListener {
        void onSongSelected(@NonNull final Song song, @NonNull final List<Song> songs);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final ImageView foreground;
        final TextView track, title, duration;

        SimpleViewHolder(@NonNull final View itemView) {
            super(itemView);

            foreground = itemView.findViewById(R.id.foreground);
            track = itemView.findViewById(R.id.track);
            title = itemView.findViewById(R.id.title);
            duration = itemView.findViewById(R.id.duration);
            itemView.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            final Song song = mSongs.get(getAdapterPosition());
            mSongSelectedListener.onSongSelected(song, mSongs);
            PlayingInfoProvider.setPlayedAlbum(mAlbum);
        }
    }
}