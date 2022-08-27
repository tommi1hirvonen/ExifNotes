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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import androidx.transition.*
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.FragmentRollEditBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.DateTime
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.dialogs.FilmStockEditDialog
import com.tommihirvonen.exifnotes.dialogs.SelectFilmStockDialog
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.RollEditViewModel
import com.tommihirvonen.exifnotes.viewmodels.RollEditViewModelFactory
import com.tommihirvonen.exifnotes.viewmodels.RollsViewModel

/**
 * Dialog to edit Roll's information
 */
class RollEditFragment : Fragment() {

    private val rollsModel by activityViewModels<RollsViewModel>()
    private val roll by lazy { requireArguments().getParcelable(ExtraKeys.ROLL) ?: Roll() }
    private val model by lazy {
        val factory = RollEditViewModelFactory(requireActivity().application, roll.copy())
        ViewModelProvider(this, factory)[RollEditViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireParentFragment().childFragmentManager.popBackStack()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentRollEditBinding.inflate(inflater, container, false)

        val transitionName = requireArguments().getString(ExtraKeys.TRANSITION_NAME)
        binding.root.transitionName = transitionName
        binding.topAppBar.title = requireArguments().getString(ExtraKeys.TITLE)
        binding.topAppBar.setNavigationOnClickListener {
            requireParentFragment().childFragmentManager.popBackStack()
        }

        binding.viewmodel = model.observable

        rollsModel.cameras.observe(viewLifecycleOwner) { cameras ->
            model.cameras = cameras
        }

        binding.addFilmStock.setOnClickListener {
            binding.noteEditText.clearFocus()
            binding.nameEditText.clearFocus()
            val dialog = FilmStockEditDialog()
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewFilmStock))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), null)
            dialog.setFragmentResultListener("EditFilmStockDialog") { _, bundle ->
                val filmStock: FilmStock = bundle.getParcelable(ExtraKeys.FILM_STOCK)
                    ?: return@setFragmentResultListener
                database.addFilmStock(filmStock)
                setFilmStock(filmStock)
            }
        }

        binding.filmStockLayout.setOnClickListener {
            val dialog = SelectFilmStockDialog()
            dialog.show(parentFragmentManager.beginTransaction(), null)
            dialog.setFragmentResultListener("SelectFilmStockDialog") { _, bundle ->
                val filmStock: FilmStock = bundle.getParcelable(ExtraKeys.FILM_STOCK)
                    ?: return@setFragmentResultListener
                setFilmStock(filmStock)
            }
        }

        binding.addCamera.setOnClickListener {
            val sharedElementTransition = TransitionSet()
                .addTransition(ChangeBounds())
                .addTransition(ChangeTransform())
                .addTransition(ChangeImageTransform())
                .addTransition(Fade())
                .setCommonInterpolator(FastOutSlowInInterpolator())
                .apply { duration = 250L }
            val fragment = CameraEditFragment().apply {
                sharedElementEnterTransition = sharedElementTransition
            }
            val arguments = Bundle()
            val sharedElement = binding.addCamera
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewCamera))
            arguments.putString(ExtraKeys.TRANSITION_NAME, sharedElement.transitionName)
            fragment.arguments = arguments

            requireParentFragment().childFragmentManager
                .beginTransaction()
                .setReorderingAllowed(true)
                .addSharedElement(sharedElement, sharedElement.transitionName)
                .replace(R.id.rolls_fragment_container, fragment)
                .addToBackStack(null)
                .commit()

            fragment.setFragmentResultListener("CameraEditFragment") { _, bundle ->
                val camera: Camera = bundle.getParcelable(ExtraKeys.CAMERA)
                    ?: return@setFragmentResultListener
                rollsModel.addCamera(camera)
                model.observable.setCamera(camera)
            }
        }

        // DATE
        if (roll.date == null) {
            model.observable.setLoadedOn(DateTime.fromCurrentTime())
        }

        DateTimeLayoutManager(
            requireActivity() as AppCompatActivity,
            binding.dateLoadedLayout,
            { model.roll.date },
            model.observable::setLoadedOn)

        DateTimeLayoutManager(
            requireActivity() as AppCompatActivity,
            binding.dateUnloadedLayout,
            { model.roll.unloaded },
            model.observable::setUnloadedOn)

        DateTimeLayoutManager(
            requireActivity() as AppCompatActivity,
            binding.dateDevelopedLayout,
            { model.roll.developed },
            model.observable::setDevelopedOn)

        binding.positiveButton.setOnClickListener {
            if (model.validate()) {
                roll.name = model.roll.name
                roll.note = model.roll.note
                roll.camera = model.roll.camera
                roll.date = model.roll.date
                roll.unloaded = model.roll.unloaded
                roll.developed = model.roll.developed
                roll.iso = model.roll.iso
                roll.format = model.roll.format
                roll.pushPull = model.roll.pushPull
                roll.filmStock = model.roll.filmStock

                val bundle = Bundle()
                bundle.putParcelable(ExtraKeys.ROLL, roll)
                setFragmentResult("EditRollDialog", bundle)
                requireActivity().onBackPressed()
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

    private fun setFilmStock(filmStock: FilmStock) {
        model.observable.setFilmStock(filmStock)
        // If the film stock ISO is defined, set the ISO
        if (filmStock.iso != 0) {
            model.observable.setIso(filmStock.iso.toString())
        }
    }

}