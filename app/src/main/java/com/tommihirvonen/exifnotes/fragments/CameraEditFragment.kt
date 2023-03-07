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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.ChangeBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.ChangeTransform
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.FragmentCameraEditBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.CameraEditViewModel
import com.tommihirvonen.exifnotes.viewmodels.CameraEditViewModelFactory

/**
 * Dialog to edit Camera's information
 */
class CameraEditFragment : Fragment() {

    private val arguments by navArgs<CameraEditFragmentArgs>()

    private val editModel by viewModels<CameraEditViewModel> {
        val camera = arguments.camera?.copy() ?: Camera()
        CameraEditViewModelFactory(requireActivity().application, camera)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeTransform())
            .addTransition(ChangeImageTransform())
            .addTransition(Fade())
            .setCommonInterpolator(FastOutSlowInInterpolator())
            .apply { duration = 250L }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentCameraEditBinding.inflate(inflater)
        binding.root.transitionName = arguments.transitionName
        binding.topAppBar.title = arguments.title
        binding.topAppBar.setNavigationOnClickListener { navigateBack() }
        binding.viewmodel = editModel.observable

        // FIXED LENS
        binding.fixedLensHelp.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setMessage(R.string.FixedLensHelp)
                setPositiveButton(R.string.Close) { _: DialogInterface, _: Int -> }
            }.create().show()
        }
        binding.fixedLensLayout.setOnClickListener {
            val title = resources.getString(R.string.SetFixedLens)
            val action = CameraEditFragmentDirections
                .fixedLensEditAction(editModel.camera.lens, true, title, null)
            findNavController().navigate(action)
        }
        binding.positiveButton.setOnClickListener {
            if (editModel.validate()) {
                setNavigationResult(editModel.camera, ExtraKeys.CAMERA)
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
        observeThenClearNavigationResult(ExtraKeys.LENS, editModel.observable::setLens)
    }

    private fun navigateBack() = findNavController().navigateUp()

}