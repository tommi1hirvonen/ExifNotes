/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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