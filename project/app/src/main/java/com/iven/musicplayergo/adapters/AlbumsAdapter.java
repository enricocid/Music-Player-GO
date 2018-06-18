package com.iven.musicplayergo.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iven.musicplayergo.R;
import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.playback.PlayerAdapter;

import java.util.List;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.SimpleViewHolder> {

    private final Activity mActivity;
    private final AlbumSelectedListener mAlbumSelectedListener;
    private final PlayerAdapter mPlayerAdapter;
    private final int mAccent;
    private List<Album> mAlbums;
    private Album mSelectedAlbum;

    public AlbumsAdapter(@NonNull final Activity activity, @NonNull final List<Album> albums, @NonNull final PlayerAdapter playerAdapter, final int accent) {
        mActivity = activity;
        mAlbums = albums;
        mPlayerAdapter = playerAdapter;
        mAccent = accent;
        mAlbumSelectedListener = (AlbumSelectedListener) mActivity;
        updateAlbumsForArtist();
    }

    public void swapArtist(List<Album> albums) {
        mAlbums = albums;
        notifyDataSetChanged();
        updateAlbumsForArtist();
    }

    private void updateAlbumsForArtist() {
        mSelectedAlbum = mPlayerAdapter.getSelectedAlbum() != null ? mPlayerAdapter.getSelectedAlbum() : mAlbums.get(0);
        mAlbumSelectedListener.onAlbumSelected(mSelectedAlbum);
    }

    @Override
    @NonNull
    public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        final View itemView = LayoutInflater.from(mActivity)
                .inflate(R.layout.album_item, parent, false);

        return new SimpleViewHolder(itemView);
    }

    private String getYear(final int year) {
        return year != 0 && year != -1 ? String.valueOf(year) : mActivity.getString(R.string.unknown_year);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position) {

        final Album album = mAlbums.get(holder.getAdapterPosition());
        final String albumTitle = album.getTitle();
        holder.title.setText(albumTitle);
        holder.year.setText(getYear(album.getYear()));
    }

    @Override
    public int getItemCount() {
        return mAlbums.size();
    }

    public interface AlbumSelectedListener {
        void onAlbumSelected(@NonNull final Album album);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView title, year;

        SimpleViewHolder(@NonNull final View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.album);
            year = itemView.findViewById(R.id.year);
            final CardView cardContainer = (CardView) itemView;
            cardContainer.setCardBackgroundColor(ColorUtils.setAlphaComponent(mAccent, 15));
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            //update songs list only if the album is updated
            if (mAlbums.get(getAdapterPosition()) != mSelectedAlbum) {
                mSelectedAlbum = mAlbums.get(getAdapterPosition());
                mAlbumSelectedListener.onAlbumSelected(mSelectedAlbum);
            }
        }
    }
}