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

package com.tommihirvonen.exifnotes.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.GearActivity
import com.tommihirvonen.exifnotes.adapters.FilmStockAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentFilmsBinding
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.dialogs.EditFilmStockDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.database

class FilmStocksFragment : Fragment(), MenuProvider {

    companion object {
        private const val SORT_MODE_NAME = 1
        private const val SORT_MODE_ISO = 2
        private const val FILTER_MODE_ALL = 0
        private const val FILTER_MODE_ADDED_BY_USER = 1
        private const val FILTER_MODE_PREADDED = 2
    }

    private lateinit var binding: FragmentFilmsBinding
    private lateinit var allFilmStocks: MutableList<FilmStock>
    private lateinit var filteredFilmStocks: MutableList<FilmStock>
    private lateinit var filmStockAdapter: FilmStockAdapter
    private var sortMode = SORT_MODE_NAME
    private var manufacturerFilterList = emptyList<String>().toMutableList()
    private var isoFilterList = emptyList<Int>().toMutableList()
    private var filmTypeFilterList = emptyList<Int>().toMutableList()
    private var filmProcessFilterList = emptyList<Int>().toMutableList()
    private var addedByFilterMode = FILTER_MODE_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allFilmStocks = database.filmStocks.toMutableList()
        filteredFilmStocks = allFilmStocks.toMutableList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentFilmsBinding.inflate(inflater, container, false)
        binding.fabFilms.setOnClickListener(onFabClickListener)

        val layoutManager = LinearLayoutManager(activity)
        binding.filmsRecyclerView.layoutManager = layoutManager
        filmStockAdapter = FilmStockAdapter(requireActivity(), onFilmStockClickListener)
        filmStockAdapter.filmStocks = filteredFilmStocks
        binding.filmsRecyclerView.adapter = filmStockAdapter
        filmStockAdapter.notifyDataSetChanged()

