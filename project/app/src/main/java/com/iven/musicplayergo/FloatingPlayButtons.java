package com.iven.musicplayergo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

//inspired by https://github.com/dimorinny/floating-text-button
public class FloatingPlayButtons extends FrameLayout {

    private CardView container;

    public FloatingPlayButtons(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateLayout(context);
        container.post(new Runnable() {
            @Override
            public void run() {
                container.setRadius(container.getHeight() / 2);
            }
        });
    }

    private void inflateLayout(@NonNull final Context context) {
        final View v = LayoutInflater.from(context).inflate(R.layout.player_buttons, this, true);
        container = v.findViewById(R.id.layout_button_container);
    }
}
