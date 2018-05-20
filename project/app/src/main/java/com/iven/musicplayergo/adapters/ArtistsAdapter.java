package com.iven.musicplayergo.adapters;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iven.musicplayergo.R;
import com.iven.musicplayergo.models.Artist;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.List;

public class ArtistsAdapter extends RecyclerView.Adapter<ArtistsAdapter.SimpleViewHolder> implements FastScrollRecyclerView.SectionedAdapter {

    private final FragmentActivity mActivity;
    private final List<Artist> mArtists;
    private final ArtistSelectedListener mArtistSelectedListener;

    public ArtistsAdapter(FragmentActivity activity, List<Artist> artists) {
        mActivity = activity;
        mArtists = artists;
        mArtistSelectedListener = (ArtistSelectedListener) activity;
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        return mArtists.get(position).getName().substring(0, 1);
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

        String artist = mArtists.get(holder.getAdapterPosition()).getName();
        holder.title.setText(artist);
    }

    @Override
    public int getItemCount() {

        return mArtists.size();
    }

    public interface ArtistSelectedListener {
        void onArtistSelected(String artist);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView title;

        SimpleViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.artist);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mArtistSelectedListener.onArtistSelected(mArtists.get(getAdapterPosition()).getName());
        }
    }
}