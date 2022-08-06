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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.ActivityMainBinding
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.viewmodels.FrameViewModel
import com.tommihirvonen.exifnotes.viewmodels.FrameViewModelFactory

class FramesFragment : Fragment() {

    private val roll by lazy<Roll> {
        requireArguments().getParcelable(ExtraKeys.ROLL)!!
    }

    /**
     * ViewModel shared by child fragments FramesListFragment and FramesMapFragment.
     */
    private val model by lazy {
        val factory = FrameViewModelFactory(requireActivity().application, roll)
        ViewModelProvider(this, factory)[FrameViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ActivityMainBinding.inflate(layoutInflater)
        val transitionName = requireArguments().getString(ExtraKeys.TRANSITION_NAME)
        binding.root.transitionName = transitionName
        model.frames.observe(viewLifecycleOwner) {
            // When the frames have been loaded from the database, start transition animation.
            startPostponedEnterTransition()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Postpone enter transition animation until frames have been loaded from the database.
        // Transition is started from the ViewModel callback in onCreateView().
        postponeEnterTransition()
        val fragment = FramesListFragment()
        val transitionName = requireArguments().getString(ExtraKeys.TRANSITION_NAME)
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TRANSITION_NAME, transitionName)
        fragment.arguments = arguments
        childFragmentManager
            .beginTransaction()
            .addToBackStack(null)
            .add(R.id.fragment_container, fragment)
            .commit()
    }

}