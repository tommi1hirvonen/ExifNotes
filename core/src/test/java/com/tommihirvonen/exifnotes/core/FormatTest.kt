/*
 * Exif Notes
 * Copyright (C) 2026  Tommi Hirvonen
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

package com.tommihirvonen.exifnotes.core

import com.tommihirvonen.exifnotes.core.entities.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {

    @Test
    fun focalLengthIn35mmFormat_returnsSameFocalLengthFor35mm() {
        assertEquals(50, Format.MM35.focalLengthIn35mmFormat(50))
    }

    @Test
    fun focalLengthIn35mmFormat_convertsKnownMediumFormats() {
        assertEquals(50, Format.MediumFormat645.focalLengthIn35mmFormat(80))
        assertEquals(44, Format.MediumFormat66.focalLengthIn35mmFormat(80))
        assertEquals(39, Format.MediumFormat67.focalLengthIn35mmFormat(80))
        assertEquals(34, Format.MediumFormat69.focalLengthIn35mmFormat(80))
        assertEquals(28, Format.MediumFormat612.focalLengthIn35mmFormat(80))
        assertEquals(20, Format.MediumFormat617.focalLengthIn35mmFormat(80))
    }

    @Test
    fun focalLengthIn35mmFormat_convertsPanoramicAndSheetFormats() {
        assertEquals(28, Format.XPan.focalLengthIn35mmFormat(45))
        assertEquals(42, Format.Sheet4x5.focalLengthIn35mmFormat(150))
        assertEquals(41, Format.Sheet8x10.focalLengthIn35mmFormat(300))
    }

    @Test
    fun focalLengthIn35mmFormat_returnsNullWithoutEnoughInformation() {
        assertNull(Format.MediumFormat120.focalLengthIn35mmFormat(80))
        assertNull(Format.Sheet.focalLengthIn35mmFormat(80))
        assertNull(Format.MM35.focalLengthIn35mmFormat(0))
    }

    @Test
    fun existingFormatOrdinals_remainStable() {
        assertEquals(Format.MM35, Format.from(0))
        assertEquals(Format.MediumFormat120, Format.from(1))
        assertEquals(Format.APS110, Format.from(2))
        assertEquals(Format.Sheet, Format.from(3))
    }

    @Test
    fun selectableEntries_groupsRelatedFormats() {
        assertEquals(
            listOf(
                Format.MM35,
                Format.XPan,
                Format.MediumFormat120,
                Format.MediumFormat645,
                Format.MediumFormat66,
                Format.MediumFormat67,
                Format.MediumFormat69,
                Format.MediumFormat612,
                Format.MediumFormat617,
                Format.APS110,
                Format.Sheet,
                Format.Sheet4x5,
                Format.Sheet8x10
            ),
            Format.selectableEntries
        )
    }
}
