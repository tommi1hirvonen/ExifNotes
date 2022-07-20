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

package com.tommihirvonen.exifnotes.utilities

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import com.tommihirvonen.exifnotes.R

class GeneralDialogBuilder(private val context: Context, private val title: String, private val message: String) {
    fun create(): AlertDialog {
        val generalDialogBuilder = AlertDialog.Builder(context)
        generalDialogBuilder.setTitle(title)
        val spannableString = SpannableString(message)
        Linkify.addLinks(spannableString, Linkify.WEB_URLS)
        generalDialogBuilder.setMessage(spannableString)
        generalDialogBuilder.setNegativeButton(R.string.Close) { _: DialogInterface?, _: Int -> }
        val generalDialog = generalDialogBuilder.create()
        generalDialog.setOnShowListener {
            val textView = generalDialog.findViewById<TextView>(android.R.id.message)
            textView.textSize = 14f
            textView.movementMethod = LinkMovementMethod.getInstance()
        }
        return generalDialog
    }
}