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
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.ItemGearBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.utilities.toStringList

class CameraAdapter(private val context: Context) : RecyclerView.Adapter<CameraAdapter.ViewHolder>() {

    var cameras: List<Camera> = emptyList()
    var lenses: List<Lens> = emptyList()
    var filters: List<Filter> = emptyList()

    companion object {
        // Since we have to manually add the menu items to the context menus to pass the item position
        // to the implementing class, menu item ids are declared here instead of a menu resource file.
        // No menu resource file can be referenced here, so we use public constants instead.
        const val MENU_ITEM_SELECT_MOUNTABLE_LENSES = 1
        const val MENU_ITEM_SELECT_MOUNTABLE_FILTERS = 2
        const val MENU_ITEM_EDIT = 3
        const val MENU_ITEM_DELETE = 4
    }

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
            // Instead of short click perform long click to activate the OnCreateContextMenuListener.
            binding.itemGearLayout.setOnClickListener { obj: View -> obj.performLongClick() }
            binding.itemGearLayout.setOnCreateContextMenuListener { contextMenu: ContextMenu, _: View?, _: ContextMenu.ContextMenuInfo? ->
                val camera = cameras[bindingAdapterPosition]
                val name = camera.name
                contextMenu.setHeaderTitle(name)

                // Use the order parameter (3rd parameter) of the ContextMenu.add() method
                // to pass the position of the list item which was clicked.
                // This can be used in the implementing class to retrieve the items position.
                if (camera.isNotFixedLens) {
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_LENSES,
                        bindingAdapterPosition, R.string.SelectMountableLenses)
                } else {
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_FILTERS,
                        bindingAdapterPosition, R.string.SelectMountableFilters)
                }
                contextMenu.add(0, MENU_ITEM_EDIT,
                    bindingAdapterPosition, R.string.Edit)
                contextMenu.add(0, MENU_ITEM_DELETE,
                    bindingAdapterPosition, R.string.Delete)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemGearBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val camera = cameras[position]
        holder.binding.tvGearName.text = camera.name
        val stringBuilder = StringBuilder()
        if (camera.isFixedLens) {
            camera.lens?.let { lens ->
                val filters = this.filters.filter { lens.filterIds.contains(it.id) }
                stringBuilder
                    .append(context.getString(R.string.FixedLens)).append("\n\n")
                    .append(context.getString(R.string.FiltersNoCap)).append(":")
                    .append(filters.toStringList())
            }
        } else {
            val lenses = this.lenses.filter { camera.lensIds.contains(it.id) }
            stringBuilder.append(context.getString(R.string.LensesNoCap)).append(":")
                .append(lenses.toStringList())
        }
        holder.binding.tvMountables.text = stringBuilder.toString()
    }

    override fun getItemCount(): Int = cameras.size

    override fun getItemId(position: Int): Long = cameras[position].id
}