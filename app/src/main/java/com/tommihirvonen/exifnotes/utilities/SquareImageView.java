package com.tommihirvonen.exifnotes.utilities;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

/**
 * Custom ImageView class, that forces the view to be square.
 */
public class SquareImageView extends androidx.appcompat.widget.AppCompatImageView {

    /**
     * {@inheritDoc}
     * @param context {@inheritDoc}
     * @param attrs {@inheritDoc}
     */
    public SquareImageView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Set the view height to be the same as the measured width.
     *
     * @param widthMeasureSpec {@inheritDoc}
     * @param heightMeasureSpec {@inheritDoc}
     */
    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        setMeasuredDimension(width, width);
    }

}
