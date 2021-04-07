package com.imnjh.imagepicker.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.RelativeLayout;

/**
 * Created by Martin on 2017/1/17.
 */
public class ClipImageLayout extends RelativeLayout {
    private ClipZoomImageView zoomImageView;
    private ClipImageBorderView clipImageView;
    private int horizontalPadding = 0;
    private int aspectX = 1;
    private int aspectY = 1;

    public ClipImageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        zoomImageView = new ClipZoomImageView(context);
        clipImageView = new ClipImageBorderView(context);

        android.view.ViewGroup.LayoutParams lp = new LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT);

        this.addView(zoomImageView, lp);
        this.addView(clipImageView, lp);

        horizontalPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, horizontalPadding, getResources()
                        .getDisplayMetrics());
        zoomImageView.setHorizontalPadding(horizontalPadding);
        clipImageView.setHorizontalPadding(horizontalPadding);
        clipImageView.setAspectX(aspectX);
        clipImageView.setAspectY(aspectY);
        zoomImageView.setAspectX(aspectX);
        zoomImageView.setAspectY(aspectY);
    }

    public void setImageDrawable(Drawable drawable) {
        zoomImageView.setImageDrawable(drawable);
    }

    public void setImageBitmap(Bitmap bitmap) {
        zoomImageView.setImageBitmap(bitmap);
    }

    public void setHorizontalPadding(int mHorizontalPadding) {
        this.horizontalPadding = mHorizontalPadding;
    }

    public int getAspectX() {
        return aspectX;
    }

    public void setAspectX(int aspectX) {
        if (aspectX <= 0) {
            aspectX = 1;
        }
        this.aspectX = aspectX;
        clipImageView.setAspectX(aspectX);
        zoomImageView.setAspectX(aspectX);
    }

    public int getAspectY() {
        return aspectY;
    }

    public void setAspectY(int aspectY) {
        if (aspectY <= 0) {
            aspectY = 1;
        }
        this.aspectY = aspectY;
        clipImageView.setAspectY(aspectY);
        zoomImageView.setAspectY(aspectY);
    }

    public Bitmap clip() {
        return zoomImageView.clip();
    }
}
