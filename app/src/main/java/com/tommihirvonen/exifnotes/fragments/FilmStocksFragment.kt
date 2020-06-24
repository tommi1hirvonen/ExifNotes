package com.tommihirvonen.exifnotes.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.GearAdapter
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.dialogs.EditFilmStockDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper
import com.tommihirvonen.exifnotes.utilities.Utilities
import java.util.*

class FilmStocksFragment : Fragment(), View.OnClickListener {

    companion object {
        private const val ADD_FILM_STOCK = 1
        private const val EDIT_FILM_STOCK = 2
        const val SORT_MODE_NAME = 1
        const val SORT_MODE_ISO = 2
        const val FILTER_MODE_ALL = 0
        const val FILTER_MODE_ADDED_BY_USER = 1
        const val FILTER_MODE_PREADDED = 2
    }

    private lateinit var database: FilmDbHelper
    private lateinit var allFilmStocks: MutableList<FilmStock>
    private lateinit var filteredFilmStocks: MutableList<FilmStock>
    private var fragmentVisible = false
    private lateinit var filmStocksRecyclerView: RecyclerView
    private lateinit var filmStockAdapter: GearAdapter
    var sortMode = SORT_MODE_NAME
        private set
    private var manufacturerFilterList = emptyList<String>().toMutableList()
    private var isoFilterList = emptyList<Int>().toMutableList()
    private var filmTypeFilterList = emptyList<Int>().toMutableList()
    private var filmProcessFilterList = emptyList<Int>().toMutableList()
    private var addedByFilterMode = FILTER_MODE_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        database = FilmDbHelper.getInstance(activity)
        allFilmStocks = database.allFilmStocks
        filteredFilmStocks = allFilmStocks.toMutableList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        @SuppressLint("InflateParams")
        val view = inflater.inflate(R.layout.fragment_films, null)

        val floatingActionButton: FloatingActionButton = view.findViewById(R.id.fab_films)
        floatingActionButton.setOnClickListener(this)
        // Also change the floating action button color. Use the darker secondaryColor for this.
        val secondaryColor = Utilities.getSecondaryUiColor(requireActivity())
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(secondaryColor)

        filmStocksRecyclerView = view.findViewById(R.id.films_recycler_view)
        val layoutManager = LinearLayoutManager(activity)
        filmStocksRecyclerView.layoutManager = layoutManager
        filmStocksRecyclerView.addItemDecoration(
                DividerItemDecoration(
                        filmStocksRecyclerView.context, layoutManager.orientation
                )
        )
        filmStockAdapter = GearAdapter(requireActivity(), filteredFilmStocks)
        filmStocksRecyclerView.adapter = filmStockAdapter
        filmStockAdapter.notifyDataSetChanged()

        return view
    }

    override fun onResume() {
        fragmentVisible = true
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        fragmentVisible = false
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab_films) {
            val dialog = EditFilmStockDialog()
            dialog.setTargetFragment(this, ADD_FILM_STOCK)
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewFilmStock))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), null)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (fragmentVisible) {
            val position = item.order
            val filmStock = filteredFilmStocks[position]
            when (item.itemId) {
                GearAdapter.MENU_ITEM_DELETE -> {
                    val builder = AlertDialog.Builder(activity)
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
                    return true
                }
                GearAdapter.MENU_ITEM_EDIT -> {
                    val dialog = EditFilmStockDialog()
                    dialog.setTargetFragment(this, EDIT_FILM_STOCK)
                    val arguments = Bundle()
                    arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditFilmStock))
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
                    arguments.putParcelable(ExtraKeys.FILM_STOCK, filmStock)
                    dialog.arguments = arguments
                    dialog.show(parentFragmentManager.beginTransaction(), null)
                    return true
                }
            }
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ADD_FILM_STOCK -> if (resultCode == Activity.RESULT_OK) {
                val filmStock: FilmStock = data?.getParcelableExtra(ExtraKeys.FILM_STOCK) ?: return
                val rowId = database.addFilmStock(filmStock)
                filmStock.id = rowId
                // Add the new film stock to both lists.
                filteredFilmStocks.add(filmStock) // The new film stock is shown immediately regardless of filters.
                allFilmStocks.add(filmStock) // The new film stock is shown after new filters are applied and they match.
                sortFilmStocks()
                val position = filteredFilmStocks.indexOf(filmStock)
                filmStockAdapter.notifyItemInserted(position)
                filmStocksRecyclerView.scrollToPosition(position)
            }
            EDIT_FILM_STOCK -> if (resultCode == Activity.RESULT_OK) {
                val filmStock: FilmStock = data?.getParcelableExtra(ExtraKeys.FILM_STOCK) ?: return
                database.updateFilmStock(filmStock)
                val oldPosition = filteredFilmStocks.indexOf(filmStock)
                sortFilmStocks()
                val newPosition = filteredFilmStocks.indexOf(filmStock)
                filmStockAdapter.notifyItemChanged(oldPosition)
                filmStockAdapter.notifyItemMoved(oldPosition, newPosition)
            }
        }
    }

    private fun sortFilmStocks() {
        setSortMode(sortMode, false)
    }

    fun setSortMode(sortMode_: Int, notifyDataSetChanged: Boolean) {
        when (sortMode_) {
            SORT_MODE_NAME -> {
                sortMode = SORT_MODE_NAME
                filteredFilmStocks.sortWith(Comparator { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) })
            }
            SORT_MODE_ISO -> {
                sortMode = SORT_MODE_ISO
                filteredFilmStocks.sortWith(Comparator { o1, o2 -> o1.iso.compareTo(o2.iso) })
            }
        }

        if (notifyDataSetChanged) filmStockAdapter.notifyDataSetChanged()
    }

    fun resetFilters() {
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

        filmStockAdapter.gearList = filteredFilmStocks
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

    fun showManufacturerFilterDialog() {
        val builder = AlertDialog.Builder(requireActivity())
        // Get all filter items.
        val items = FilmDbHelper.getInstance(requireActivity()).allFilmManufacturers.toTypedArray()
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

    fun showIsoValuesFilterDialog() {
        val builder = AlertDialog.Builder(requireActivity())
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

    fun showFilmTypeFilterDialog() {
        val builder = AlertDialog.Builder(requireActivity())
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

    fun showFilmProcessFilterDialog() {
        val builder = AlertDialog.Builder(requireActivity())
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

    fun showAddedByFilterDialog() {
        val builder = AlertDialog.Builder(requireActivity())
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