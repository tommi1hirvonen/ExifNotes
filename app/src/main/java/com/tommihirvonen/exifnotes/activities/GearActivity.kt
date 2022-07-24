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

package com.tommihirvonen.exifnotes.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
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

        // Manually handling the back navigation button press enables custom transition animations.
        addMenuProvider(object : MenuProvider{
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == android.R.id.home) {
                    onBackPressed()
                    return true
                }
                return false
            }
        })
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