package com.iven.musicplayergo.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iven.musicplayergo.R;
import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Artist;
import com.iven.musicplayergo.playback.PlayerAdapter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.SimpleViewHolder> {

    private Activity mActivity;

    private TextView mDisc, mDiscs;

    private Album mSelectedAlbum;

    private Pair<Artist, List<Album>> mAlbumsForArtist;

    private List<Album> mAlbums;

    private albumSelectedListener mAlbumSelectedListener;

    private PlayerAdapter mPlayerAdapter;

    public AlbumsAdapter(Activity activity, Pair<Artist, List<Album>> albumsForArtist, PlayerAdapter playerAdapter) {

        mActivity = activity;

        mAlbumSelectedListener = (albumSelectedListener) mActivity;

        mAlbumsForArtist = albumsForArtist;

        mPlayerAdapter = playerAdapter;

        mAlbums = mAlbumsForArtist.second;

        mDisc = mActivity.findViewById(R.id.disc);

        mDiscs = mActivity.findViewById(R.id.discs);

        updateAlbumsForArtist();
    }

    public void swapArtist(Pair<Artist, List<Album>> albumsForArtist) {
        mAlbumsForArtist = albumsForArtist;
        mAlbums = mAlbumsForArtist.second;
        updateAlbumsForArtist();
        notifyDataSetChanged();
    }

    private void updateAlbumsForArtist() {

        sortAlbums();

        Artist artist = mAlbumsForArtist.first;

        mSelectedAlbum = mPlayerAdapter != null && mPlayerAdapter.getSelectedAlbum(mPlayerAdapter.isPlaying()) != null ? mPlayerAdapter.getSelectedAlbum(mPlayerAdapter.isPlaying()) : artist.getFirstAlbum();

        mDiscs.setText(mActivity.getString(R.string.albums, artist.getName(), getItemCount()));

        mDisc.setText(mActivity.getString(R.string.album, mSelectedAlbum.getTitle(), getYear(mSelectedAlbum.getYear())));

        mAlbumSelectedListener.onAlbumSelected(mSelectedAlbum);
    }

    private void sortAlbums() {
        if (getItemCount() > 1) {
            Collections.sort(mAlbums, new Comparator<Album>() {
                public int compare(Album obj1, Album obj2) {
                    return Integer.compare(obj1.getYear(), obj2.getYear());
                }
            });
        }
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
        String albumTitle = album.getTitle();
        holder.title.setText(albumTitle);
        holder.number.setText(String.valueOf(album.getSongCount()));

        holder.year.setText(getYear(album.getYear()));
    }

    @Override
    public int getItemCount() {

        return mAlbums.size();
    }

    public interface albumSelectedListener {
        void onAlbumSelected(Album album);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, number, year;

        SimpleViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.album);
            number = itemView.findViewById(R.id.count);
            year = itemView.findViewById(R.id.year);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            //update songs list only if the album is updated
            if (mAlbums.get(getAdapterPosition()) != mSelectedAlbum) {
                mSelectedAlbum = mAlbums.get(getAdapterPosition());
                mPlayerAdapter.setSelectedAlbum(mSelectedAlbum);
                mDisc.setText(mActivity.getString(R.string.album, mSelectedAlbum.getTitle(), getYear(mSelectedAlbum.getYear())));
                mAlbumSelectedListener.onAlbumSelected(mSelectedAlbum);
            }
        }
    }
}