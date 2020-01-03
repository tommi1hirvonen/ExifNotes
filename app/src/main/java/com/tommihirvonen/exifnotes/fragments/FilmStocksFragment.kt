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
    }

    private lateinit var database: FilmDbHelper
    private lateinit var filmStocks: MutableList<FilmStock>
    private var fragmentVisible = false
    private lateinit var filmStocksRecyclerView: RecyclerView
    private lateinit var filmStockAdapter: GearAdapter
    var sortMode = SORT_MODE_NAME
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        database = FilmDbHelper.getInstance(activity)
        filmStocks = database.allFilmStocks
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        @SuppressLint("InflateParams")
        val view = inflater.inflate(R.layout.fragment_films, null)

        val floatingActionButton: FloatingActionButton = view.findViewById(R.id.fab_films)
        floatingActionButton.setOnClickListener(this)
        // Also change the floating action button color. Use the darker secondaryColor for this.
        val secondaryColor = Utilities.getSecondaryUiColor(activity)
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(secondaryColor)

        filmStocksRecyclerView = view.findViewById(R.id.films_recycler_view)
        val layoutManager = LinearLayoutManager(activity)
        filmStocksRecyclerView.layoutManager = layoutManager
        filmStocksRecyclerView.addItemDecoration(
                DividerItemDecoration(
                        filmStocksRecyclerView.context, layoutManager.orientation
                )
        )
        filmStockAdapter = GearAdapter(activity, filmStocks)
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
            dialog.show(fragmentManager!!.beginTransaction(), null)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (fragmentVisible) {
            val position = item.order
            val filmStock = filmStocks[position]
            when (item.itemId) {
                GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_DELETE -> {
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
                        filmStocks.removeAt(position)
                        filmStockAdapter.notifyDataSetChanged()
                    }
                    builder.create().show()
                    return true
                }
                GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_EDIT -> {
                    val dialog = EditFilmStockDialog()
                    dialog.setTargetFragment(this, EDIT_FILM_STOCK)
                    val arguments = Bundle()
                    arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditFilmStock))
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
                    arguments.putParcelable(ExtraKeys.FILM_STOCK, filmStock)
                    dialog.arguments = arguments
                    dialog.show(fragmentManager!!.beginTransaction(), null)
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
                filmStocks.add(filmStock)
                sortFilmStocks()
                val position = filmStocks.indexOf(filmStock)
                filmStockAdapter.notifyItemInserted(position)
                filmStocksRecyclerView.scrollToPosition(position)
            }
            EDIT_FILM_STOCK -> if (resultCode == Activity.RESULT_OK) {
                val filmStock: FilmStock = data?.getParcelableExtra(ExtraKeys.FILM_STOCK) ?: return
                database.updateFilmStock(filmStock)
                val oldPosition = filmStocks.indexOf(filmStock)
                sortFilmStocks()
                val newPosition = filmStocks.indexOf(filmStock)
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
                filmStocks.sortWith(Comparator { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) })
            }
            SORT_MODE_ISO -> {
                sortMode = SORT_MODE_ISO
                filmStocks.sortWith(Comparator { o1, o2 -> o1.iso.compareTo(o2.iso) })
            }
        }

        if (notifyDataSetChanged) filmStockAdapter.notifyDataSetChanged()
    }

    fun filterFilmStocks(manufacturers: List<String?>) {
        filmStocks = database.allFilmStocks.filter { manufacturers.contains(it.make) }.toMutableList()
        sortFilmStocks()
        filmStockAdapter.setGearList(filmStocks)
        filmStockAdapter.notifyDataSetChanged()
    }

}