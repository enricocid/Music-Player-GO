package com.iven.musicplayergo.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.iven.musicplayergo.R;

public class ColorsAdapter extends RecyclerView.Adapter<ColorsAdapter.SimpleViewHolder> {

    private final Activity mActivity;
    private final int mAccent;
    private final AccentChangedListener mOnAccentChangedListener;

    //fixed int array of accent colors
    private final int[] colors = new int[]{
            R.color.red_A400,
            R.color.pink_A400,
            R.color.purple_A400,
            R.color.deep_purple_A400,
            R.color.indigo_A400,
            R.color.blue_A400,
            R.color.light_blue_A400,
            R.color.cyan_A400,
            R.color.teal_A400,
            R.color.green_A400,
            R.color.amber_A400,
            R.color.orange_A400,
            R.color.deep_orange_A400,
            R.color.brown_400,
            R.color.gray_400,
            R.color.gray_900,
            R.color.blue_gray_400
    };

    public ColorsAdapter(@NonNull Activity activity, int accent) {
        mActivity = activity;
        mAccent = accent;
        mOnAccentChangedListener = (AccentChangedListener) mActivity;
    }

    @Override
    @NonNull
    public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.color_option, parent, false);
        return new SimpleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position) {

        int color = colors[holder.getAdapterPosition()];

        int drawable = color != mAccent ? R.drawable.ic_checkbox_blank : R.drawable.ic_checkbox_marked;

        int parsedColor = ContextCompat.getColor(mActivity, color);
        holder.color.setImageResource(drawable);
        holder.color.setColorFilter(parsedColor);
    }

    @Override
    public int getItemCount() {

        return colors.length;
    }

    public interface AccentChangedListener {
        void onAccentChanged(int color);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final ImageView color;

        SimpleViewHolder(View itemView) {
            super(itemView);

            color = (ImageView) itemView;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            //recreate the activity only if necessary
            int color = colors[getAdapterPosition()];
            if (color != mAccent) {
                mOnAccentChangedListener.onAccentChanged(color);
            }
        }
    }
}