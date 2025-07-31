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

package com.tommihirvonen.exifnotes.di.export

import android.content.Context
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.LightSource
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.sortableDateTime
import com.tommihirvonen.exifnotes.screens.settings.SettingsViewModel
import com.tommihirvonen.exifnotes.util.exifToolLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExifToolCommandsBuilder @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private const val ignoreWarningsOption = "-m"
        private const val exiftoolCmd = "exiftool"
        private const val artistTag = "-Artist="
        private const val copyrightTag = "-Copyright="
        private const val cameraMakeTag = "-Make="
        private const val cameraModelTag = "-Model="
        private const val lensMakeTag = "-LensMake="
        private const val lensModelTag = "-LensModel="
        private const val lensTag = "-Lens="
        private const val dateTag = "-DateTime="
        private const val dateTimeOriginalTag = "-DateTimeOriginal="
        private const val shutterTag = "-ShutterSpeedValue="
        private const val exposureTimeTag = "-ExposureTime="
        private const val apertureTag = "-ApertureValue="
        private const val fNumberTag = "-FNumber="
        private const val commentTag = "-UserComment="
        private const val imageDescriptionTag = "-ImageDescription="
        private const val exposureCompTag = "-ExposureCompensation="
        private const val focalLengthTag = "-FocalLength="
        private const val isoTag = "-ISO="
        private const val serialNumberTag = "-SerialNumber="
        private const val lensSerialNumberTag = "-LensSerialNumber="
        private const val flashTag = "-Flash="
        private const val lightSourceTag = "-LightSource="
    }

    fun create(roll: Roll, frames: List<Frame>, lineSeparatorOs: LineSeparatorOs): String {
        val stringBuilder = StringBuilder()
        val artistName = prefs.getString(SettingsViewModel.KEY_ARTIST_NAME, "") ?: ""
        val copyrightInformation = prefs.getString(SettingsViewModel.KEY_COPYRIGHT_INFO, "") ?: ""
        val exiftoolPath = prefs.getString(SettingsViewModel.KEY_EXIFTOOL_PATH, "") ?: ""
        val picturesPath = prefs.getString(SettingsViewModel.KEY_PATH_TO_PICTURES, "") ?: ""
        val ignoreWarnings = prefs.getBoolean(SettingsViewModel.KEY_IGNORE_WARNINGS, false)
        var fileEnding = prefs.getString(SettingsViewModel.KEY_FILE_ENDING, ".jpg") ?: ".jpg"

        //Check that fileEnding begins with a dot.
        if (fileEnding.first() != '.') fileEnding = ".$fileEnding"

        val quote = "\""
        val space = " "
        val lineSep = lineSeparatorOs.lineSeparator
        val camera = roll.camera
        for (frame in frames) {
            //ExifTool path
            if (exiftoolPath.isNotEmpty()) stringBuilder.append(exiftoolPath)
            //ExifTool command
            stringBuilder.append(exiftoolCmd).append(space)
            //Ignore warnings
            if (ignoreWarnings) stringBuilder.append(ignoreWarningsOption).append(space)
            if (camera != null) {
                //CameraMakeTag
                stringBuilder.append(cameraMakeTag).append(quote).append(camera.make)
                    .append(quote).append(space)
                //CameraModelTag
                stringBuilder.append(cameraModelTag).append(quote).append(camera.model)
                    .append(quote).append(space)
                //SerialNumber
                val serialNumber = camera.serialNumber
                if (serialNumber?.isNotEmpty() == true) stringBuilder.append(serialNumberTag).append(quote).append(serialNumber)
                    .append(quote).append(space)
            }
            val lens = frame.lens
            if (lens != null) {
                //LensMakeTag
                stringBuilder.append(lensMakeTag).append(quote).append(lens.make).append(quote).append(space)
                //LensModelTag
                stringBuilder.append(lensModelTag).append(quote).append(lens.model).append(quote).append(space)
                //LensTag
                stringBuilder.append(lensTag).append(quote).append(lens.make).append(space)
                    .append(lens.model).append(quote).append(space)
                //LensSerialNumber
                val serialNumber = lens.serialNumber
                if (serialNumber?.isNotEmpty() == true) stringBuilder.append(lensSerialNumberTag).append(quote).append(serialNumber)
                    .append(quote).append(space)
            }
            val date = frame.date
            //DateTime
            stringBuilder.append(dateTag).append(quote).append(date.sortableDateTime
                .replace("-", ":")).append(quote).append(space)
            //DateTimeOriginal
            stringBuilder.append(dateTimeOriginalTag).append(quote).append(date.sortableDateTime
                .replace("-", ":")).append(quote).append(space)

            //ShutterSpeedValue & ExposureTime
            val shutter = frame.shutter
            if (shutter != null) {
                stringBuilder.append(shutterTag).append(quote).append(shutter
                    .replace("\"", "")).append(quote).append(space)
                stringBuilder.append(exposureTimeTag).append(quote).append(shutter
                    .replace("\"", "")).append(quote).append(space)
            }
            //ApertureValue & FNumber
            val aperture = frame.aperture
            if (aperture != null) {
                stringBuilder.append(apertureTag).append(quote).append(aperture).append(quote).append(space)
                stringBuilder.append(fNumberTag).append(quote).append(aperture).append(quote).append(space)
            }
            //UserComment & ImageDescription
            val note = frame.note
            if (note?.isNotEmpty() == true) {
                stringBuilder.append(commentTag).append(quote)
                    .append(
                        Normalizer.normalize(note, Normalizer.Form.NFC)
                        .replace("\"", "'")
                        .replace("\n", " ")).append(quote).append(space)
                stringBuilder.append(imageDescriptionTag).append(quote)
                    .append(
                        Normalizer.normalize(note, Normalizer.Form.NFC)
                        .replace("\"", "'")
                        .replace("\n", " ")).append(quote).append(space)
            }
            //GPSLatitude & GPSLongitude & GPSLatitudeRef & GPSLongitudeRef
            val location = frame.location
            if (location?.exifToolLocation != null) {
                stringBuilder.append(location.exifToolLocation)
            }
            //ExposureCompensation
            val exposureComp = frame.exposureComp
            if (exposureComp != null) stringBuilder.append(exposureCompTag)
                .append(quote).append(exposureComp).append(quote).append(space)
            //FocalLength
            val focalLength = frame.focalLength
            if (focalLength > 0) stringBuilder.append(focalLengthTag).append(quote)
                .append(focalLength).append(quote).append(space)
            //ISO
            val iso = roll.iso
            if (iso > 0) stringBuilder.append(isoTag).append(quote).append(iso)
                .append(quote).append(space)
            // Flash
            if (frame.flashUsed) stringBuilder.append(flashTag).append(quote)
                .append("Fired").append(quote).append(space)
            // Light source
            val lightSource: String = when (frame.lightSource) {
                LightSource.Daylight -> "Daylight"
                LightSource.Sunny -> "Fine Weather"
                LightSource.Cloudy -> "Cloudy"
                LightSource.Shade -> "Shade"
                LightSource.Fluorescent -> "Fluorescent"
                LightSource.Tungsten -> "Tungsten"
                LightSource.Flash -> "Flash"
                LightSource.Unknown -> "Unknown" // Unknown
            }
            stringBuilder.append(lightSourceTag).append(quote)
                .append(lightSource).append(quote).append(space)

            //Artist
            if (artistName.isNotEmpty()) stringBuilder.append(artistTag).append(quote)
                .append(artistName).append(quote).append(space)
            //Copyright
            if (copyrightInformation.isNotEmpty()) stringBuilder.append(copyrightTag).append(quote)
                .append(copyrightInformation).append(quote).append(space)
            //Path to pictures
            if (picturesPath.contains(" ") || fileEnding.contains(" ")) stringBuilder.append(quote)
            if (picturesPath.isNotEmpty()) stringBuilder.append(picturesPath)
            //File ending
            stringBuilder.append("*_").append(frame.count).append(fileEnding)
            if (picturesPath.contains(" ") || fileEnding.contains(" ")) stringBuilder.append(quote)
            //Double new line
            stringBuilder.append(lineSep).append(lineSep)
        }
        return stringBuilder.toString()
    }
}