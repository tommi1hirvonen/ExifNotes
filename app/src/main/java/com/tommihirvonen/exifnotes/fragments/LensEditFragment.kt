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
import androidx.activity.addCallback
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import com.tommihirvonen.exifnotes.databinding.FragmentLensEditBinding
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.viewmodels.LensEditViewModel
import com.tommihirvonen.exifnotes.viewmodels.LensEditViewModelFactory

/**
 * Dialog to edit Lens's information
 */
class LensEditFragment : Fragment() {

    companion object {
        const val TAG = "LENS_EDIT_FRAGMENT"
        const val REQUEST_KEY = TAG
    }

    private val model by lazy {
        val fixedLens = requireArguments().getBoolean(ExtraKeys.FIXED_LENS)
        val lens = requireArguments().getParcelable<Lens>(ExtraKeys.LENS)?.copy() ?: Lens()
        val factory = LensEditViewModelFactory(requireActivity().application, fixedLens, lens.copy())
        ViewModelProvider(this, factory)[LensEditViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigateBack()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentLensEditBinding.inflate(inflater)
        binding.viewmodel = model.observable
        val transitionName = requireArguments().getString(ExtraKeys.TRANSITION_NAME)
        binding.root.transitionName = transitionName
        binding.topAppBar.title = requireArguments().getString(ExtraKeys.TITLE)
        binding.topAppBar.setNavigationOnClickListener { navigateBack() }
        binding.positiveButton.setOnClickListener {
            if (model.validate()) {
                val bundle = Bundle().apply {
                    putParcelable(ExtraKeys.LENS, model.lens)
                }
                setFragmentResult(REQUEST_KEY, bundle)
                navigateBack()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        // Start the transition once all views have been measured and laid out.
        (view.parent as ViewGroup).doOnPreDraw {
            startPostponedEnterTransition()
        }
    }

    private fun navigateBack() =
        requireParentFragment().childFragmentManager.popBackStack()
}