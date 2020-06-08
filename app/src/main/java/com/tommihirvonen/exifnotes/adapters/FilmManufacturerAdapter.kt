package com.tommihirvonen.exifnotes.adapters

import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper
import java.util.*

class FilmManufacturerAdapter(
        private val context: Context,
        private val listener: (FilmStock?) -> Unit?)
    : RecyclerView.Adapter<FilmManufacturerAdapter.ViewHolder>() {

    private val expandedManufacturers = SparseBooleanArray()
    private val expandAnimations = SparseBooleanArray()
    private var currentExpandedIndex = -1
    private val filmStocksMap: Map<String?, List<FilmStock>>
    private val manufacturers: List<String?>

    init {
        val database = FilmDbHelper.getInstance(context)
        filmStocksMap = database.allFilmStocks.groupBy { it.make }
        manufacturers = filmStocksMap.map { it.key }.sortedBy { it?.toLowerCase(Locale.ROOT) }
        setHasStableIds(true)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val manufacturerTextView: TextView = itemView.findViewById(R.id.text_view_manufacturer_name)
        val manufacturerLayout: View = itemView.findViewById(R.id.layout_manufacturer)
        val expandLayout: View = itemView.findViewById(R.id.layout_expand)
        val expandButton: ImageView = itemView.findViewById(R.id.image_view_expand)
        val filmStocksRecyclerView: RecyclerView = itemView.findViewById(R.id.recycler_view_film_stocks)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_film_manufacturer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val manufacturer = manufacturers[position]
        val filmStocks = filmStocksMap[manufacturer] ?: return
        val adapter = FilmStockAdapter(filmStocks)
        val layoutManager = LinearLayoutManager(context)
        holder.filmStocksRecyclerView.layoutManager = layoutManager
        holder.filmStocksRecyclerView.adapter = adapter
        holder.manufacturerTextView.text = manufacturer
        holder.manufacturerLayout.setOnClickListener {
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
            toggleArrow(holder.expandButton, true, animate)
            toggleLayout(holder.expandLayout, true, animate)
        } else {
            toggleArrow(holder.expandButton, false, animate)
            toggleLayout(holder.expandLayout, false, animate)
        }
        if (animate) currentExpandedIndex = -1
    }

    private inner class FilmStockAdapter internal constructor(private val filmStocks: List<FilmStock>)
        : RecyclerView.Adapter<FilmStockAdapter.ViewHolder>() {

        init {
            setHasStableIds(true)
        }

        internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val filmStockTextView: TextView = itemView.findViewById(R.id.text_view_film_stock)
            val filmStockLayout: LinearLayout = itemView.findViewById(R.id.layout_film_stock)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_film_stock, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val filmStock = filmStocks[position]
            holder.filmStockTextView.text = filmStock.model
            holder.filmStockLayout.setOnClickListener { listener(filmStock) }
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