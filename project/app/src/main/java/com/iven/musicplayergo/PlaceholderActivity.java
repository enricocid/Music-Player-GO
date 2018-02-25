package com.iven.musicplayergo;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.iven.musicplayergo.utils.AndroidVersion;

public class PlaceholderActivity extends Activity {

    public static final int STARTUP_DELAY = 300;
    public static final int ANIM_ITEM_DURATION = 1000;
    public static final int ITEM_DELAY = 300;

    private ViewGroup mContainer;
    private TextView mRationale;

    @TargetApi(23)
    private boolean hasStoragePermission() {
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_placeholder);
        mContainer = findViewById(R.id.container);
        mRationale = findViewById(R.id.perm_rationale);

        if (AndroidVersion.isMarshmallow()) {
            animate(hasStoragePermission());
        } else {
            animate(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            animateText(ContextCompat.getColor(this, R.color.green_500), true);

        } else {
            animateText(ContextCompat.getColor(this, R.color.pink_500), false);
        }
    }

    public void animateText(int color, final boolean hasPermission) {

        ObjectAnimator a = ObjectAnimator.ofInt(mRationale, "textColor", Color.WHITE, color);

        a.setInterpolator(new LinearInterpolator());
        a.setDuration(1000);
        a.setRepeatCount(ValueAnimator.RESTART);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setEvaluator(new ArgbEvaluator());
        AnimatorSet t = new AnimatorSet();
        t.play(a);
        t.start();

        t.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (hasPermission) {
                    startMusicPlayerGO();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    private void animateLogo(int translationY, final boolean hasPermission) {

        TextView logoView = findViewById(R.id.img_logo);

        if (hasPermission) {
            mContainer.setVisibility(View.GONE);
            ViewGroup.MarginLayoutParams logoViewLayoutParams = (ViewGroup.MarginLayoutParams) logoView.getLayoutParams();
            logoViewLayoutParams.setMargins(0, -translationY, 0, 0);
        }

        ViewCompat.animate(logoView)
                .translationY(translationY)
                .setStartDelay(STARTUP_DELAY)
                .setDuration(ANIM_ITEM_DURATION).setInterpolator(
                new DecelerateInterpolator(1.2f)).setListener(new ViewPropertyAnimatorListener() {
            @Override
            public void onAnimationStart(View view) {
            }

            @Override
            public void onAnimationEnd(View view) {
                if (hasPermission) {
                    startMusicPlayerGO();
                }
            }

            @Override
            public void onAnimationCancel(View view) {
            }
        }).start();
    }

    @TargetApi(23)
    private void requestReadPermission() {
        final int READ_FILES_CODE = 2588;
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
                , READ_FILES_CODE);
    }

    private void animate(final boolean hasPermission) {

        if (!hasPermission) {

            animateLogo(-64, false);
            for (int i = 0; i < mContainer.getChildCount(); i++) {
                View v = mContainer.getChildAt(i);
                ViewPropertyAnimatorCompat viewAnimator;

                if (!(v instanceof Button) && v.equals(mRationale)) {
                    viewAnimator = ViewCompat.animate(v)
                            .translationY(48).alpha(1)
                            .setStartDelay((ITEM_DELAY * i) + 500)
                            .setDuration(1000);
                } else {
                    viewAnimator = ViewCompat.animate(v)
                            .scaleY(1).scaleX(1)
                            .setStartDelay((ITEM_DELAY * i) + 500)
                            .setDuration(500);
                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestReadPermission();
                        }
                    });
                }

                viewAnimator.setInterpolator(new DecelerateInterpolator()).start();
            }
        } else {
            animateLogo(-192, true);
        }
    }

    private void startMusicPlayerGO() {
        Intent i = new Intent(PlaceholderActivity.this, MainActivity.class);
        startActivity(i);
        finish();
    }
}
