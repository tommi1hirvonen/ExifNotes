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
import com.tommihirvonen.exifnotes.databinding.ItemFrameConstraintBinding
import com.tommihirvonen.exifnotes.entities.Frame
import com.tommihirvonen.exifnotes.utilities.ComplementaryPicturesManager

/**
 * FrameAdapter acts as an adapter to link an ArrayList of Frames and a RecyclerView together.
 *
 * @property context Reference to Activity's context. Used to get resources.
 * @property items Reference to the main list of Frames received from implementing class.
 * @property listener Reference to the implementing class's OnItemClickListener.
 */
class FrameAdapter(
    private val context: Context,
    private val complementaryPicturesManager: ComplementaryPicturesManager,
    private val listener: FrameAdapterListener,
    recyclerView: RecyclerView)
    : SelectableItemAdapter<Frame, FrameAdapter.ViewHolder>(context, recyclerView) {

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

    override val checkboxSelector: (ViewHolder) -> View get() = { holder ->
        holder.binding.checkbox.root
    }

    override val backgroundSelector: (ViewHolder) -> View get() = { holder ->
        holder.binding.selectedBackground
    }

    /**
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
    inner class ViewHolder(val binding: ItemFrameConstraintBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.itemFrameLayout.setOnClickListener {
                listener.onItemClick(items[bindingAdapterPosition], binding.root)
            }
            binding.itemFrameLayout.setOnLongClickListener {
                listener.onItemLongClick(items[bindingAdapterPosition])
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemFrameConstraintBinding.inflate(inflater, parent, false))

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val frame = items[position]
        holder.binding.frame = frame
        val pictureFound = frame.pictureFilename?.let {
            val pictureFile = complementaryPicturesManager.getPictureFile(it)
            pictureFile.exists()
        } ?: false
        holder.binding.complementaryPictureFound = pictureFound

        // Call to super to handle checkbox animations.
        super.onBindViewHolder(holder, position)
    }

    override fun getItemCount(): Int = items.size

    // Implemented because hasStableIds has been set to true.
    override fun getItemId(position: Int): Long = items[position].id

}