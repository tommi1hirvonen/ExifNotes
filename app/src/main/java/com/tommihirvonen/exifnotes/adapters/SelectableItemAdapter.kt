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

package com.tommihirvonen.exifnotes.adapters

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R

/**
 * Abstract RecyclerView adapter to help handle item selection related
 * checkbox and background animations. The class also keeps track of selected items.
 *
 * @property items Main items list. This property needs to provided and should correspond to the
 * main list of items drawn by the implementing adapter.
 * @property checkboxSelector Property getter to map the ViewHolder to a checkbox View
 * depicting the items selection
 * @property backgroundSelector Property getter to map the ViewHolder to a background View
 * depicting the items selection
 * @property selectedItems Convenience method to get a list of selected items
 */
abstract class SelectableItemAdapter<T, U : RecyclerView.ViewHolder>(
    private val context: Context,
    private val recyclerView: RecyclerView
) : RecyclerView.Adapter<U>() {

    var items = emptyList<T>()

    protected abstract val checkboxSelector: (U) -> View
    protected open val backgroundSelector: (U) -> View? = { null }

    val selectedItems get() = mSelectedItems.keys

    /**
     * Used to hold the positions of selected items in the RecyclerView.
     */
    private val mSelectedItems = mutableMapOf<T, Boolean>()

    /**
     * Used to pass the selected item's position to onBindViewHolder(),
     * so that animations are started only when the item is actually selected.
     */
    private var mCurrentSelectedItem: T? = null

    /**
     * Helper boolean used to indicate when all item selections are being undone.
     */
    private var mReverseAllAnimations = false

    /**
     * Helper boolean used to indicate when all items are being selected.
     */
    private var mAnimateAll = false

    /**
     * Helper array to keep track of animation statuses.
     */
    private val mAnimationItems = mutableMapOf<T, Boolean>()

    override fun onBindViewHolder(holder: U, position: Int) {
        val item = items[position]
        holder.itemView.isActivated = mSelectedItems[item] ?: false
        val checkbox = checkboxSelector(holder)
        val background = backgroundSelector(holder)
        if (mSelectedItems[item] == true) {
            // First set the check box to be visible. This is the state it will be left in after
            // the animation has finished.
            checkbox.visibility = View.VISIBLE

            // Also set a slightly grey background to be visible.
            background?.visibility = View.VISIBLE

            // If the item is selected or all items are being selected and the item was not previously selected
            if (mCurrentSelectedItem == item || mAnimateAll && mAnimationItems[item] != true) {
                val checkboxAnimation = AnimationUtils.loadAnimation(context, R.anim.scale_up)
                checkbox.startAnimation(checkboxAnimation)
                val backgroundAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
                background?.startAnimation(backgroundAnimation)
                resetCurrentSelectedItem()
            }
        } else {
            // First set the check box to be gone. This is the state it will be left in after
            // the animation has finished.
            checkbox.visibility = View.GONE

            // Hide the slightly grey background
            background?.visibility = View.GONE

            // If the item is deselected or all selections are undone and the item was previously selected
            if (mCurrentSelectedItem == item || mReverseAllAnimations && mAnimationItems[item] == true) {
                val checkboxAnimation = AnimationUtils.loadAnimation(context, R.anim.scale_down)
                checkbox.startAnimation(checkboxAnimation)
                val backgroundAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
                background?.startAnimation(backgroundAnimation)
                resetCurrentSelectedItem()
            }
        }
    }

    /**
     * Use to toggle the selection state of a single item in the list.
     */
    fun toggleSelection(item: T) {
        mCurrentSelectedItem = item
        if (mSelectedItems[item] == true) {
            mSelectedItems.remove(item)
            mAnimationItems.remove(item)
        } else {
            mSelectedItems[item] = true
            mAnimationItems[item] = true
        }
        notifyDataSetChanged()
    }

    /**
     * Can be called to select all items in the list.
     * After animations have been completed on the RecyclerView,
     * resetAnimateAll() should be called.
     */
    fun toggleSelectionAll() {
        resetCurrentSelectedItem()
        mSelectedItems.clear()
        mAnimateAll = true
        items.forEach { mSelectedItems[it] = true }
        notifyDataSetChanged()
        recyclerView.post {
            // Update animation items to be in line with selected items
            // after RecyclerView animations have completed.
            mAnimateAll = false
            items.forEach { mAnimationItems[it] = true }
        }
    }

    /**
     * Can be called to undo all selections.
     * After animations have been completed on the RecyclerView,
     * resetAnimationIndex() should be called.
     */
    fun clearSelections() {
        mReverseAllAnimations = true
        mSelectedItems.clear()
        notifyDataSetChanged()
        recyclerView.post {
            // Clear animation items after RecyclerView animations have completed.
            mReverseAllAnimations = false
            mAnimationItems.clear()
        }
    }

    /**
     * Should be called when the selection/deselection action has been consumed.
     */
    private fun resetCurrentSelectedItem() {
        mCurrentSelectedItem = null
    }
}