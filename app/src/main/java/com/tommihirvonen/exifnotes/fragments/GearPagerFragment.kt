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

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.FragmentGearPagerBinding
import java.lang.IllegalArgumentException

class GearPagerFragment : Fragment() {

    companion object {
        const val TAG = "GEAR_PAGER_FRAGMENT"
        private const val POSITION_CAMERAS = 0
        private const val POSITION_LENSES = 1
        private const val POSITION_FILTERS = 2
        private const val POSITION_FILMS = 3
        private const val PAGE_COUNT = 4
    }

    lateinit var topAppBar: MaterialToolbar

    private lateinit var binding: FragmentGearPagerBinding

    /**
     * ViewPager is responsible for changing the layout when the user swipes or clicks on a tab.
     */
    private lateinit var viewPager: ViewPager2

    /**
     * PagerAdapter adapts fragments to show in the ViewPager
     */
    private lateinit var pagerAdapter: PagerAdapter

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentGearPagerBinding.inflate(inflater)

        topAppBar = binding.topAppBar
        topAppBar.setNavigationOnClickListener { requireActivity().onBackPressed() }

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = binding.viewPager
        pagerAdapter = PagerAdapter()
        viewPager.adapter = pagerAdapter
        viewPager.registerOnPageChangeCallback(onPageChangeCallback)

        bottomNavigation = binding.bottomNavigation
        bottomNavigation.setOnItemSelectedListener(navigationItemSelectedListener)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        // Start the transition once all views have been
        // measured and laid out
        (view.parent as? ViewGroup)?.doOnPreDraw {
            ObjectAnimator.ofFloat(binding.root, View.ALPHA, 0f, 1f).apply {
                duration = 250L
                start()
            }
            startPostponedEnterTransition()
        }
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
            super.onPageSelected(position)
        }
    }

    /**
     * Manages the fragments inside GearActivity.
     */
    private inner class PagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = PAGE_COUNT

        override fun createFragment(position: Int): Fragment = when (position) {
            POSITION_FILMS -> FilmStocksFragment()
            POSITION_FILTERS -> FiltersFragment()
            POSITION_LENSES -> LensesFragment()
            POSITION_CAMERAS -> CamerasFragment()
            else -> throw IllegalArgumentException("Illegal fragment position $position")
        }
    }

}