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

import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.data.repositories.FrameRepository
import com.tommihirvonen.exifnotes.util.illegalCharsRemoved
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RollExportBuilder @Inject constructor(
    private val frameRepository: FrameRepository,
    private val csvBuilder: CsvBuilder,
    private val exifToolCommandsBuilder: ExifToolCommandsBuilder) {

    fun create(roll: Roll, options: List<RollExportOption>): List<RollExport> {
        val frames = frameRepository.getFrames(roll)
        return options.map { option ->
            val (filename, content) = contentMapping(roll, frames, option)
            RollExport(option, filename, content)
        }
    }

    private fun contentMapping(roll: Roll, frames: List<Frame>, option: RollExportOption): Pair<String, String> {
        val rollName = roll.name?.illegalCharsRemoved()
        return when (option) {
            RollExportOption.CSV -> {
                "${rollName}_csv.txt" to csvBuilder.create(roll, frames)
            }
            RollExportOption.EXIFTOOL -> {
                "${rollName}_ExifToolCmds.txt" to exifToolCommandsBuilder.create(roll, frames)
            }
            RollExportOption.JSON -> {
                "${rollName}.json" to JsonBuilder.create(roll, frames)
            }
        }
    }
}