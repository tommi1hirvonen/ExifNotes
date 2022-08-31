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
import androidx.fragment.app.*
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

    companion object {
        const val TAG = "ROLL_EDIT_FRAGMENT"
        const val REQUEST_KEY = TAG
    }

    private val backStackName by lazy {
        requireArguments().getString(ExtraKeys.BACKSTACK_NAME)
    }
    private val fragmentContainerId by lazy {
        requireArguments().getInt(ExtraKeys.FRAGMENT_CONTAINER_ID)
    }

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

        val addFilmStockFragment = requireParentFragment().childFragmentManager
            .findFragmentByTag(FilmStockEditDialog.TAG)
        addFilmStockFragment
            ?.setFragmentResultListener(FilmStockEditDialog.REQUEST_KEY, onFilmStockAdded)

        val addCameraFragment = requireParentFragment().childFragmentManager
            .findFragmentByTag(CameraEditFragment.TAG)
        addCameraFragment?.setFragmentResultListener(CameraEditFragment.REQUEST_KEY, onCameraAdded)

        val selectFilmStockDialog = requireParentFragment().childFragmentManager
            .findFragmentByTag(SelectFilmStockDialog.TAG)
        selectFilmStockDialog
            ?.setFragmentResultListener(SelectFilmStockDialog.REQUEST_KEY, onFilmStockSelected)
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
            val transaction = requireParentFragment().childFragmentManager
                .beginTransaction()
                .addToBackStack(backStackName)
            dialog.show(transaction, FilmStockEditDialog.TAG)
            dialog.setFragmentResultListener(FilmStockEditDialog.REQUEST_KEY, onFilmStockAdded)
        }

        binding.filmStockLayout.setOnClickListener {
            val dialog = SelectFilmStockDialog()
            val transaction = requireParentFragment().childFragmentManager
                .beginTransaction().addToBackStack(backStackName)
            dialog.show(transaction, SelectFilmStockDialog.TAG)
            dialog.setFragmentResultListener(SelectFilmStockDialog.REQUEST_KEY, onFilmStockSelected)
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
            val sharedElement = binding.addCamera
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewCamera))
            arguments.putString(ExtraKeys.TRANSITION_NAME, sharedElement.transitionName)
            arguments.putString(ExtraKeys.BACKSTACK_NAME, backStackName)
            fragment.arguments = arguments

            requireParentFragment().childFragmentManager
                .beginTransaction()
                .setReorderingAllowed(true)
                .addSharedElement(sharedElement, sharedElement.transitionName)
                .replace(fragmentContainerId, fragment, CameraEditFragment.TAG)
                .addToBackStack(backStackName)
                .commit()

            fragment.setFragmentResultListener(CameraEditFragment.REQUEST_KEY, onCameraAdded)
        }

        // DATE
        if (roll.date == null) {
            model.observable.setLoadedOn(DateTime.fromCurrentTime())
        }

        DateTimeLayoutManager(
            requireActivity() as AppCompatActivity,
            binding.dateLoadedLayout,
            model.roll::date,
            model.observable::setLoadedOn)

        DateTimeLayoutManager(
            requireActivity() as AppCompatActivity,
            binding.dateUnloadedLayout,
            model.roll::unloaded,
            model.observable::setUnloadedOn)

        DateTimeLayoutManager(
            requireActivity() as AppCompatActivity,
            binding.dateDevelopedLayout,
            model.roll::developed,
            model.observable::setDevelopedOn)

        binding.positiveButton.setOnClickListener {
            if (model.validate()) {
                val bundle = Bundle().apply {
                    putParcelable(ExtraKeys.ROLL, model.roll)
                }
                setFragmentResult(REQUEST_KEY, bundle)
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

    private val onFilmStockSelected: (String, Bundle) -> Unit = { _, bundle ->
        bundle.getParcelable<FilmStock>(ExtraKeys.FILM_STOCK)?.let(model.observable::setFilmStock)
    }

    private val onFilmStockAdded: (String, Bundle) -> Unit = { _, bundle ->
        bundle.getParcelable<FilmStock>(ExtraKeys.FILM_STOCK)?.let(model::addFilmStock)
    }

    private val onCameraAdded: (String, Bundle) -> Unit = { _, bundle ->
        bundle.getParcelable<Camera>(ExtraKeys.CAMERA)?.let { camera ->
            rollsModel.addCamera(camera)
            model.observable.setCamera(camera)
        }
    }
}