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
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.ActivityGearBinding
import com.tommihirvonen.exifnotes.fragments.CamerasFragment
import com.tommihirvonen.exifnotes.fragments.FilmStocksFragment
import com.tommihirvonen.exifnotes.fragments.FiltersFragment
import com.tommihirvonen.exifnotes.fragments.LensesFragment
import java.lang.IllegalArgumentException

/**
 * GearActivity contains fragments for adding, editing and removing cameras, lenses and filters.
 */
class GearActivity : AppCompatActivity() {

    companion object {
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
     * ViewPager is responsible for changing the layout when the user swipes or clicks on a tab.
     */
    private lateinit var viewPager: ViewPager2

    /**
     * PagerAdapter adapts fragments to show in the ViewPager
     */
    private lateinit var pagerAdapter: PagerAdapter

    private lateinit var bottomNavigation: BottomNavigationView

    // Inflate the activity, set the UI, ViewPager and TabLayout.
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.enter_from_right, R.anim.hold)
        super.onCreate(savedInstanceState)

        val binding = ActivityGearBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.topAppBar.setNavigationOnClickListener { finish() }
        setSupportActionBar(binding.topAppBar)

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = binding.viewPager
        pagerAdapter = PagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.registerOnPageChangeCallback(onPageChangeCallback)

        bottomNavigation = binding.bottomNavigation
        bottomNavigation.setOnItemSelectedListener(navigationItemSelectedListener)

        //Get the index for the view which was last shown.

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

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.nothing, R.anim.exit_to_right)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(POSITION, viewPager.currentItem)
    }

    // Gets the displayed fragment's index from savedInstanceState
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewPager.currentItem = savedInstanceState.getInt(POSITION)
    }

    private val navigationItemSelectedListener = { item: MenuItem ->
        when (item.itemId) {
            R.id.page_cameras -> {
                viewPager.currentItem = POSITION_CAMERAS
                true
            }
            R.id.page_lenses -> {
                viewPager.currentItem = POSITION_LENSES
                true
            }
            R.id.page_filters -> {
                viewPager.currentItem = POSITION_FILTERS
                true
            }
            R.id.page_film_stocks -> {
                viewPager.currentItem = POSITION_FILMS
                true
            }
            else -> false
        }
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            when (position) {
                POSITION_CAMERAS -> {
                    bottomNavigation.menu.findItem(R.id.page_cameras).isChecked = true
                }
                POSITION_LENSES -> {
                    bottomNavigation.menu.findItem(R.id.page_lenses).isChecked = true
                }
                POSITION_FILTERS -> {
                    bottomNavigation.menu.findItem(R.id.page_filters).isChecked = true
                }
                POSITION_FILMS -> {
                    bottomNavigation.menu.findItem(R.id.page_film_stocks).isChecked = true
                }
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