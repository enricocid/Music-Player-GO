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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class ArtistsAdapter extends RecyclerView.Adapter<ArtistsAdapter.SimpleViewHolder> {

    private final Activity mActivity;
    private final List<Artist> mArtists;
    private final ArtistSelectedListener mArtistSelectedListener;
    private final HashMap<Integer, String> mIndexedPositions = new LinkedHashMap<>();

    public ArtistsAdapter(@NonNull final Activity activity, @NonNull final List<Artist> artists) {
        mActivity = activity;
        mArtists = artists;
        mArtistSelectedListener = (ArtistSelectedListener) activity;
        generateIndexes();
    }

    public String[] getIndexes() {
        return mIndexedPositions.values().toArray(new String[mIndexedPositions.values().size()]);
    }

    public int getIndexPosition(final int currentSection) {
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
    public SimpleViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {

        View itemView = LayoutInflater.from(mActivity)
                .inflate(R.layout.artist_item, parent, false);

        return new SimpleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final SimpleViewHolder holder, final int position) {

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
        void onArtistSelected(@NonNull final String artist, final boolean showPlayedArtist);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView title, albumCount;

        SimpleViewHolder(@NonNull final View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.artist);
            albumCount = itemView.findViewById(R.id.album_count);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(@NonNull final View v) {
            mArtistSelectedListener.onArtistSelected(mArtists.get(getAdapterPosition()).getName(), false);
        }
    }
}