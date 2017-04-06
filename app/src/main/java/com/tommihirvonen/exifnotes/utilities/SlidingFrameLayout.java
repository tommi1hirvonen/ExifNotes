package com.tommihirvonen.exifnotes.utilities;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

/**
 * A custom frame layout to be used in frames_fragment.xml and slide_left.xml and slide_right.xml.
 */
public class SlidingFrameLayout extends FrameLayout {

    private float yFraction = 0;
    private float xFraction = 0;

    public SlidingFrameLayout(Context context) {
        super(context);
    }

    public SlidingFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlidingFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private ViewTreeObserver.OnPreDrawListener preDrawListener = null;

    @SuppressWarnings("unused")
    public void setYFraction(float fraction) {

        this.yFraction = fraction;

        if (getHeight() == 0) {
            if (preDrawListener == null) {
                preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
                        setYFraction(yFraction);
                        return true;
                    }
                };
                getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            }
            return;
        }

        float translationY = getHeight() * fraction;
        setTranslationY(translationY);
    }

    @SuppressWarnings("unused")
    public float getYFraction() {
        return this.yFraction;
    }


    @SuppressWarnings("unused")
    public void setXFraction(float fraction) {

        this.xFraction = fraction;

        if (getWidth() == 0) {
            if (preDrawListener == null) {
                preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
                        setXFraction(xFraction);
                        return true;
                    }
                };
                getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            }
            return;
        }

        float translationX = getWidth() * fraction;
        setTranslationX(translationX);
    }

    @SuppressWarnings("unused")
    public float getXFraction() {
        return this.xFraction;
    }
}
