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
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
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
import com.tommihirvonen.exifnotes.utilities.setIconsVisible
import com.tommihirvonen.exifnotes.viewmodels.FilmStockFilterSet
import com.tommihirvonen.exifnotes.viewmodels.FilmStocksViewModel

class FilmStocksFragment : Fragment(), MenuProvider {

    private val model by activityViewModels<FilmStocksViewModel>()
    private val gearFragment by lazy {
        requireParentFragment().requireParentFragment() as GearFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check if an existing film stock edit dialog is open after configuration change
        // and attach listener if so.
        val dialog = gearFragment.childFragmentManager.findFragmentByTag(FilmStockEditDialog.TAG)
        dialog?.setFragmentResultListener(FilmStockEditDialog.REQUEST_KEY) { _, bundle ->
            bundle.getParcelable<FilmStock>(ExtraKeys.FILM_STOCK)?.let(model::submitFilmStock)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding = FragmentFilmsBinding.inflate(inflater, container, false)
        binding.fabFilms.setOnClickListener { openFilmStockEditDialog(null) }

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
        val dialog = FilmStockEditDialog()
        val arguments = Bundle()
        if (filmStock != null) {
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditFilmStock))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
            arguments.putParcelable(ExtraKeys.FILM_STOCK, filmStock)
        } else {
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewFilmStock))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
        }
        dialog.arguments = arguments

        val transaction = gearFragment.childFragmentManager
            .beginTransaction()
            .addToBackStack(GearFragment.BACKSTACK_NAME)
        dialog.show(transaction, FilmStockEditDialog.TAG)
        dialog.setFragmentResultListener(FilmStockEditDialog.REQUEST_KEY) { _, bundle ->
            bundle.getParcelable<FilmStock>(ExtraKeys.FILM_STOCK)?.let(model::submitFilmStock)
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
        val filmTypes = resources.getStringArray(R.array.FilmTypes)
        val checkedItems = filmTypes.indices.map(model.filterSet.types::contains).toBooleanArray()

        builder.setMultiChoiceItems(filmTypes, checkedItems) { _, which, isChecked ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _, _ ->
            // Get the indices of items that were marked true.
            val selectedTypes = checkedItems.asIterable()
                .mapIndexedNotNull { index, selected -> if (selected) index else null }
            model.filterSet = model.filterSet.copy(types = selectedTypes)
        }
        builder.setNeutralButton(R.string.Reset) { _, _ ->
            model.filterSet = model.filterSet.copy(types = emptyList())
        }
        builder.create().show()
    }

    private fun showFilmProcessFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val filmProcesses = resources.getStringArray(R.array.FilmProcesses)
        val checkedItems = filmProcesses.indices.map(model.filterSet.processes::contains)
            .toBooleanArray()
        builder.setMultiChoiceItems(filmProcesses, checkedItems) { _, which, isChecked ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _, _ ->
            // Get the indices of items that were marked true.
            val selectedProcesses = checkedItems.asIterable()
                .mapIndexedNotNull { index, selected -> if (selected) index else null }
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