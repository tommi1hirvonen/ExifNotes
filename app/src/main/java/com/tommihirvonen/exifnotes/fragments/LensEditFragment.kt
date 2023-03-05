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

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.ChangeBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.ChangeTransform
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.DialogCustomApertureValuesBinding
import com.tommihirvonen.exifnotes.databinding.FragmentLensEditBinding
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.setCommonInterpolator
import com.tommihirvonen.exifnotes.utilities.setNavigationResult
import com.tommihirvonen.exifnotes.viewmodels.LensEditViewModel
import com.tommihirvonen.exifnotes.viewmodels.LensEditViewModelFactory

/**
 * Dialog to edit Lens's information
 */
class LensEditFragment : Fragment() {

    private val arguments by navArgs<LensEditFragmentArgs>()

    private val model by lazy {
        val fixedLens = arguments.fixedLens
        val lens = arguments.lens?.copy() ?: Lens()
        val factory = LensEditViewModelFactory(requireActivity().application, fixedLens, lens.copy())
        ViewModelProvider(this, factory)[LensEditViewModel::class.java]
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
        val binding = FragmentLensEditBinding.inflate(inflater)
        binding.viewmodel = model.observable
        binding.root.transitionName = arguments.transitionName
        binding.topAppBar.title = arguments.title
        binding.topAppBar.setNavigationOnClickListener { navigateBack() }
        binding.positiveButton.setOnClickListener {
            if (model.validate()) {
                setNavigationResult(model.lens, ExtraKeys.LENS)
                navigateBack()
            }
        }
        binding.customApertureValuesHelp.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setMessage(R.string.CustomApertureValuesHelp)
                setPositiveButton(R.string.Close) { _: DialogInterface, _: Int -> }
            }.create().show()
        }
        binding.addCustomApertureValue.setOnClickListener {
            val view = requireActivity().layoutInflater.inflate(R.layout.dialog_single_decimal_edit_text, null)
            val editText = view.findViewById<EditText>(R.id.edit_text)
            MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle(R.string.EnterCustomerApertureValue)
                .setPositiveButton(R.string.OK) { _, _ ->
                    val value = editText.text.toString().toFloatOrNull() ?: return@setPositiveButton
                    val values = model.lens.customApertureValues.plus(value)
                    model.observable.setCustomApertureValues(values)
                }
                .setNegativeButton(R.string.Cancel) { _, _ -> /*Do nothing*/ }
                .create()
                .show()
            editText.requestFocus()
        }
        binding.customApertureValuesButton.setOnClickListener {
            val mutableValues = model.lens.customApertureValues.toMutableList()
            val dialogBinding = DialogCustomApertureValuesBinding.inflate(layoutInflater)
            val adapter = CustomApertureValueAdapter(requireContext(), mutableValues)
            dialogBinding.listViewCustomApertureValues.adapter = adapter
            MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.OK) { _, _ -> model.observable.setCustomApertureValues(mutableValues) }
                .setNegativeButton(R.string.Cancel) { _, _ -> }
                .create()
                .show()
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

    private fun navigateBack() = findNavController().navigateUp()

    private class CustomApertureValueAdapter(context: Context,
        private val values: MutableList<Float>)
        : ArrayAdapter<Float>(context, android.R.layout.simple_list_item_1, values) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val holder: ViewHolder
            val view = if (convertView != null) {
                holder = convertView.tag as ViewHolder
                convertView
            } else {
                val tempView = LayoutInflater.from(context).inflate(R.layout.item_custom_aperture_value, parent, false)
                holder = ViewHolder().apply {
                    textView = tempView.findViewById(R.id.text_view)
                    deleteButton = tempView.findViewById(R.id.delete)
                }
                tempView.tag = holder
                tempView
            }
            val value = values[position]
            holder.textView.text = value.toString()
            holder.deleteButton.setOnClickListener {
                values.remove(value)
                notifyDataSetChanged()
            }
            return view
        }

        private class ViewHolder {
            lateinit var textView: TextView
            lateinit var deleteButton: Button
        }
    }
}