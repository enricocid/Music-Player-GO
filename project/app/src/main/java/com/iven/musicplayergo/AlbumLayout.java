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

    private Paint mCirclePaint;
    private Paint mBorderPaint;

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

        if (mOffscreenBitmap == null) {
            mOffscreenBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            mOffscreenCanvas = new Canvas(mOffscreenBitmap);
            BitmapShader mBitmapShader = new BitmapShader(mOffscreenBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

            mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCirclePaint.setStyle(Paint.Style.FILL);
            mCirclePaint.setShader(mBitmapShader);

            mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBorderPaint.setStyle(Paint.Style.STROKE);
            mBorderPaint.setStrokeWidth(2);
            mBorderPaint.setShader(null);
            mBorderPaint.setColor(Color.parseColor(String.valueOf(getContentDescription())));
            mBorderPaint.setAlpha(25);
        }
        super.draw(mOffscreenCanvas);

        canvas.drawCircle(canvas.getHeight() / 2, canvas.getHeight() / 2, canvas.getHeight() / 2, mCirclePaint);
        canvas.drawCircle(canvas.getHeight() / 2, canvas.getHeight() / 2, canvas.getHeight() / 2 - 2, mBorderPaint);
    }
}