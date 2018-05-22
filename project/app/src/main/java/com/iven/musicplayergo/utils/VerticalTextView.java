package com.iven.musicplayergo.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;


public class VerticalTextView extends TextView {

    Rect mTextBounds = new Rect();
    Path mTextPath = new Path();

    public VerticalTextView(Context context) {
        super(context);
    }

    public VerticalTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        getPaint().getTextBounds(getText().toString(), 0, getText().length(),
                mTextBounds);
        setMeasuredDimension(measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = mTextBounds.height() + getPaddingTop()
                    + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private int measureHeight(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = mTextBounds.width() + getPaddingLeft() + getPaddingRight();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();

        int startX;
        int startY;
        int stopX;
        int stopY;

        startX = (getWidth() - mTextBounds.height() >> 1);
        startY = (getHeight() - mTextBounds.width() >> 1);
        stopX = (getWidth() - mTextBounds.height() >> 1);
        stopY = (getHeight() + mTextBounds.width() >> 1);
        mTextPath.moveTo(startX, startY);
        mTextPath.lineTo(stopX, stopY);

        this.getPaint().setColor(this.getCurrentTextColor());
        canvas.drawTextOnPath(getText().toString(), mTextPath, 0, 0, this.getPaint());
        canvas.restore();
    }
}
