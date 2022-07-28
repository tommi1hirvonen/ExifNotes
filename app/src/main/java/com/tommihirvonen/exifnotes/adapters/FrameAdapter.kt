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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.ItemFrameConstraintBinding
import com.tommihirvonen.exifnotes.datastructures.Frame
import com.tommihirvonen.exifnotes.utilities.ComplementaryPicturesManager

/**
 * FrameAdapter acts as an adapter to link an ArrayList of Frames and a RecyclerView together.
 *
 * @property context Reference to Activity's context. Used to get resources.
 * @property frames Reference to the main list of Frames received from implementing class.
 * @property listener Reference to the implementing class's OnItemClickListener.
 */
class FrameAdapter(private val context: Context,
        private val listener: FrameAdapterListener) : RecyclerView.Adapter<FrameAdapter.ViewHolder>() {

    var frames = emptyList<Frame>()

    init {
        // Used to make the RecyclerView perform better and to make our custom animations
        // work more reliably. Now we can use notifyDataSetChanged(), which works well
        // with our custom animations.
        setHasStableIds(true)
    }

    /**
     * Interface for implementing classes.
     * Used to send onItemClicked messages back to implementing Activities and/or Fragments.
     */
    interface FrameAdapterListener {
        fun onItemClick(frame: Frame, view: View)
        fun onItemLongClick(frame: Frame)
    }

    val selectedFrames get() = selectedItems.map{ it.key }

    /**
     * Used to hold the positions of selected items in the RecyclerView.
     */
    private val selectedItems = mutableMapOf<Frame, Boolean>()

    /**
     * Used to pass the selected item's position to onBindViewHolder(),
     * so that animations are started only when the item is actually selected.
     */
    private var currentSelectedFrame: Frame? = null

    /**
     * Helper boolean used to indicate when all item selections are being undone.
     */
    private var reverseAllAnimations = false

    /**
     * Helper boolean used to indicate when all items are being selected.
     */
    private var animateAll = false

    /**
     * Helper array to keep track of animation statuses.
     */
    private val animationItems = mutableMapOf<Frame, Boolean>()

    /**
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
    inner class ViewHolder(val binding: ItemFrameConstraintBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.itemFrameLayout.setOnClickListener {
                listener.onItemClick(frames[bindingAdapterPosition], binding.root)
            }
            binding.itemFrameLayout.setOnLongClickListener {
                listener.onItemLongClick(frames[bindingAdapterPosition])
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemFrameConstraintBinding.inflate(inflater, parent, false))

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val frame = frames[position]
        holder.binding.root.transitionName = "transition_frame_${frame.id}"
        holder.binding.tvFrameText.text = frame.date?.dateTimeAsText
        holder.binding.tvCount.text = "${frame.count}"
        holder.binding.tvFrameText2.text = frame.lens?.name
            ?:
            if (frame.roll.camera?.isNotFixedLens == true) context.resources.getString(R.string.NoLens)
            else ""
        holder.binding.tvFrameNote.text = frame.note
        holder.binding.tvAperture.text = frame.aperture?.let { "f/$it" } ?: ""
        holder.binding.tvShutter.text = frame.shutter

        holder.binding.pictureImageView.visibility = View.INVISIBLE
        holder.binding.brokenPictureImageView.visibility = View.INVISIBLE
        frame.pictureFilename?.let {
            val pictureFile = ComplementaryPicturesManager.getPictureFile(context, it)
            if (pictureFile.exists()) holder.binding.pictureImageView.visibility = View.VISIBLE
            else holder.binding.brokenPictureImageView.visibility = View.VISIBLE
        }

        holder.itemView.isActivated = selectedItems[frame] ?: false
        applyCheckBoxAnimation(holder, frame)
    }

    private fun applyCheckBoxAnimation(holder: ViewHolder, frame: Frame) {
        val checkbox = holder.binding.checkbox.root
        val background = holder.binding.greyBackground
        if (selectedItems[frame] == true) {
            // First set the check box to be visible. This is the state it will be left in after
            // the animation has finished.
            checkbox.visibility = View.VISIBLE
            // Also set a slightly grey background to be visible.
            background.visibility = View.VISIBLE

            // If the item is selected or all items are being selected and the item was not previously selected
            if (currentSelectedFrame == frame || animateAll && animationItems[frame] != true) {
                val animation = AnimationUtils.loadAnimation(context, R.anim.scale_up)
                checkbox.startAnimation(animation)
                val animation1 = AnimationUtils.loadAnimation(context, R.anim.fade_in)
                background.startAnimation(animation1)
                resetCurrentSelectedIndex()
            }
        } else {
            // First set the check box to be gone. This is the state it will be left in after
            // the animation has finished.
            checkbox.visibility = View.GONE
            // Hide the slightly grey background
            background.visibility = View.GONE

            // If the item is deselected or all selections are undone and the item was previously selected
            if (currentSelectedFrame == frame || reverseAllAnimations && animationItems[frame] == true) {
                val animation = AnimationUtils.loadAnimation(context, R.anim.scale_down)
                checkbox.startAnimation(animation)
                val animation1 = AnimationUtils.loadAnimation(context, R.anim.fade_out)
                background.startAnimation(animation1)
                resetCurrentSelectedIndex()
            }
        }
    }

    override fun getItemCount(): Int = frames.size

    // Implemented because hasStableIds has been set to true.
    override fun getItemId(position: Int): Long = frames[position].id

    fun toggleSelection(frame: Frame) {
        currentSelectedFrame = frame
        if (selectedItems[frame] == true) {
            selectedItems.remove(frame)
            animationItems.remove(frame)
        } else {
            selectedItems[frame] = true
            animationItems[frame] = true
        }
        notifyDataSetChanged()
    }

    fun toggleSelectionAll() {
        resetCurrentSelectedIndex()
        selectedItems.clear()
        animateAll = true
        frames.forEach { selectedItems[it] = true }
        notifyDataSetChanged()
    }

    fun clearSelections() {
        reverseAllAnimations = true
        selectedItems.clear()
        notifyDataSetChanged()
    }

    /**
     * Called in FramesFragment after all selections have been undone.
     */
    fun resetAnimationIndex() {
        reverseAllAnimations = false
        animationItems.clear()
    }

    /**
     * Called in FramesFragment after all items have been selected using toggleSelectionAll().
     * Sets animateAll back to false and updates animationItemsIndex to be in line with selectedItems.
     */
    fun resetAnimateAll() {
        animateAll = false
        frames.forEach { animationItems[it] = true }
    }

    /**
     * When the selection/deselection action has been consumed, the index of the (de)selected
     * item is reset.
     */
    private fun resetCurrentSelectedIndex() {
        currentSelectedFrame = null
    }

}