        val activity = requireActivity() as GearActivity
        activity.topAppBar.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_film_stocks_fragment, menu)
        menu.findItem(R.id.sort_mode_film_stock_name).isChecked = true
    }

    override fun onPrepareMenu(menu: Menu) {
        when (sortMode) {
            SORT_MODE_NAME -> menu.findItem(R.id.sort_mode_film_stock_name).isChecked = true
            SORT_MODE_ISO -> menu.findItem(R.id.sort_mode_film_stock_iso).isChecked = true
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.sort_mode_film_stock_name -> {
                setSortMode(SORT_MODE_NAME, true)
                menuItem.isChecked = true
                return true
            }
            R.id.sort_mode_film_stock_iso -> {
                setSortMode(SORT_MODE_ISO, true)
                menuItem.isChecked = true
                return true
            }
            R.id.filter_mode_film_manufacturer -> {
                showManufacturerFilterDialog()
                return true
            }
            R.id.filter_mode_added_by -> {
                showAddedByFilterDialog()
                return true
            }
            R.id.filter_mode_film_iso -> {
                showIsoValuesFilterDialog()
                return true
            }
            R.id.filter_mode_film_type -> {
                showFilmTypeFilterDialog()
                return true
            }
            R.id.filter_mode_film_process -> {
                showFilmProcessFilterDialog()
                return true
            }
            R.id.filter_mode_reset -> {
                resetFilters()
                return true
            }
        }
        return false
    }

    private val onFabClickListener = { _: View ->
        val dialog = EditFilmStockDialog()
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewFilmStock))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), null)
        dialog.setFragmentResultListener("EditFilmStockDialog") { _, bundle ->
            val filmStock: FilmStock = bundle.getParcelable(ExtraKeys.FILM_STOCK)
                ?: return@setFragmentResultListener
            database.addFilmStock(filmStock)
            // Add the new film stock to both lists.
            filteredFilmStocks.add(filmStock) // The new film stock is shown immediately regardless of filters.
            allFilmStocks.add(filmStock) // The new film stock is shown after new filters are applied and they match.
            sortFilmStocks()
            val position = filteredFilmStocks.indexOf(filmStock)
            filmStockAdapter.notifyItemInserted(position)
            binding.filmsRecyclerView.scrollToPosition(position)
        }
    }

    private val onFilmStockClickListener = { filmStock: FilmStock ->
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(filmStock.name)
        val items = arrayOf(
            requireActivity().getString(R.string.Edit),
            requireActivity().getString(R.string.Delete)
        )
        builder.setItems(items) { _, which ->
            when (which) {
                0 -> { openFilmStockEditDialog(filmStock) }
                1 -> { confirmDeleteFilmStock(filmStock) }
            }
        }
        builder.setNegativeButton(R.string.Cancel) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun openFilmStockEditDialog(filmStock: FilmStock) {
        val dialog = EditFilmStockDialog()
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditFilmStock))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
        arguments.putParcelable(ExtraKeys.FILM_STOCK, filmStock)
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), null)
        dialog.setFragmentResultListener("EditFilmStockDialog") { _, bundle ->
            val filmStock1: FilmStock = bundle.getParcelable(ExtraKeys.FILM_STOCK)
                ?: return@setFragmentResultListener
            database.updateFilmStock(filmStock1)
            val oldPosition = filteredFilmStocks.indexOf(filmStock1)
            sortFilmStocks()
            val newPosition = filteredFilmStocks.indexOf(filmStock1)
            filmStockAdapter.notifyItemChanged(oldPosition)
            filmStockAdapter.notifyItemMoved(oldPosition, newPosition)
        }
    }

    private fun confirmDeleteFilmStock(filmStock: FilmStock) {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(
            resources.getString(R.string.DeleteFilmStock) + " " + filmStock.name
        )
        if (database.isFilmStockBeingUsed(filmStock)) {
            builder.setMessage(R.string.FilmStockIsInUseConfirmation)
        }
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
            database.deleteFilmStock(filmStock)
            filteredFilmStocks.remove(filmStock)
            allFilmStocks.remove(filmStock)
            filmStockAdapter.notifyDataSetChanged()
        }
        builder.create().show()
    }

    private fun sortFilmStocks() {
        setSortMode(sortMode, false)
    }

    private fun setSortMode(sortMode_: Int, notifyDataSetChanged: Boolean) {
        when (sortMode_) {
            SORT_MODE_NAME -> {
                sortMode = SORT_MODE_NAME
                filteredFilmStocks.sortWith { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) }
            }
            SORT_MODE_ISO -> {
                sortMode = SORT_MODE_ISO
                filteredFilmStocks.sortWith { o1, o2 -> o1.iso.compareTo(o2.iso) }
            }
        }

        if (notifyDataSetChanged) filmStockAdapter.notifyDataSetChanged()
    }

    private fun resetFilters() {
        manufacturerFilterList.clear()
        isoFilterList.clear()
        filmTypeFilterList.clear()
        filmProcessFilterList.clear()
        addedByFilterMode = FILTER_MODE_ALL
        filterFilmStocks()
    }

    private fun filterFilmStocks() {
        // First filter the list based on manufacturer. No filtering is done if manufacturers is null.
        filteredFilmStocks = allFilmStocks.filter {
            // Filter based on manufacturers
            (manufacturerFilterList.contains(it.make) || manufacturerFilterList.isEmpty()) &&
            // Filter based on type
            (filmTypeFilterList.contains(it.type) || filmTypeFilterList.isEmpty()) &&
            // Filter based on process
            (filmProcessFilterList.contains(it.process) || filmProcessFilterList.isEmpty()) &&
            //Then filter based on filter mode.
            when (addedByFilterMode) {
                FILTER_MODE_PREADDED -> it.isPreadded
                FILTER_MODE_ADDED_BY_USER -> !it.isPreadded
                FILTER_MODE_ALL -> true
                else -> throw IllegalArgumentException("Illegal argument filterModeAddedBy: $addedByFilterMode")
            }
            // Finally filter based on ISO values.
            && (isoFilterList.contains(it.iso) || isoFilterList.isEmpty())
        }.toMutableList()
        sortFilmStocks()

        filmStockAdapter.filmStocks = filteredFilmStocks
        filmStockAdapter.notifyDataSetChanged()
    }

    // Possible ISO values are filtered based on currently selected manufacturers and filter mode.
    private val possibleIsoValues get() = allFilmStocks.filter {
            (manufacturerFilterList.contains(it.make) || manufacturerFilterList.isEmpty()) &&
            (filmTypeFilterList.contains(it.type) || filmTypeFilterList.isEmpty()) &&
            (filmProcessFilterList.contains(it.process) || filmProcessFilterList.isEmpty()) &&
            when (addedByFilterMode) {
                FILTER_MODE_PREADDED -> it.isPreadded
                FILTER_MODE_ADDED_BY_USER -> !it.isPreadded
                FILTER_MODE_ALL -> true
                else -> throw IllegalArgumentException("Illegal argument filterModeAddedBy: $addedByFilterMode")
            }
    }.map { it.iso }.distinct().sorted()

    private fun showManufacturerFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        // Get all filter items.
        val items = database.filmManufacturers.toTypedArray()
        // Create a boolean array of same size with selected items marked true.
        val checkedItems = items.map { manufacturerFilterList.contains(it) }.toBooleanArray()
        builder.setMultiChoiceItems(items, checkedItems) { _: DialogInterface?, which: Int, isChecked: Boolean ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _: DialogInterface?, _: Int ->
            // Get the indices of items that were marked true and their corresponding strings.
            manufacturerFilterList = checkedItems
                    .mapIndexed { index, selected -> index to selected }
                    .filter { it.second }.map { it.first }.map { items[it] }.toMutableList()
            filterFilmStocks()
        }
        builder.setNeutralButton(R.string.Reset) { _: DialogInterface?, _: Int ->
            manufacturerFilterList.clear()
            filterFilmStocks()
        }
        builder.create().show()
    }

    private fun showIsoValuesFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        // Get all filter items.
        val items = possibleIsoValues.toTypedArray()
        val itemStrings = items.map { it.toString() }.toTypedArray()
        // Create a boolean array of same size with selected items marked true.
        val checkedItems = items.map { isoFilterList.contains(it) }.toBooleanArray()
        builder.setMultiChoiceItems(itemStrings, checkedItems) { _: DialogInterface?, which: Int, isChecked: Boolean ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _: DialogInterface?, _: Int ->
            // Get the indices of items that were marked true and their corresponding int values.
            isoFilterList = checkedItems
                    .mapIndexed { index, selected -> index to selected }
                    .filter { it.second }.map { it.first }.map { items[it] }.toMutableList()
            filterFilmStocks()
        }
        builder.setNeutralButton(R.string.Reset) { _: DialogInterface?, _: Int ->
            isoFilterList.clear()
            filterFilmStocks()
        }
        builder.create().show()
    }

    private fun showFilmTypeFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        // Get all filter items.
        val items = resources.getStringArray(R.array.FilmTypes)
        // Create a boolean array of same size with selected items marked true.
        val checkedItems = items.indices.map { filmTypeFilterList.contains(it) }.toBooleanArray()
        builder.setMultiChoiceItems(items, checkedItems) { _: DialogInterface?, which: Int, isChecked: Boolean ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _: DialogInterface?, _: Int ->
            // Get the indices of items that were marked true.
            filmTypeFilterList = checkedItems
                    .mapIndexed { index, selected -> index to selected }
                    .filter { it.second }.map { it.first }.toMutableList()
            filterFilmStocks()
        }
        builder.setNeutralButton(R.string.Reset) { _: DialogInterface?, _: Int ->
            filmTypeFilterList.clear()
            filterFilmStocks()
        }
        builder.create().show()
    }

    private fun showFilmProcessFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        // Get all filter items.
        val items = resources.getStringArray(R.array.FilmProcesses)
        // Create a boolean array of same size with selected items marked true.
        val checkedItems = items.indices.map { filmProcessFilterList.contains(it) }.toBooleanArray()
        builder.setMultiChoiceItems(items, checkedItems) { _: DialogInterface?, which: Int, isChecked: Boolean ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _: DialogInterface?, _: Int ->
            // Get the indices of items that were marked true.
            filmProcessFilterList = checkedItems
                    .mapIndexed { index, selected -> index to selected }
                    .filter { it.second }.map { it.first }.toMutableList()
            filterFilmStocks()
        }
        builder.setNeutralButton(R.string.Reset) { _: DialogInterface?, _: Int ->
            filmProcessFilterList.clear()
            filterFilmStocks()
        }
        builder.create().show()
    }

    private fun showAddedByFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val checkedItem: Int
        val filterModeAddedBy = addedByFilterMode
        checkedItem = when (filterModeAddedBy) {
            FILTER_MODE_PREADDED -> 1
            FILTER_MODE_ADDED_BY_USER -> 2
            else -> 0
        }
        builder.setSingleChoiceItems(R.array.FilmStocksFilterMode, checkedItem) { dialog: DialogInterface, which: Int ->
            when (which) {
                0 -> addedByFilterMode = FILTER_MODE_ALL
                1 -> addedByFilterMode = FILTER_MODE_PREADDED
                2 -> addedByFilterMode = FILTER_MODE_ADDED_BY_USER
            }
            filterFilmStocks()
            dialog.dismiss()
        }
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.create().show()
    }

}