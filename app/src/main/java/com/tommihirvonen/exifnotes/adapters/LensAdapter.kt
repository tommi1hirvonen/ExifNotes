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
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.ItemGearBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.utilities.toStringList

class LensAdapter(
    private val context: Context,
    private val onLensClickListener: (Lens) -> Any) : RecyclerView.Adapter<LensAdapter.ViewHolder>() {

    var lenses: List<Lens> = emptyList()
    var cameras: List<Camera> = emptyList()
    var filters: List<Filter> = emptyList()

    init {
        // Used to make the RecyclerView perform better and to make our custom animations
        // work more reliably. Now we can use notifyDataSetChanged(), which works well
        // with possible custom animations.
        setHasStableIds(true)
    }

    /**
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
    inner class ViewHolder(val binding: ItemGearBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.itemGearLayout.setOnClickListener {
                val lens = lenses[bindingAdapterPosition]
                onLensClickListener(lens)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemGearBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lens = lenses[position]
        holder.binding.title.text = lens.name
        val stringBuilder = StringBuilder()
        val mountableCameras = cameras.filter { lens.cameraIds.contains(it.id) }
        val mountableFilters = filters.filter { lens.filterIds.contains(it.id) }
        stringBuilder.append(context.getString(R.string.CamerasNoCap)).append(":")
            .append(mountableCameras.toStringList()).append("\n\n")
            .append(context.getString(R.string.FiltersNoCap)).append(":")
            .append(mountableFilters.toStringList())
        holder.binding.description.text = stringBuilder.toString()
    }

    override fun getItemCount(): Int = lenses.size

    override fun getItemId(position: Int): Long = lenses[position].id
}