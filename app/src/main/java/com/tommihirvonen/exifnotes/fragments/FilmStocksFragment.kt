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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.FilmStockAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentFilmsBinding
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.datastructures.FilmStockFilterMode
import com.tommihirvonen.exifnotes.datastructures.FilmStockSortMode
import com.tommihirvonen.exifnotes.dialogs.FilmStockEditDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.database
import com.tommihirvonen.exifnotes.viewmodels.FilmStockFilterSet
import com.tommihirvonen.exifnotes.viewmodels.FilmStockViewModel

class FilmStocksFragment : Fragment(), MenuProvider {

    private val model by viewModels<FilmStockViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding = FragmentFilmsBinding.inflate(inflater, container, false)
        binding.fabFilms.setOnClickListener(onFabClickListener)

        binding.filmsRecyclerView.layoutManager = LinearLayoutManager(activity)
        val adapter = FilmStockAdapter(requireActivity(), onFilmStockClickListener)
        binding.filmsRecyclerView.adapter = adapter

        val pagerFragment = requireParentFragment() as GearPagerFragment
        pagerFragment.topAppBar.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        model.filmStocks.observe(viewLifecycleOwner) { filmStocks ->
            adapter.filmStocks = filmStocks
            adapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_film_stocks_fragment, menu)
        menu.findItem(R.id.sort_mode_film_stock_name).isChecked = true
    }

    override fun onPrepareMenu(menu: Menu) {
        when (model.sortMode.value) {
            FilmStockSortMode.NAME -> menu.findItem(R.id.sort_mode_film_stock_name).isChecked = true
            FilmStockSortMode.ISO -> menu.findItem(R.id.sort_mode_film_stock_iso).isChecked = true
            null -> {}
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.sort_mode_film_stock_name -> {
                model.setSortMode(FilmStockSortMode.NAME)
                menuItem.isChecked = true
                return true
            }
            R.id.sort_mode_film_stock_iso -> {
                model.setSortMode(FilmStockSortMode.ISO)
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
                model.filterSet = FilmStockFilterSet()
                return true
            }
        }
        return false
    }

    private val onFabClickListener = { _: View ->
        val dialog = FilmStockEditDialog()
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewFilmStock))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), null)
        dialog.setFragmentResultListener("EditFilmStockDialog") { _, bundle ->
            val filmStock: FilmStock = bundle.getParcelable(ExtraKeys.FILM_STOCK)
                ?: return@setFragmentResultListener
            model.addFilmStock(filmStock)
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
        val dialog = FilmStockEditDialog()
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditFilmStock))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
        arguments.putParcelable(ExtraKeys.FILM_STOCK, filmStock)
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), null)
        dialog.setFragmentResultListener("EditFilmStockDialog") { _, bundle ->
            val filmStock1: FilmStock = bundle.getParcelable(ExtraKeys.FILM_STOCK)
                ?: return@setFragmentResultListener
            model.updateFilmStock(filmStock1)
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
            model.deleteFilmStock(filmStock)
        }
        builder.create().show()
    }

    private fun showManufacturerFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        // Get all filter items.
        val items = model.filteredManufacturers.toTypedArray()
        // Create a boolean array of same size with selected items marked true.
        val checkedItems = items
            .map { model.filterSet.manufacturers.contains(it) }
            .toBooleanArray()
        builder.setMultiChoiceItems(items, checkedItems) { _: DialogInterface?, which: Int, isChecked: Boolean ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _: DialogInterface?, _: Int ->
            // Get the indices of items that were marked true and their corresponding strings.
            val selectedManufacturers = checkedItems
                    .mapIndexed { index, selected -> index to selected }
                    .filter { it.second }.map { it.first }.map { items[it] }
            model.filterSet = model.filterSet.copy(manufacturers = selectedManufacturers)
        }
        builder.setNeutralButton(R.string.Reset) { _: DialogInterface?, _: Int ->
            model.filterSet = model.filterSet.copy(manufacturers = emptyList())
        }
        builder.create().show()
    }

    private fun showIsoValuesFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        // Get all filter items.
        val items = model.filteredIsoValues
        val itemStrings = items.map { it.toString() }.toTypedArray()
        // Create a boolean array of same size with selected items marked true.
        val checkedItems = items
            .map { model.filterSet.isoValues.contains(it) }
            .toBooleanArray()
        builder.setMultiChoiceItems(itemStrings, checkedItems) { _: DialogInterface?, which: Int, isChecked: Boolean ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _: DialogInterface?, _: Int ->
            // Get the indices of items that were marked true and their corresponding int values.
            val selectedIsoValues = checkedItems
                    .mapIndexed { index, selected -> index to selected }
                    .filter { it.second }.map { it.first }.map { items[it] }
            model.filterSet = model.filterSet.copy(isoValues = selectedIsoValues)
        }
        builder.setNeutralButton(R.string.Reset) { _: DialogInterface?, _: Int ->
            model.filterSet = model.filterSet.copy(isoValues = emptyList())
        }
        builder.create().show()
    }

    private fun showFilmTypeFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        // Get all filter items.
        val items = resources.getStringArray(R.array.FilmTypes)
        // Create a boolean array of same size with selected items marked true.
        val checkedItems = items.indices
            .map { model.filterSet.types.contains(it) }
            .toBooleanArray()
        builder.setMultiChoiceItems(items, checkedItems) { _: DialogInterface?, which: Int, isChecked: Boolean ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _: DialogInterface?, _: Int ->
            // Get the indices of items that were marked true.
            val selectedTypes = checkedItems
                    .mapIndexed { index, selected -> index to selected }
                    .filter { it.second }.map { it.first }
            model.filterSet = model.filterSet.copy(types = selectedTypes)
        }
        builder.setNeutralButton(R.string.Reset) { _: DialogInterface?, _: Int ->
            model.filterSet = model.filterSet.copy(types = emptyList())
        }
        builder.create().show()
    }

    private fun showFilmProcessFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        // Get all filter items.
        val items = resources.getStringArray(R.array.FilmProcesses)
        // Create a boolean array of same size with selected items marked true.
        val checkedItems = items.indices
            .map { model.filterSet.processes.contains(it) }
            .toBooleanArray()
        builder.setMultiChoiceItems(items, checkedItems) { _: DialogInterface?, which: Int, isChecked: Boolean ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _: DialogInterface?, _: Int ->
            // Get the indices of items that were marked true.
            val selectedProcesses = checkedItems
                    .mapIndexed { index, selected -> index to selected }
                    .filter { it.second }.map { it.first }
            model.filterSet = model.filterSet.copy(processes = selectedProcesses)
        }
        builder.setNeutralButton(R.string.Reset) { _: DialogInterface?, _: Int ->
            model.filterSet = model.filterSet.copy(processes = emptyList())
        }
        builder.create().show()
    }

    private fun showAddedByFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val checkedItem: Int
        val filterModeAddedBy = model.filterSet.filterMode
        checkedItem = filterModeAddedBy.ordinal
        builder.setSingleChoiceItems(R.array.FilmStocksFilterMode, checkedItem) { dialog: DialogInterface, which: Int ->
            val selectedFilterMode = FilmStockFilterMode.from(which)
            model.filterSet = model.filterSet.copy(filterMode = selectedFilterMode)
            dialog.dismiss()
        }
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.create().show()
    }
}