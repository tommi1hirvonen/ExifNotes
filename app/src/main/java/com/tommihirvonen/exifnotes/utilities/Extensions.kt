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

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.view.Gravity
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import com.tommihirvonen.exifnotes.datastructures.Gear
import java.io.*

val Context.packageInfo: PackageInfo? get() {
    try {
        return packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return null
}

/**
 * Removes potential illegal characters from a string to make it a valid file name.
 */
fun String.illegalCharsRemoved(): String = replace("[|\\\\?*<\":>/]".toRegex(), "_")

/**
 * Remove all files in a directory. Subdirectories are skipped.
 */
fun File.purgeDirectory() = this.listFiles()?.filterNot { it.isDirectory }?.forEach { it.delete() }

/**
 * Method to build a custom AlertDialog title TextView. This way we can imitate
 * the default AlertDialog title and its padding.
 */
fun Fragment.buildCustomDialogTitleTextView(titleText: String?): TextView {
    val context = requireContext()
    val titleTextView = TextView(context)
    TextViewCompat.setTextAppearance(titleTextView, android.R.style.TextAppearance_DeviceDefault_DialogWindowTitle)
    val dpi = context.resources.displayMetrics.density
    titleTextView.setPadding((20 * dpi).toInt(), (20 * dpi).toInt(), (20 * dpi).toInt(), (10 * dpi).toInt())
    titleTextView.text = titleText ?: ""
    titleTextView.gravity = Gravity.LEFT
    return titleTextView
}

fun File.makeDirsIfNotExists() { if (!isDirectory) mkdirs() }

fun List<Gear>.toStringList(): String =
    if (this.isEmpty()) "" else this.joinToString(separator = "\n-", prefix = "\n-") { it.name }