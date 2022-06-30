package com.tommihirvonen.exifnotes.utilities

import android.view.View
import androidx.core.widget.NestedScrollView

/**
 * Class which manages custom Android Marshmallow type scroll indicators inside a NestedScrollView.
 */
open class ScrollIndicatorNestedScrollViewListener(private val nestedScrollView: NestedScrollView,
                                                   private val indicatorUp: View?,
                                                   private val indicatorDown: View?) : NestedScrollView.OnScrollChangeListener {

    init {
        nestedScrollView.post { toggleIndicators() }
    }

    private fun toggleIndicators() {
        // If we can't scroll upwards, hide the up scroll indicator. Otherwise show it.

        // Using canScrollVertically methods only results in severe depression.
        // Instead we use getScrollY methods and avoid the headache entirely.
        // Besides, these methods work the same way on all devices.
        if (nestedScrollView.scrollY == 0) {
            indicatorUp?.visibility = View.INVISIBLE
        } else {
            indicatorUp?.visibility = View.VISIBLE
        }

        // If we can't scroll down, hide the down scroll indicator. Otherwise show it.

        // To get the actual height of the entire NestedScrollView, we have to do the following.
        // The ScrollView always has one child. Getting its height returns the true height
        // of the ScrollView.
        if (nestedScrollView.scrollY == nestedScrollView.getChildAt(0).height - nestedScrollView.height) {
            indicatorDown?.visibility = View.INVISIBLE
        } else {
            indicatorDown?.visibility = View.VISIBLE
        }
    }

    override fun onScrollChange(v: NestedScrollView, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        toggleIndicators()
    }

}