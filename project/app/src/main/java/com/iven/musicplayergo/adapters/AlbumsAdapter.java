package com.iven.musicplayergo.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.iven.musicplayergo.R;
import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.playback.PlayerAdapter;

import java.util.List;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.SimpleViewHolder> {

    private final Activity mActivity;
    private final AlbumSelectedListener mAlbumSelectedListener;
    private final RecyclerView mAlbumsRecyclerView;
    private List<Album> mAlbums;
    private Album mSelectedAlbum;
    private int mSelectedPosition;
    private PlayerAdapter mPlayerAdapter;

    public AlbumsAdapter(Activity activity, RecyclerView albumsRecyclerView, List<Album> albums, PlayerAdapter playerAdapter) {

        mActivity = activity;

        mAlbums = albums;

        mPlayerAdapter = playerAdapter;

        mAlbumsRecyclerView = albumsRecyclerView;

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
        mAlbumsRecyclerView.smoothScrollToPosition(mSelectedPosition);
        mAlbumSelectedListener.onAlbumSelected(mSelectedAlbum);
    }

    @Override
    @NonNull
    public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(mActivity)
                .inflate(R.layout.album_item, parent, false);

        return new SimpleViewHolder(itemView);
    }

    private String getYear(int year) {
        return year != 0 ? String.valueOf(year) : mActivity.getString(R.string.unknown_year);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position) {

        Album album = mAlbums.get(holder.getAdapterPosition());

        int visibility = !mSelectedAlbum.getTitle().equals(album.getTitle()) ? View.GONE : View.VISIBLE;
        holder.nowPlaying.setVisibility(visibility);

        String albumTitle = album.getTitle();
        holder.title.setText(albumTitle);
        holder.year.setText(getYear(album.getYear()));
    }

    @Override
    public int getItemCount() {

        return mAlbums.size();
    }

    public interface AlbumSelectedListener {
        void onAlbumSelected(Album album);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView title, year;
        final ImageView nowPlaying;

        SimpleViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.album);
            year = itemView.findViewById(R.id.year);
            nowPlaying = itemView.findViewById(R.id.nowPlaying);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            //update songs list only if the album is updated
            if (getAdapterPosition() != mSelectedPosition) {

                notifyItemChanged(mSelectedPosition);
                mSelectedPosition = getAdapterPosition();

                mSelectedAlbum = mAlbums.get(getAdapterPosition());
                nowPlaying.setVisibility(View.VISIBLE);
                mAlbumSelectedListener.onAlbumSelected(mSelectedAlbum);
            }
        }
    }
}