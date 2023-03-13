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

package com.tommihirvonen.exifnotes.geocoder

import android.content.Context
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.utilities.HttpClientAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocoderRequestBuilder(
    private val httpClientAdapter: HttpClientAdapter,
    private val apiKey: String) {

    @Inject
    constructor(@ApplicationContext context: Context, httpClientAdapter: HttpClientAdapter)
            : this(httpClientAdapter, context.resources.getString(R.string.google_maps_key))

    /**
     * @param coordinatesOrQuery Latitude and longitude coordinates in decimal format or a search query
     * @return new GeocoderRequest instance
     */
    fun fromQuery(coordinatesOrQuery: String): GeocoderRequest {
        val urlBuilder = URLBuilder(
            protocol = URLProtocol.HTTPS,
            host = "maps.google.com",
            pathSegments = listOf("maps", "api", "geocode", "json"),
            parameters = Parameters.build {
                append("address", coordinatesOrQuery)
                append("sensor", "false")
                append("key", apiKey)
            }
        )
        val url = urlBuilder.build().toString()
        return GeocoderRequest(httpClientAdapter.client, url)
    }

    fun fromPlaceId(placeId: String): GeocoderRequest {
        val urlBuilder = URLBuilder(
            protocol = URLProtocol.HTTPS,
            host = "maps.google.com",
            pathSegments = listOf("maps", "api", "geocode", "json"),
            parameters = Parameters.build {
                append("place_id", placeId)
                append("key", apiKey)
            }
        )
        val url = urlBuilder.build().toString()
        return GeocoderRequest(httpClientAdapter.client, url)
    }
}