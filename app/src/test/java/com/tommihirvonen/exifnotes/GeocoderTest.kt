/*
 * Exif Notes
 * Copyright (C) 2023  Tommi Hirvonen
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

package com.tommihirvonen.exifnotes

import com.tommihirvonen.exifnotes.utilities.GeocoderRequest
import com.tommihirvonen.exifnotes.utilities.GeocoderRequestBuilder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class GeocoderTest {
    @Test
    fun queryUrlTest() {
        val key = "dummy"
        val request = GeocoderRequestBuilder(key).fromQuery("Helsinki")
        assertEquals(
            "https://maps.google.com/maps/api/geocode/json?address=Helsinki&sensor=false&key=dummy",
            request.requestUrl
        )
        println(request.requestUrl)
    }

    @Test
    fun responseParseTest() {
        val format = Json { ignoreUnknownKeys = true }
        val data = format.decodeFromString<GeocoderRequest.Response>(response)
        println(data)
    }
}

private const val response = "{\n" +
        "    \"results\" : [\n" +
        "       {\n" +
        "          \"address_components\" : [\n" +
        "             {\n" +
        "                \"long_name\" : \"Helsinki\",\n" +
        "                \"short_name\" : \"HKI\",\n" +
        "                \"types\" : [ \"locality\", \"political\" ]\n" +
        "             },\n" +
        "             {\n" +
        "                \"long_name\" : \"Helsinki\",\n" +
        "                \"short_name\" : \"Helsinki\",\n" +
        "                \"types\" : [ \"administrative_area_level_3\", \"political\" ]\n" +
        "             },\n" +
        "             {\n" +
        "                \"long_name\" : \"Helsinki\",\n" +
        "                \"short_name\" : \"Helsinki\",\n" +
        "                \"types\" : [ \"administrative_area_level_2\", \"political\" ]\n" +
        "             },\n" +
        "             {\n" +
        "                \"long_name\" : \"Uusimaa\",\n" +
        "                \"short_name\" : \"Uusimaa\",\n" +
        "                \"types\" : [ \"administrative_area_level_1\", \"political\" ]\n" +
        "             },\n" +
        "             {\n" +
        "                \"long_name\" : \"Suomi\",\n" +
        "                \"short_name\" : \"FI\",\n" +
        "                \"types\" : [ \"country\", \"political\" ]\n" +
        "             },\n" +
        "             {\n" +
        "                \"long_name\" : \"00190\",\n" +
        "                \"short_name\" : \"00190\",\n" +
        "                \"types\" : [ \"postal_code\" ]\n" +
        "             }\n" +
        "          ],\n" +
        "          \"formatted_address\" : \"00190 Helsinki, Suomi\",\n" +
        "          \"geometry\" : {\n" +
        "             \"location\" : {\n" +
        "                \"lat\" : 60.14539999999999,\n" +
        "                \"lng\" : 24.9881401\n" +
        "             },\n" +
        "             \"location_type\" : \"GEOMETRIC_CENTER\",\n" +
        "             \"viewport\" : {\n" +
        "                \"northeast\" : {\n" +
        "                   \"lat\" : 60.14674883029151,\n" +
        "                   \"lng\" : 24.9891909802915\n" +
        "                },\n" +
        "                \"southwest\" : {\n" +
        "                   \"lat\" : 60.1440508697085,\n" +
        "                   \"lng\" : 24.9864930197085\n" +
        "                }\n" +
        "             }\n" +
        "          },\n" +
        "          \"partial_match\" : true,\n" +
        "          \"place_id\" : \"ChIJhTiFuI0LkkYRFwfoahbvmAw\",\n" +
        "          \"plus_code\" : {\n" +
        "             \"compound_code\" : \"4XWQ+57 Helsinki, Suomi\",\n" +
        "             \"global_code\" : \"9GG64XWQ+57\"\n" +
        "          },\n" +
        "          \"types\" : [ \"establishment\", \"point_of_interest\", \"tourist_attraction\" ]\n" +
        "       }\n" +
        "    ],\n" +
        "    \"status\" : \"OK\"\n" +
        " }"