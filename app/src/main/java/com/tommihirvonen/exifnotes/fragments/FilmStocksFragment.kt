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

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.FilmStockAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentFilmsBinding
import com.tommihirvonen.exifnotes.entities.FilmProcess
import com.tommihirvonen.exifnotes.entities.FilmStock
import com.tommihirvonen.exifnotes.entities.FilmStockFilterMode
import com.tommihirvonen.exifnotes.entities.FilmStockSortMode
import com.tommihirvonen.exifnotes.entities.FilmType
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.data.database
import com.tommihirvonen.exifnotes.utilities.observeThenClearNavigationResult
import com.tommihirvonen.exifnotes.utilities.setIconsVisible
import com.tommihirvonen.exifnotes.viewmodels.FilmStockFilterSet
import com.tommihirvonen.exifnotes.viewmodels.FilmStocksViewModel

class FilmStocksFragment : Fragment(), MenuProvider {

    private val model by viewModels<FilmStocksViewModel>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding = FragmentFilmsBinding.inflate(inflater, container, false)
        binding.fabFilms.setOnClickListener { openFilmStockEditDialog(null) }

        binding.filmsRecyclerView.layoutManager = LinearLayoutManager(activity)
        val adapter = FilmStockAdapter(requireActivity(), onFilmStockClickListener)
        binding.filmsRecyclerView.adapter = adapter

        val pagerFragment = requireParentFragment() as GearFragment
        pagerFragment.topAppBar.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        model.filmStocks.observe(viewLifecycleOwner) { filmStocks ->
            adapter.filmStocks = filmStocks
            adapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navBackStackEntry = findNavController().getBackStackEntry(R.id.gear_dest)
        navBackStackEntry.observeThenClearNavigationResult<FilmStock>(
            viewLifecycleOwner, ExtraKeys.FILM_STOCK) { filmStock ->
            filmStock?.let(model::submitFilmStock)
        }
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

    private val onFilmStockClickListener = { filmStock: FilmStock, view: View ->
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_film_stock_item, popup.menu)
        popup.setIconsVisible(requireContext())
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_item_edit -> openFilmStockEditDialog(filmStock)
                R.id.menu_item_delete -> confirmDeleteFilmStock(filmStock)
            }
            true
        }
        popup.show()
    }

    private fun openFilmStockEditDialog(filmStock: FilmStock?) {
        val (title, positiveButtonText) = if (filmStock == null) {
            resources.getString(R.string.AddNewFilmStock) to resources.getString(R.string.Add)
        } else {
            resources.getString(R.string.EditFilmStock) to resources.getString(R.string.OK)
        }
        val action = GearFragmentDirections.filmStockEditAction(filmStock, title,positiveButtonText)
        findNavController().navigate(action)
    }

    private fun confirmDeleteFilmStock(filmStock: FilmStock) {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(
            resources.getString(R.string.DeleteFilmStock) + " " + filmStock.name
        )
        if (database.isFilmStockBeingUsed(filmStock)) {
            builder.setMessage(R.string.FilmStockIsInUseConfirmation)
        }
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        builder.setPositiveButton(R.string.OK) { _, _ ->
            model.deleteFilmStock(filmStock)
        }
        builder.create().show()
    }

    private fun showManufacturerFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val manufacturers = model.filteredManufacturers.toTypedArray()
        val checkedItems = manufacturers.map(model.filterSet.manufacturers::contains).toBooleanArray()

        builder.setMultiChoiceItems(manufacturers, checkedItems) { _, which, isChecked ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _, _ ->
            // Get the indices of items that were marked true and their corresponding strings.
            val selectedManufacturers = checkedItems.zip(manufacturers)
                .mapNotNull { (selected, manufacturer) -> if (selected) manufacturer else null }
            model.filterSet = model.filterSet.copy(manufacturers = selectedManufacturers)
        }
        builder.setNeutralButton(R.string.Reset) { _, _ ->
            model.filterSet = model.filterSet.copy(manufacturers = emptyList())
        }
        builder.create().show()
    }

    private fun showIsoValuesFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val isoValues = model.filteredIsoValues
        val itemStrings = isoValues.map(Int::toString).toTypedArray()
        val checkedItems = isoValues.map(model.filterSet.isoValues::contains).toBooleanArray()

        builder.setMultiChoiceItems(itemStrings, checkedItems) { _, which, isChecked ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _, _ ->
            // Get the indices of items that were marked true and their corresponding int values.
            val selectedIsoValues = checkedItems.zip(isoValues)
                .mapNotNull { (selected, isoValue) -> if (selected) isoValue else null }
            model.filterSet = model.filterSet.copy(isoValues = selectedIsoValues)
        }
        builder.setNeutralButton(R.string.Reset) { _, _ ->
            model.filterSet = model.filterSet.copy(isoValues = emptyList())
        }
        builder.create().show()
    }

    private fun showFilmTypeFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val filmTypes = FilmType.entries.toTypedArray()
        val filmTypeDescriptions = filmTypes.map { it.description(requireContext()) }.toTypedArray()
        val checkedItems = filmTypes.map(model.filterSet.types::contains).toBooleanArray()

        builder.setMultiChoiceItems(filmTypeDescriptions, checkedItems) { _, which, isChecked ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _, _ ->
            // Get the indices of items that were marked true.
            val selectedTypes = checkedItems
                .zip(filmTypes)
                .filter(Pair<Boolean, FilmType>::first)
                .map(Pair<Boolean, FilmType>::second)
            model.filterSet = model.filterSet.copy(types = selectedTypes)
        }
        builder.setNeutralButton(R.string.Reset) { _, _ ->
            model.filterSet = model.filterSet.copy(types = emptyList())
        }
        builder.create().show()
    }

    private fun showFilmProcessFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val filmProcesses = FilmProcess.entries.toTypedArray()
        val filmProcessDescriptions = filmProcesses.map { it.description(requireContext()) }.toTypedArray()
        val checkedItems = filmProcesses.map(model.filterSet.processes::contains).toBooleanArray()
        builder.setMultiChoiceItems(filmProcessDescriptions, checkedItems) { _, which, isChecked ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _, _ ->
            // Get the indices of items that were marked true.
            val selectedProcesses = checkedItems
                .zip(filmProcesses)
                .filter(Pair<Boolean, FilmProcess>::first)
                .map(Pair<Boolean, FilmProcess>::second)
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
        builder.setSingleChoiceItems(R.array.FilmStocksFilterMode, checkedItem) { dialog, which ->
            val selectedFilterMode = FilmStockFilterMode.from(which)
            model.filterSet = model.filterSet.copy(filterMode = selectedFilterMode)
            dialog.dismiss()
        }
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        builder.create().show()
    }
}