/*
 * Exif Notes
 * Copyright (C) 2024  Tommi Hirvonen
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
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.databinding.ItemLabelBinding

class LabelAdapter(private val context: Context,
                   private val onLabelClickListener: (Label, View) -> Any,
                   private val onLabelDeleteClickListener: (Label, View) -> Any)
    : RecyclerView.Adapter<LabelAdapter.ViewHolder>(){

    var labels = emptyList<Label>()

    init {
        setHasStableIds(true)
    }

    inner class ViewHolder(val binding: ItemLabelBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            val label = labels[bindingAdapterPosition]
            binding.itemLabelLayout.setOnClickListener {
                onLabelClickListener(label, binding.root)
            }
            binding.deleteImageView.setOnClickListener {
                onLabelDeleteClickListener(label, binding.deleteImageView)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelAdapter.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemLabelBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val label = labels[position]
        holder.binding.title.text = label.name
        holder.binding.description.text = context.resources
            .getQuantityString(R.plurals.RollsAmount, label.rollCount, label.rollCount)
    }

    override fun getItemCount(): Int = labels.size

    override fun getItemId(position: Int): Long = labels[position].id
}