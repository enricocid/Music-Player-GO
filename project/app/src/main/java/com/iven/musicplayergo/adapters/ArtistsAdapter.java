package com.iven.musicplayergo.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iven.musicplayergo.R;
import com.iven.musicplayergo.Utils;
import com.iven.musicplayergo.models.Artist;

import java.util.List;

public class ArtistsAdapter extends RecyclerView.Adapter<ArtistsAdapter.SimpleViewHolder> {

    private final Activity mActivity;
    private final List<Artist> mArtists;
    private final ArtistSelectedListener mArtistSelectedListener;

    public ArtistsAdapter(@NonNull Activity activity, List<Artist> artists) {
        mActivity = activity;
        mArtists = artists;
        mArtistSelectedListener = (ArtistSelectedListener) activity;
    }

    @Override
    @NonNull
    public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(mActivity)
                .inflate(R.layout.artist_item, parent, false);

        return new SimpleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position) {

        Artist artist = mArtists.get(holder.getAdapterPosition());
        holder.title.setText(artist.getName());

        Spanned spanned = Utils.buildSpanned(mActivity.getString(R.string.artist_info, artist.albums.size(), artist.getSongCount()));

        holder.albumCount.setText(spanned);
    }

    @Override
    public int getItemCount() {
        return mArtists.size();
    }

    public interface ArtistSelectedListener {
        void onArtistSelected(String artist);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView title, albumCount;

        SimpleViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.artist);
            albumCount = itemView.findViewById(R.id.album_count);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mArtistSelectedListener.onArtistSelected(mArtists.get(getAdapterPosition()).getName());
        }
    }
}