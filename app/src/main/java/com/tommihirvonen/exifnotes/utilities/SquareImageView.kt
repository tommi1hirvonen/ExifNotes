package com.tommihirvonen.exifnotes.utilities

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * Custom ImageView class, that forces the view to be square.
 */
class SquareImageView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Set the view height to be the same as the measured width.
        val width = measuredWidth
        setMeasuredDimension(width, width)
    }
}