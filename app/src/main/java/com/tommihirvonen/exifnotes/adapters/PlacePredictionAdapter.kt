/*
 * Exif Notes
 * Copyright (C) 2023  Tommi Hirvonen
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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.tommihirvonen.exifnotes.adapters.PlacePredictionAdapter.PlacePredictionViewHolder
import com.tommihirvonen.exifnotes.databinding.ItemPlacePredictionBinding

class PlacePredictionAdapter
    : RecyclerView.Adapter<PlacePredictionViewHolder>() {

    private val predictions = mutableListOf<AutocompletePrediction>()
    var onPlaceClickListener: ((AutocompletePrediction) -> Any?)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlacePredictionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPlacePredictionBinding.inflate(inflater, parent, false)
        return PlacePredictionViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return predictions.size
    }

    override fun onBindViewHolder(holder: PlacePredictionViewHolder, position: Int) {
        val place = predictions[position]
        holder.binding.placeName.text = place.getPrimaryText(null)
        holder.binding.placeAddress.text = place.getSecondaryText(null)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setPredictions(predictions: List<AutocompletePrediction>) {
        this.predictions.clear()
        this.predictions.addAll(predictions)
        notifyDataSetChanged()
    }

    inner class PlacePredictionViewHolder(val binding: ItemPlacePredictionBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val place = predictions[bindingAdapterPosition]
                onPlaceClickListener?.invoke(place)
            }
        }
    }

}