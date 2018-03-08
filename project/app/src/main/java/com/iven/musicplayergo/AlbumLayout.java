package com.iven.musicplayergo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.FrameLayout;

class AlbumLayout extends FrameLayout {

    private Bitmap mOffscreenBitmap;
    private Canvas mOffscreenCanvas;
    private BitmapShader mBitmapShader;

    public AlbumLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AlbumLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init() {
        setWillNotDraw(false);
    }

    @Override
    public void draw(Canvas canvas) {

        Paint circlePaint;
        if (mOffscreenBitmap == null) {
            mOffscreenBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            mOffscreenCanvas = new Canvas(mOffscreenBitmap);
            mBitmapShader = new BitmapShader(mOffscreenBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        }
        super.draw(mOffscreenCanvas);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setShader(mBitmapShader);
        canvas.drawCircle(canvas.getHeight() / 2, canvas.getHeight() / 2, canvas.getHeight() / 2, circlePaint);

        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(2);
        circlePaint.setShader(null);
        circlePaint.setColor(Color.parseColor(String.valueOf(getContentDescription())));
        circlePaint.setAlpha(25);
        canvas.drawCircle(canvas.getHeight() / 2, canvas.getHeight() / 2, canvas.getHeight() / 2 - 2, circlePaint);
    }
}