package com.iven.musicplayergo.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.iven.musicplayergo.R;
import com.iven.musicplayergo.Utils;
import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.playback.PlayerAdapter;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.SimpleViewHolder> {

    private final Context mContext;
    private final PlayerAdapter mPlayerAdapter;
    private final AlbumSelectedListener mAlbumSelectedListener;
    private final List<Album> mAlbums;
    private final int mAccent;
    private final boolean sThemeInverted;
    private Album mSelectedAlbum;
    private int mSelectedPosition;

    public AlbumsAdapter(@NonNull final Context context, @NonNull final PlayerAdapter playerAdapter, @NonNull final List<Album> albums, final boolean showPlayedArtist, final int accent, final boolean isThemeDark) {
        mContext = context;
        mPlayerAdapter = playerAdapter;
        mAlbums = albums;
        mAccent = accent;
        mAlbumSelectedListener = (AlbumSelectedListener) mContext;
        mSelectedAlbum = showPlayedArtist ? mPlayerAdapter.getCurrentSong().getSongAlbum() : mPlayerAdapter.getNavigationAlbum() != null ? mPlayerAdapter.getNavigationAlbum() : mAlbums.get(0);
        mSelectedPosition = mSelectedAlbum.getAlbumPosition();
        sThemeInverted = isThemeDark;
        mAlbumSelectedListener.onAlbumSelected(mSelectedAlbum);
    }

    @Override
    @NonNull
    public SimpleViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {

        final View itemView = LayoutInflater.from(mContext)
                .inflate(R.layout.album_item, parent, false);

        return new SimpleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final SimpleViewHolder holder, final int position) {

        final Album album = mAlbums.get(holder.getAdapterPosition());
        final String albumTitle = album.getTitle();
        holder.title.setText(albumTitle);
        holder.year.setText(Album.getYearForAlbum(mContext, album.getYear()));
        holder.container.setCardBackgroundColor(ColorUtils.setAlphaComponent(mAccent, 25));
        if (!mSelectedAlbum.getTitle().equals(album.getTitle())) {
            Utils.setCardStroke(mContext, holder.container, Color.TRANSPARENT);
        } else {
            Utils.setCardStroke(mContext, holder.container, sThemeInverted? Color.WHITE : Color.BLACK);
            mSelectedPosition = holder.getAdapterPosition();
        }
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
        final MaterialCardView container;

        SimpleViewHolder(@NonNull final View itemView) {
            super(itemView);
            container = (MaterialCardView) itemView;
            title = itemView.findViewById(R.id.album);
            year = itemView.findViewById(R.id.year);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(@NonNull final View v) {

            //update songs list only if the album is updated
            if (getAdapterPosition() != mSelectedPosition) {
                notifyItemChanged(mSelectedPosition);
                mSelectedPosition = getAdapterPosition();
                Utils.setCardStroke(mContext, container, sThemeInverted? Color.WHITE : Color.BLACK);
                mSelectedAlbum = mAlbums.get(getAdapterPosition());
                mPlayerAdapter.setNavigationAlbum(mSelectedAlbum);
                mAlbumSelectedListener.onAlbumSelected(mSelectedAlbum);
            }
        }
    }
}