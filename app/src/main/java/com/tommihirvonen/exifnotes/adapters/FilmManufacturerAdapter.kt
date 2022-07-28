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
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.databinding.ItemFilmManufacturerBinding
import com.tommihirvonen.exifnotes.databinding.ItemFilmStockBinding
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import java.util.*

class FilmManufacturerAdapter(
        private val context: Context,
        private val listener: (FilmStock?) -> Unit?)
    : RecyclerView.Adapter<FilmManufacturerAdapter.ViewHolder>() {

    private val expandedManufacturers = SparseBooleanArray()
    private val expandAnimations = SparseBooleanArray()
    private var currentExpandedIndex = -1
    private var filmStocksMap: Map<String?, List<FilmStock>> = emptyMap()
    private var manufacturers: List<String?> = emptyList()

    init { setHasStableIds(true) }

    inner class ViewHolder(val binding: ItemFilmManufacturerBinding) : RecyclerView.ViewHolder(binding.root)

    fun setFilmStocks(filmStocks: List<FilmStock>) {
        filmStocksMap = filmStocks.groupBy { it.make }
        manufacturers = filmStocksMap.map { it.key }.sortedBy { it?.lowercase(Locale.ROOT) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return ViewHolder(ItemFilmManufacturerBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val manufacturer = manufacturers[position]
        val filmStocks = filmStocksMap[manufacturer] ?: return
        val adapter = FilmStockAdapter(filmStocks)
        val layoutManager = LinearLayoutManager(context)
        holder.binding.recyclerViewFilmStocks.layoutManager = layoutManager
        holder.binding.recyclerViewFilmStocks.adapter = adapter
        holder.binding.textViewManufacturerName.text = manufacturer
        holder.binding.layoutManufacturer.setOnClickListener {
            toggleManufacturer(position)
            expandOrCollapseManufacturer(holder, position)
        }
        expandOrCollapseManufacturer(holder, position)
    }

    override fun getItemCount(): Int {
        return manufacturers.size
    }

    override fun getItemId(position: Int): Long {
        return manufacturers[position].hashCode().toLong()
    }

    private fun toggleManufacturer(position: Int) {
        currentExpandedIndex = position
        if (expandedManufacturers[position, false]) {
            expandedManufacturers.delete(position)
            expandAnimations.delete(position)
        } else {
            expandedManufacturers.put(position, true)
            expandAnimations.put(position, true)
        }
    }

    private fun expandOrCollapseManufacturer(holder: ViewHolder, position: Int) {
        val animate = currentExpandedIndex == position
        if (expandedManufacturers[position, false]) {
            toggleArrow(holder.binding.imageViewExpand, true, animate)
            toggleLayout(holder.binding.layoutExpand, true, animate)
        } else {
            toggleArrow(holder.binding.imageViewExpand, false, animate)
            toggleLayout(holder.binding.layoutExpand, false, animate)
        }
        if (animate) currentExpandedIndex = -1
    }

    private inner class FilmStockAdapter(private val filmStocks: List<FilmStock>)
        : RecyclerView.Adapter<FilmStockAdapter.ViewHolder>() {

        init { setHasStableIds(true) }

        inner class ViewHolder(val binding: ItemFilmStockBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(context)
            return ViewHolder(ItemFilmStockBinding.inflate(inflater, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val filmStock = filmStocks[position]
            holder.binding.textViewFilmStock.text = filmStock.model
            holder.binding.layoutFilmStock.setOnClickListener { listener(filmStock) }
        }

        override fun getItemCount(): Int {
            return filmStocks.size
        }

        override fun getItemId(position: Int): Long {
            return filmStocks[position].id
        }

    }

    companion object {
        private fun toggleLayout(view: View, isExpanded: Boolean,
                                 animate: Boolean) {
            if (isExpanded) {
                expand(view, animate)
            } else {
                collapse(view, animate)
            }
        }

        private fun toggleArrow(view: View, isExpanded: Boolean, animate: Boolean) {
            if (isExpanded && animate) {
                view.animate().setDuration(200).rotation(180f)
            } else if (isExpanded) {
                view.rotation = 180f
            } else if (animate) {
                view.animate().setDuration(200).rotation(0f)
            } else {
                view.rotation = 0f
            }
        }

        private fun expand(view: View, animate: Boolean) {
            // Call view.measure() before calling view.getMeasuredHeight()
            view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (animate) {
                val actualHeight = view.measuredHeight
                val currentHeight = view.layoutParams.height
                view.visibility = View.VISIBLE
                val animation: Animation = object : Animation() {
                    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                        // interpolatedTime == 1 => the animation has reached its end
                        if (interpolatedTime == 1f) {
                            view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        } else {
                            view.layoutParams.height = (currentHeight + (actualHeight - currentHeight) * interpolatedTime).toInt()
                        }
                        view.requestLayout()
                    }
                }
                animation.duration = (actualHeight / view.context.resources.displayMetrics.density).toLong()
                view.startAnimation(animation)
            } else {
                view.layoutParams.height = view.measuredHeight
                view.visibility = View.VISIBLE
            }
        }

        private fun collapse(view: View, animate: Boolean) {
            if (animate) {
                val actualHeight = view.measuredHeight
                val animation: Animation = object : Animation() {
                    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                        // interpolatedTime == 1 => the animation has reached its end
                        if (interpolatedTime == 1f) {
                            view.visibility = View.GONE
                        } else {
                            view.layoutParams.height = actualHeight - (actualHeight * interpolatedTime).toInt()
                            view.requestLayout()
                        }
                    }
                }
                animation.duration = (actualHeight / view.context.resources.displayMetrics.density).toLong()
                view.startAnimation(animation)
            } else {
                view.visibility = View.GONE
                view.layoutParams.height = 0
            }
        }
    }

}