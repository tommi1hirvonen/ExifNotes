package com.tommihirvonen.exifnotes.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.fragments.CamerasFragment
import com.tommihirvonen.exifnotes.fragments.FilmStocksFragment
import com.tommihirvonen.exifnotes.fragments.FiltersFragment
import com.tommihirvonen.exifnotes.fragments.LensesFragment
import com.tommihirvonen.exifnotes.utilities.isAppThemeDark
import com.tommihirvonen.exifnotes.utilities.primaryUiColor
import com.tommihirvonen.exifnotes.utilities.setUiColor
import java.lang.IllegalArgumentException

/**
 * GearActivity contains fragments for adding, editing and removing cameras, lenses and filters.
 */
class GearActivity : AppCompatActivity() {

    companion object {
        /**
         * Tag for the index of the current view to store in SharedPreferences
         */
        private const val GEAR_ACTIVITY_SAVED_VIEW = "GEAR_ACTIVITY_SAVED_VIEW"

        /**
         * Tag for the index of the current view to store while the activity is paused
         */
        private const val POSITION = "POSITION"
        private const val POSITION_CAMERAS = 0
        private const val POSITION_LENSES = 1
        private const val POSITION_FILTERS = 2
        private const val POSITION_FILMS = 3
        private const val PAGE_COUNT = 4
    }

    /**
     * Android TabLayout member
     */
    private lateinit var tabLayout: TabLayout

    /**
     * ViewPager is responsible for changing the layout when the user swipes or clicks on a tab.
     */
    private lateinit var viewPager: ViewPager2

    /**
     * PagerAdapter adapts fragments to show in the ViewPager
     */
    private lateinit var pagerAdapter: PagerAdapter

