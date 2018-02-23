package com.iven.musicplayergo.adapters;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.iven.musicplayergo.R;

public class ColorsAdapter extends RecyclerView.Adapter<ColorsAdapter.SimpleViewHolder> {

    private Activity mActivity;
    private int mAccent;
    private onAccentChangedListener mOnAccentChangedListener;

    //fixed int array of accent colors
    private int[] colors = new int[]{

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
            R.color.deep_orange_A400
    };

    public ColorsAdapter(Activity activity, int accent) {
        mActivity = activity;
        mAccent = accent;
        mOnAccentChangedListener = (onAccentChangedListener) mActivity;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.color_item, parent, false);
        return new SimpleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {

        int color = colors[holder.getAdapterPosition()];

        int drawable = color != mAccent ? R.drawable.ic_checkbox_blank_circle_24dp : R.drawable.ic_checkbox_marked_circle_24dp;
        holder.color.setImageResource(drawable);
        holder.color.setColorFilter(ContextCompat.getColor(mActivity, color));
    }

    @Override
    public int getItemCount() {

        return colors.length;
    }

    public interface onAccentChangedListener {
        void onAccentChanged(int color);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageButton color;

        SimpleViewHolder(View itemView) {
            super(itemView);

            color = (ImageButton) itemView;
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