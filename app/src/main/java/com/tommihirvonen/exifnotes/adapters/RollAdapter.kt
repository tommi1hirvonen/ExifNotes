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

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.ItemRollConstraintBinding
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.utilities.database

/**
 * RollAdapter acts as an adapter between a List of rolls and a RecyclerView.
 *
 * @property context Reference to Activity's context. Used to get resources.
 * @property rolls Reference to the main list of Rolls received from implementing class.
 * @property listener Reference to the implementing class's OnItemClickListener.
 */
class RollAdapter(private val context: Context,
        private val listener: RollAdapterListener) : RecyclerView.Adapter<RollAdapter.ViewHolder>() {

    var rolls = emptyList<Roll>()

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
    interface RollAdapterListener {
        fun onItemClick(roll: Roll, layout: View)
        fun onItemLongClick(roll: Roll)
    }

    val selectedRolls get() = selectedItems.map { it.key }

    /**
     * Used to hold the positions of selected items in the RecyclerView.
     */
    private val selectedItems = mutableMapOf<Roll, Boolean>()

    /**
     * Used to pass the selected item's position to onBindViewHolder(),
     * so that animations are started only when the item is actually selected.
     */
    private var currentSelectedRoll: Roll? = null

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
    private val animationItems = mutableMapOf<Roll, Boolean>()

    /**
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
    inner class ViewHolder(val binding: ItemRollConstraintBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageViews = listOf(binding.dateImageView, binding.filmStockImageView,
                binding.cameraImageView, binding.photosImageView, binding.notesImageView)
        init {
            binding.itemRollLayout.setOnClickListener {
                listener.onItemClick(rolls[bindingAdapterPosition], binding.itemRollTopLayout)
            }
            binding.itemRollLayout.setOnLongClickListener {
                listener.onItemLongClick(rolls[bindingAdapterPosition])
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return ViewHolder(ItemRollConstraintBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val roll = rolls[position]
        val numberOfFrames = context.database.getNumberOfFrames(roll)
        holder.binding.itemRollTopLayout.transitionName = "transition_roll_${roll.id}"
        holder.binding.tvRollDate.text =
                roll.developed?.dateTimeAsText?.also { holder.binding.statusTextView.text = context.resources.getString(R.string.Developed) }
                        ?: roll.unloaded?.dateTimeAsText?.also { holder.binding.statusTextView.text = context.resources.getString(R.string.Unloaded) }
                                ?: roll.date?.dateTimeAsText?.also { holder.binding.statusTextView.text = context.resources.getString(R.string.Loaded) }

        holder.binding.tvRollName.text = roll.name

        if (roll.note?.isNotEmpty() == true) {
            holder.binding.tvRollNote.text = roll.note
            holder.binding.notesLayout.visibility = View.VISIBLE
        } else {
            holder.binding.notesLayout.visibility = View.GONE
        }

        roll.filmStock?.let {
            holder.binding.tvFilmStock.text = it.name
            holder.binding.filmStockLayout.visibility = View.VISIBLE
        } ?: run {
            holder.binding.tvFilmStock.text = ""
            holder.binding.filmStockLayout.visibility = View.GONE
        }

        holder.binding.tvCamera.text = roll.camera?.name ?: context.resources.getString(R.string.NoCamera)

        if (numberOfFrames == 0) {
            holder.binding.tvPhotos.text = context.resources.getString(R.string.NoPhotos)
        } else {
            holder.binding.tvPhotos.text = context.resources.getQuantityString(
                    R.plurals.PhotosAmount, numberOfFrames, numberOfFrames)
        }

        val noFade = 1.0f
        val lightFade = 0.9f
        val moderateFade = 0.5f
        val heavyFade = 0.4f

        // If the roll is archived, fade the text somewhat
        if (roll.archived) {
            holder.imageViews.forEach { it.alpha = heavyFade }
            holder.binding.tvRollName.alpha = heavyFade
            holder.binding.tvFilmStock.alpha = heavyFade
            holder.binding.tvRollDate.alpha = moderateFade
            holder.binding.statusTextView.alpha = moderateFade
            holder.binding.tvRollNote.alpha = moderateFade
            holder.binding.tvPhotos.alpha = moderateFade
            holder.binding.tvCamera.alpha = moderateFade
        } else {
            holder.imageViews.forEach { it.alpha = lightFade }
            holder.binding.tvRollName.alpha = lightFade
            holder.binding.tvFilmStock.alpha = lightFade
            holder.binding.tvRollDate.alpha = noFade
            holder.binding.statusTextView.alpha = noFade
            holder.binding.tvRollNote.alpha = noFade
            holder.binding.tvPhotos.alpha = noFade
            holder.binding.tvCamera.alpha = noFade
        }
        holder.itemView.isActivated = selectedItems[roll] ?: false
        applyCheckBoxAnimation(holder, roll)
    }

    private fun applyCheckBoxAnimation(holder: ViewHolder, roll: Roll) {
        val checkbox = holder.binding.checkbox.root
        val topLayout = holder.binding.itemRollTopLayout
        if (selectedItems[roll] == true) {
            // First set the check box to be visible. This is the state it will be left in after
            // the animation has finished.
            checkbox.visibility = View.VISIBLE

            // Also set a slightly grey background to be visible.
            topLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.background_selected))

            // If the item is selected or all items are being selected and the item was not previously selected
            if (currentSelectedRoll == roll || animateAll && animationItems[roll] != true) {
                val animation = AnimationUtils.loadAnimation(context, R.anim.scale_up)
                checkbox.startAnimation(animation)
                val fromColor = ContextCompat.getColor(context, R.color.transparent)
                val toColor = ContextCompat.getColor(context, R.color.background_selected)
                val colorAnimation = ValueAnimator.ofArgb(fromColor, toColor)
                colorAnimation.duration = 500
                colorAnimation.addUpdateListener { animator: ValueAnimator ->
                    topLayout.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()
                resetCurrentSelectedIndex()
            }
        } else {
            // First set the check box to be gone. This is the state it will be left in after
            // the animation has finished.
            checkbox.visibility = View.GONE

            // Hide the slightly grey background
            topLayout.setBackgroundResource(0)

            // If the item is deselected or all selections are undone and the item was previously selected
            if (currentSelectedRoll == roll || reverseAllAnimations && animationItems[roll] == true) {
                val animation = AnimationUtils.loadAnimation(context, R.anim.scale_down)
                checkbox.startAnimation(animation)
                val fromColor = ContextCompat.getColor(context, R.color.background_selected)
                val toColor = ContextCompat.getColor(context, R.color.transparent)
                val colorAnimation = ValueAnimator.ofArgb(fromColor, toColor)
                colorAnimation.duration = 500
                colorAnimation.addUpdateListener { animator: ValueAnimator ->
                    topLayout.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()
                resetCurrentSelectedIndex()
            }
        }
    }

    override fun getItemCount(): Int = rolls.size

    override fun getItemId(position: Int): Long = rolls[position].id

    fun toggleSelection(roll: Roll) {
        currentSelectedRoll = roll
        if (selectedItems[roll] == true) {
            selectedItems.remove(roll)
            animationItems.remove(roll)
        } else {
            selectedItems[roll] = true
            animationItems[roll] = true
        }
        notifyDataSetChanged()
    }

    fun toggleSelectionAll() {
        resetCurrentSelectedIndex()
        selectedItems.clear()
        animateAll = true
        rolls.forEach { selectedItems[it] = true }
        notifyDataSetChanged()
    }

    fun clearSelections() {
        reverseAllAnimations = true
        selectedItems.clear()
        notifyDataSetChanged()
    }

    /**
     * Called in RollsFragment after all selections have been undone.
     */
    fun resetAnimationIndex() {
        reverseAllAnimations = false
        animationItems.clear()
    }

    /**
     * Called in RollsFragment after all items have been selected using toggleSelectionAll().
     * Sets animateAll back to false and updates animationItemsIndex to be in line with selectedItems.
     */
    fun resetAnimateAll() {
        animateAll = false
        rolls.forEach { animationItems[it] = true }
    }

    /**
     * When the selection/deselection action has been consumed, the index of the (de)selected
     * item is reset.
     */
    private fun resetCurrentSelectedIndex() {
        currentSelectedRoll = null
    }

}