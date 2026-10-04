package com.tommihirvonen.exifnotes.core

import com.tommihirvonen.exifnotes.core.entities.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {

    @Test
    fun focalLengthIn35mmFormat_returnsSameFocalLengthFor35mm() {
        assertEquals(50, Format.MM35.focalLengthIn35mmFormat(50.0))
    }

    @Test
    fun focalLengthIn35mmFormat_convertsKnownMediumFormats() {
        assertEquals(50, Format.MediumFormat645.focalLengthIn35mmFormat(80.0))
        assertEquals(44, Format.MediumFormat66.focalLengthIn35mmFormat(80.0))
        assertEquals(19, Format.MediumFormat66.focalLengthIn35mmFormat(35.5))
        assertEquals(39, Format.MediumFormat67.focalLengthIn35mmFormat(80.0))
        assertEquals(34, Format.MediumFormat69.focalLengthIn35mmFormat(80.0))
        assertEquals(28, Format.MediumFormat612.focalLengthIn35mmFormat(80.0))
        assertEquals(20, Format.MediumFormat617.focalLengthIn35mmFormat(80.0))
    }

    @Test
    fun focalLengthIn35mmFormat_convertsPanoramicAndSheetFormats() {
        assertEquals(28, Format.XPan.focalLengthIn35mmFormat(45.0))
        assertEquals(42, Format.Sheet4x5.focalLengthIn35mmFormat(150.0))
        assertEquals(41, Format.Sheet8x10.focalLengthIn35mmFormat(300.0))
    }

    @Test
    fun focalLengthIn35mmFormat_returnsNullWithoutEnoughInformation() {
        assertNull(Format.MediumFormat120.focalLengthIn35mmFormat(80.0))
        assertNull(Format.Sheet.focalLengthIn35mmFormat(80.0))
        assertNull(Format.MM35.focalLengthIn35mmFormat(0.0))
    }

    @Test
    fun existingFormatOrdinals_remainStable() {
        assertEquals(Format.MM35, Format.from(0))
        assertEquals(Format.MediumFormat120, Format.from(1))
        assertEquals(Format.APS110, Format.from(2))
        assertEquals(Format.Sheet, Format.from(3))
    }
}
