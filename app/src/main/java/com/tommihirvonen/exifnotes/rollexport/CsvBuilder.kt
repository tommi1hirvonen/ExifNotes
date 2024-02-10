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

package com.tommihirvonen.exifnotes.rollexport

import android.content.Context
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.entities.Frame
import com.tommihirvonen.exifnotes.entities.Roll
import com.tommihirvonen.exifnotes.utilities.readableCoordinates
import com.tommihirvonen.exifnotes.utilities.sortableDateTime
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.text.StringEscapeUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvBuilder @Inject constructor(@ApplicationContext private val context: Context) {
    fun create(roll: Roll, frames: List<Frame>): String {
        val camera = roll.camera
        val filmStock = roll.filmStock
        val separator = ","
        val stringBuilder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_CSV)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val artistName = prefs.getString("ArtistName", "")
        val copyrightInformation = prefs.getString("CopyrightInformation", "")

        //Roll and camera information
        stringBuilder.append("Roll name: ").append(roll.name).append("\n")
        stringBuilder.append("Loaded on: ").append(roll.date.sortableDateTime).append("\n")
        stringBuilder.append("Unloaded on: ").append(roll.unloaded?.sortableDateTime ?: "").append("\n")
        stringBuilder.append("Developed on: ").append(roll.developed?.sortableDateTime ?: "").append("\n")
        stringBuilder.append("Film stock: ").append(filmStock?.name ?: "").append("\n")
        stringBuilder.append("ISO: ").append(roll.iso.toString()).append("\n")
        stringBuilder.append("Format: ").append(roll.format.description(context)).append("\n")
        stringBuilder.append("Push/pull: ").append(roll.pushPull ?: "").append("\n")
        stringBuilder.append("Camera: ").append(camera?.name ?: "").append("\n")
        stringBuilder.append("Serial number: ").append(camera?.serialNumber ?: "").append("\n")
        stringBuilder.append("Notes: ").append(roll.note ?: "").append("\n")
        stringBuilder.append("Artist name: ").append(artistName).append("\n")
        stringBuilder.append("Copyright: ").append(copyrightInformation).append("\n")

        //Column headers
        stringBuilder
            .append("Frame Count").append(separator)
            .append("Date").append(separator)
            .append("Lens").append(separator)
            .append("Lens serial number").append(separator)
            .append("Shutter").append(separator)
            .append("Aperture").append(separator)
            .append("Focal length").append(separator)
            .append("Exposure compensation").append(separator)
            .append("Notes").append(separator)
            .append("No of exposures").append(separator)
            .append("Filter").append(separator)
            .append("Location").append(separator)
            .append("Address").append(separator)
            .append("Flash").append(separator)
            .append("Light source")
            .append("\n")
        for (frame in frames) {
            stringBuilder.append(frame.count.toString()).append(separator)
                .append(frame.date.sortableDateTime).append(separator)
                .escape(frame.lens?.name ?: "").append(separator)
                .escape(frame.lens?.serialNumber ?: "").append(separator)
                .append(frame.shutter ?: "").append(separator)

            frame.aperture?.let { stringBuilder.append("f").append(it) }
            stringBuilder.append(separator)

            if (frame.focalLength > 0) stringBuilder.append(frame.focalLength.toString())
            stringBuilder.append(separator)

            frame.exposureComp?.let { if (it.length > 1) stringBuilder.append(it) }
            stringBuilder.append(separator)

            stringBuilder.escape(frame.note ?: "").append(separator)
                .append(frame.noOfExposures.toString()).append(separator)
                .escape(frame.filters.joinToString(separator = "|") { it.name }).append(separator)
                .escape(frame.location?.readableCoordinates ?: "").append(separator)
                .escape(frame.formattedAddress ?: "").append(separator)
                .append(frame.flashUsed.toString()).append(separator)

            try {
                stringBuilder.escape(frame.lightSource.description(context))
            } catch (e: ArrayIndexOutOfBoundsException) {
                stringBuilder.append("Error")
            }
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }
}