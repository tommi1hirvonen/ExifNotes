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

package com.tommihirvonen.exifnotes.core

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class ShutterSpeedParsingTest(private val input: String?, private val expectedValue: String?) {

    companion object {
        @JvmStatic
        @Parameters
        fun data(): List<Array<Any?>> = listOf(
            arrayOf("0", null),
            arrayOf("0\"", null),
            arrayOf("0.", null),
            arrayOf("0.0", null),
            arrayOf("1/250", "1/250"),
            arrayOf("1/0250", null),
            arrayOf("1/", null),
            arrayOf("2/", null),
            arrayOf("30\"", "30\""),
            arrayOf("15", "15\""),
            arrayOf("05", null),
            arrayOf("0.7", "0.7\""),
            arrayOf("0.7\"", "0.7\""),
            arrayOf("2.", null),
            arrayOf("123.", null),
            arrayOf("0.05", "0.05\""),
            arrayOf("01.5", null),
            arrayOf("1.001", "1.001\""),
            arrayOf("2\"", "2\""),
            arrayOf("2.5", "2.5\""),
            arrayOf("2.5\"", "2.5\""),
            arrayOf("12.5", "12.5\""),
            arrayOf("B", "B"),
            arrayOf(null, null),
            arrayOf("asd", null)
        )
    }

    @Test
    fun test() {
        val value = input?.toShutterSpeedOrNull()
        assertEquals(
            expectedValue,
            value
        )
    }
}