package com.iven.musicplayergo.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.support.annotation.NonNull;
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
    private AccentChangedListener mOnAccentChangedListener;

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
        mOnAccentChangedListener = (AccentChangedListener) mActivity;
    }

    private Drawable createRipple(Context context, int rippleColor) {
        RippleDrawable ripple;
        ripple = (RippleDrawable) context.getResources().getDrawable(R.drawable.ripple, null);
        if (ripple != null) {
            ripple.setColor(ColorStateList.valueOf(rippleColor));
        }
        return ripple;
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
        holder.color.setBackground(createRipple(mActivity, parsedColor));
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