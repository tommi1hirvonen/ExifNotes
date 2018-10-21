package com.tommihirvonen.exifnotes.utilities;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

/**
 * Custom ImageView class, that forces the view to be square.
 */
public class SquareImageView extends android.support.v7.widget.AppCompatImageView {

    /**
     * {@inheritDoc}
     * @param context {@inheritDoc}
     * @param attrs {@inheritDoc}
     */
    public SquareImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Set the view height to be the same as the measured width.
     *
     * @param widthMeasureSpec {@inheritDoc}
     * @param heightMeasureSpec {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        setMeasuredDimension(width, width);
    }

}