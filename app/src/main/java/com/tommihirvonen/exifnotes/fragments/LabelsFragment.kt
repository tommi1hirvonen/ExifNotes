/*
 * Exif Notes
 * Copyright (C) 2024  Tommi Hirvonen
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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tommihirvonen.exifnotes.adapters.LabelAdapter
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.databinding.FragmentLabelsBinding
import com.tommihirvonen.exifnotes.viewmodels.LabelsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LabelsFragment : Fragment() {

    private val model: LabelsViewModel by viewModels {
        defaultViewModelProviderFactory
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        val binding = FragmentLabelsBinding.inflate(inflater)
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val layoutManager = LinearLayoutManager(activity)
        binding.labelsRecyclerView.layoutManager = layoutManager

        val labelAdapter = LabelAdapter(requireContext(),onLabelClickListener,
            onLabelDeleteClickListener)
        binding.labelsRecyclerView.adapter = labelAdapter

        model.labels.observe(viewLifecycleOwner) { labels ->
            labelAdapter.labels = labels
            binding.noAddedLabels.visibility = if (labels.isEmpty()) View.VISIBLE else View.GONE
            labelAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    private val onLabelClickListener = { label: Label, view: View ->

    }

    private val onLabelDeleteClickListener = { label: Label, view: View ->

    }
}