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
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.ItemRollConstraintBinding
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.utilities.database

/**
 * RollAdapter acts as an adapter between a List of rolls and a RecyclerView.
 *
 * @property context Reference to Activity's context. Used to get resources.
 * @property items Reference to the main list of Rolls received from implementing class.
 * @property listener Reference to the implementing class's OnItemClickListener.
 */
class RollAdapter(
    private val context: Context,
    private val listener: RollAdapterListener,
    recyclerView: RecyclerView)
    : SelectableItemAdapter<Roll, RollAdapter.ViewHolder>(context, recyclerView) {

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
        fun onItemClick(roll: Roll, layout: View, position: Int)
        fun onItemLongClick(roll: Roll)
    }

    override val checkboxSelector: (ViewHolder) -> View get() = { holder ->
        holder.binding.checkbox.root
    }

    override val backgroundSelector: (ViewHolder) -> View? get() = { holder ->
        holder.binding.selectedBackground
    }

    /**
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
    inner class ViewHolder(val binding: ItemRollConstraintBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.itemRollLayout.setOnClickListener {
                listener.onItemClick(this@RollAdapter.items[bindingAdapterPosition], binding.itemRollTopLayout, bindingAdapterPosition)
            }
            binding.itemRollLayout.setOnLongClickListener {
                listener.onItemLongClick(this@RollAdapter.items[bindingAdapterPosition])
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return ViewHolder(ItemRollConstraintBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val roll = this.items[position]
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

        // Call to super to handle checkbox animations.
        super.onBindViewHolder(holder, position)
    }

    override fun getItemCount(): Int = this.items.size

    // Implemented because hasStableIds has been set to true.
    override fun getItemId(position: Int): Long = this.items[position].id

}