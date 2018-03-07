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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class ArtistsAdapter extends RecyclerView.Adapter<ArtistsAdapter.SimpleViewHolder> {

    private FragmentActivity mActivity;
    private List<Artist> mArtists;
    private artistSelectedListener mArtistSelectedListener;
    private HashMap<Integer, String> mIndexedPositions = new LinkedHashMap<>();

    public ArtistsAdapter(FragmentActivity activity, List<Artist> artists) {

        mActivity = activity;
        mArtists = artists;
        mArtistSelectedListener = (artistSelectedListener) activity;
        generateIndexes();
    }

    public String[] getIndexes() {
        return mIndexedPositions.values().toArray(new String[mIndexedPositions.values().size()]);
    }

    public int getIndexPosition(int currentSection) {
        return mIndexedPositions.keySet().toArray(new Integer[mIndexedPositions.keySet().size()])[currentSection];
    }

    private void generateIndexes() {

        for (int i = 0, size = mArtists.size(); i < size; i++) {
            String section = mArtists.get(i).getName().substring(0, 1).toUpperCase(Locale.getDefault());
            if (!mIndexedPositions.containsValue(section)) {
                mIndexedPositions.put(i, section);
            }
        }
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

    public interface artistSelectedListener {
        void onArtistSelected(String artist);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title;

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