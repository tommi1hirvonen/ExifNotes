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

package com.tommihirvonen.exifnotes.dialogs

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.tommihirvonen.exifnotes.utilities.ExtraKeys

class EditFrameDialogCallback(val positiveButtonClicked: (Intent) -> Unit) : EditFrameDialog() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.title.positiveImageView.setOnClickListener {
            commitChanges()
            val intent = Intent()
            intent.putExtra(ExtraKeys.FRAME, frame)
            positiveButtonClicked(intent)
            dismiss()
        }
        super.onViewCreated(view, savedInstanceState)
    }

}