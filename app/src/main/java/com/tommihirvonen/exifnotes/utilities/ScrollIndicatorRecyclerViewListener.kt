package com.tommihirvonen.exifnotes.utilities

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Class which manages custom Android Marshmallow type scroll indicators based on a RecyclerView
 */
class ScrollIndicatorRecyclerViewListener(private val recyclerView: RecyclerView,
                                          private val indicatorUp: View,
                                          private val indicatorDown: View
) : RecyclerView.OnScrollListener() {

    init {
        recyclerView.post { toggleIndicators() }
    }

    private fun toggleIndicators() {
        // If we can't scroll upwards, hide the up scroll indicator. Otherwise show it.
        if (!recyclerView.canScrollVertically(-1)) {
            indicatorUp.visibility = View.INVISIBLE
        } else {
            indicatorUp.visibility = View.VISIBLE
        }
        // If we can't scroll down, hide the down scroll indicator. Otherwise show it.
        if (!recyclerView.canScrollVertically(1)) {
            indicatorDown.visibility = View.INVISIBLE
        } else {
            indicatorDown.visibility = View.VISIBLE
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        toggleIndicators()
    }

}