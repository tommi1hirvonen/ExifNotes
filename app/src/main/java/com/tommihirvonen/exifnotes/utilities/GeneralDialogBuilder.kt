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
import android.view.View
import android.widget.TextView
import com.tommihirvonen.exifnotes.R

/**
 * Custom dialog builder to simplify creation of general purpose info dialogs.
 */
class GeneralDialogBuilder(context: Context) : AlertDialog.Builder(context) {

    override fun setTitle(title: CharSequence) = apply { super.setTitle(title) }

    override fun setMessage(message: CharSequence) = apply {
        // If there are URLs embedded in the message, modify them to become links.
        val spannableString = SpannableString(message)
        Linkify.addLinks(spannableString, Linkify.WEB_URLS)
        super.setMessage(spannableString)
    }

    override fun setView(view: View) = apply { super.setView(view) }

    override fun create(): AlertDialog {
        setNegativeButton(R.string.Close) { _: DialogInterface?, _: Int -> }
        val dialog = super.create()
        dialog.setOnShowListener {
            val textView = dialog.findViewById<TextView>(android.R.id.message)
            textView.textSize = 14f // Reduce message text size from default
            textView.movementMethod = LinkMovementMethod.getInstance() // Enable links
        }
        return dialog
    }
}