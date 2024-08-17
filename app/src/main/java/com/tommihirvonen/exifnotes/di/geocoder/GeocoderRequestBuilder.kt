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

package com.tommihirvonen.exifnotes.di.geocoder

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.decimalString
import com.tommihirvonen.exifnotes.di.http.HttpClientAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocoderRequestBuilder(
    private val httpClient: HttpClient,
    private val apiKey: String
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        httpClientAdapter: HttpClientAdapter
    ) : this(
        httpClientAdapter.client,
        context.resources.getString(R.string.google_maps_key)
    )

    fun fromQuery(query: String) = ParametersBuilder().apply {
        append("address", query)
        append("sensor", "false")
    }.buildRequest()

    fun fromLatLng(latLng: LatLng) = ParametersBuilder().apply {
        append("address", latLng.decimalString)
        append("sensor", "false")
    }.buildRequest()

    fun fromPlaceId(placeId: String) = ParametersBuilder().apply {
        append("place_id", placeId)
    }.buildRequest()

    private fun ParametersBuilder.buildRequest(): GeocoderRequest {
        append("key", apiKey)
        val url = URLBuilder(
            protocol = URLProtocol.HTTPS,
            host = "maps.google.com",
            pathSegments = listOf("maps", "api", "geocode", "json"),
            parameters = build()
        ).build().toString()
        return GeocoderRequest(httpClient, url)
    }
}