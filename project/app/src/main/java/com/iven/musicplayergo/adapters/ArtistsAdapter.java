package com.iven.musicplayergo.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ArtistsAdapter extends RecyclerView.Adapter<ArtistsAdapter.SimpleViewHolder> {

    private final Context mContext;
    private final ArtistSelectedListener mArtistSelectedListener;
    private final HashMap<Integer, String> mIndexedPositions = new LinkedHashMap<>();
    private List<Artist> mArtists;

    public ArtistsAdapter(@NonNull final Context context, @NonNull final List<Artist> artists) {
        mContext = context;
        mArtists = artists;
        mArtistSelectedListener = (ArtistSelectedListener) mContext;
        generateIndexes();
    }

    public void setArtists(List<Artist> artists) {
        mArtists = artists;
        notifyDataSetChanged();
    }

    public String[] getIndexes() {
        int size = mIndexedPositions.values().size();
        return mIndexedPositions.values().toArray(new String[size]);
    }

    public int getIndexPosition(final int currentSection) {
        final int size = mIndexedPositions.keySet().size();
        return mIndexedPositions.keySet().toArray(new Integer[size])[currentSection];
    }

    private void generateIndexes() {
        try {
            for (int i = 0, size = mArtists.size(); i < size; i++) {
                String section = mArtists.get(i).getName().substring(0, 1).toUpperCase(Locale.getDefault());
                if (!mIndexedPositions.containsValue(section)) {
                    mIndexedPositions.put(i, section);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @NonNull
    public SimpleViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {

        final View itemView = LayoutInflater.from(mContext)
                .inflate(R.layout.artist_item, parent, false);

        return new SimpleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final SimpleViewHolder holder, final int position) {

        Artist artist = mArtists.get(holder.getAdapterPosition());
        holder.title.setText(artist.getName());

        final Spanned spanned = Utils.buildSpanned(mContext.getString(R.string.artist_info, artist.getAlbums().size(), artist.getSongCount()));

        holder.albumCount.setText(spanned);
    }

    @Override
    public int getItemCount() {
        return mArtists.size();
    }

    public interface ArtistSelectedListener {
        void onArtistSelected(@NonNull final String artist, final boolean showPlayedArtist);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {

        final TextView title, albumCount;
        private boolean sArtistLongPressed = false;

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

        @Override
        public boolean onLongClick(@NonNull final View v) {
            if (!sArtistLongPressed) {
                title.setSelected(true);
                sArtistLongPressed = true;
            }
            return true;
        }

        @Override
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(@NonNull final View v, @NonNull final MotionEvent event) {
            if (sArtistLongPressed && event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_OUTSIDE || event.getAction() == MotionEvent.ACTION_MOVE) {
                title.setSelected(false);
                sArtistLongPressed = false;
            }
            return false;
        }
    }
}