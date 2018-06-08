package com.iven.musicplayergo;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

//inspired by https://github.com/dimorinny/floating-text-button
public class FloatingPlayAll extends FrameLayout {

    private CardView container;

    public FloatingPlayAll(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateLayout(context);
        initView();
    }

    private void inflateLayout(Context context) {
        View v = LayoutInflater.from(context).inflate(R.layout.floating_play_all, this, true);
        container = v.findViewById(R.id.layout_button_container);
    }

    private void initView() {
        initViewRadius();
    }

    private void initViewRadius() {
        container.post(new Runnable() {
            @Override
            public void run() {
                container.setRadius(container.getHeight() / 2);
            }
        });
    }
}