    // Inflate the activity, set the UI, ViewPager and TabLayout.
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.enter_from_right, R.anim.hold)
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        if (isAppThemeDark) setTheme(R.style.AppTheme_Dark)
        super.onCreate(savedInstanceState)
        setUiColor(true)
        supportActionBar?.setTitle(R.string.Gear)
        setContentView(R.layout.activity_gear)

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = findViewById(R.id.viewpager)
        pagerAdapter = PagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // Give the TabLayout the ViewPager
        tabLayout = findViewById(R.id.sliding_tabs)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // Set tab title
            tab.text = when (position) {
                POSITION_FILMS -> applicationContext.resources.getString(R.string.FilmStocks)
                POSITION_FILTERS -> applicationContext.resources.getString(R.string.Filters)
                POSITION_LENSES -> applicationContext.resources.getString(R.string.Lenses)
                POSITION_CAMERAS -> applicationContext.resources.getString(R.string.Cameras)
                else -> "Error"
            }
        }.attach()
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.white))
        tabLayout.setBackgroundColor(primaryUiColor)

        //Get the index for the view which was last shown.
        viewPager.currentItem = prefs.getInt(GEAR_ACTIVITY_SAVED_VIEW, POSITION_CAMERAS)
    }

    /*
     * Saves the index of the current fragment so that when returning to this activity,
     * it will resume from the same fragment.
     */
    public override fun onStop() {
        super.onStop()
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        prefs.edit().putInt(GEAR_ACTIVITY_SAVED_VIEW, viewPager.currentItem).apply()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.nothing, R.anim.exit_to_right)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_gear_actvity, menu)
        menu.findItem(R.id.sort_mode_film_stock_name).isChecked = true
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val filmStocksFragment = pagerAdapter.fragments[POSITION_FILMS] as FilmStocksFragment?
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.sort_mode_film_stock_name -> {
                filmStocksFragment?.setSortMode(FilmStocksFragment.SORT_MODE_NAME, true)
                item.isChecked = true
                return true
            }
            R.id.sort_mode_film_stock_iso -> {
                filmStocksFragment?.setSortMode(FilmStocksFragment.SORT_MODE_ISO, true)
                item.isChecked = true
                return true
            }
            R.id.filter_mode_film_manufacturer -> {
                filmStocksFragment?.showManufacturerFilterDialog()
                return true
            }
            R.id.filter_mode_added_by -> {
                filmStocksFragment?.showAddedByFilterDialog()
                return true
            }
            R.id.filter_mode_film_iso -> {
                filmStocksFragment?.showIsoValuesFilterDialog()
                return true
            }
            R.id.filter_mode_film_type -> {
                filmStocksFragment?.showFilmTypeFilterDialog()
                return true
            }
            R.id.filter_mode_film_process -> {
                filmStocksFragment?.showFilmProcessFilterDialog()
                return true
            }
            R.id.filter_mode_reset -> {
                filmStocksFragment?.resetFilters()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // If a fragment other than FilmStocksFragment is being shown, disable the film stock
        // filtering and sorting options. This is also done so that the filter and sort methods
        // of FilmStocksFragment aren't called when its late init members are not initialized because
        // its onCreate() hasn't yet been called.
        if (viewPager.currentItem != POSITION_FILMS) {
            menu.findItem(R.id.sort_mode_film_stock_name).isEnabled = false
            menu.findItem(R.id.sort_mode_film_stock_iso).isEnabled = false
            menu.findItem(R.id.filter_mode_film_manufacturer).isEnabled = false
            menu.findItem(R.id.filter_mode_added_by).isEnabled = false
            menu.findItem(R.id.filter_mode_film_iso).isEnabled = false
            menu.findItem(R.id.filter_mode_film_type).isEnabled = false
            menu.findItem(R.id.filter_mode_film_process).isEnabled = false
            menu.findItem(R.id.filter_mode_reset).isEnabled = false
        } else {
            // When the options menu is opened, set the correct items to be preselected.
            val fragment = pagerAdapter.fragments[POSITION_FILMS] as FilmStocksFragment?
            when (fragment?.sortMode) {
                FilmStocksFragment.SORT_MODE_NAME -> menu.findItem(R.id.sort_mode_film_stock_name).isChecked = true
                FilmStocksFragment.SORT_MODE_ISO -> menu.findItem(R.id.sort_mode_film_stock_iso).isChecked = true
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(POSITION, tabLayout.selectedTabPosition)
    }

    // Gets the displayed fragment's index from savedInstanceState
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewPager.currentItem = savedInstanceState.getInt(POSITION)
    }

    /**
     * When new mountable combinations are added to the database, other fragments' views
     * have to be updated too to display the changes. This method updates the necessary fragment(s).
     */
    fun updateFragments() {
        when (viewPager.currentItem) {
            POSITION_CAMERAS -> {
                (pagerAdapter.fragments[POSITION_LENSES] as LensesFragment).updateFragment()
                (pagerAdapter.fragments[POSITION_FILTERS] as FiltersFragment).updateFragment()
            }
            POSITION_LENSES -> {
                (pagerAdapter.fragments[POSITION_CAMERAS] as CamerasFragment).updateFragment()
                (pagerAdapter.fragments[POSITION_FILTERS] as FiltersFragment).updateFragment()
            }
            POSITION_FILTERS -> {
                (pagerAdapter.fragments[POSITION_LENSES] as LensesFragment).updateFragment()
            }
        }
    }

    /**
     * Manages the fragments inside GearActivity.
     */
    private inner class PagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

        val fragments: MutableMap<Int, Fragment> = mutableMapOf()

        override fun getItemCount(): Int = PAGE_COUNT

        override fun createFragment(position: Int): Fragment {
            val fragment = when (position) {
                POSITION_FILMS -> FilmStocksFragment()
                POSITION_FILTERS -> FiltersFragment()
                POSITION_LENSES -> LensesFragment()
                POSITION_CAMERAS -> CamerasFragment()
                else -> throw IllegalArgumentException("Illegal fragment position $position")
            }
            fragments[position] = fragment
            return fragment
        }

    }

}