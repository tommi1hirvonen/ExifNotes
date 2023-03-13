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

package com.tommihirvonen.exifnotes.utilities

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.tommihirvonen.exifnotes.R
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import java.time.Duration
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

class GeocoderRequest(private val httpClient: HttpClient, val requestUrl: String) {
    /**
     * @return Pair object where first is latitude and longitude in decimal format and second is
     * the formatted address
     */
    suspend fun getResponse(): Pair<LatLng, String>? =
        try {
            withTimeout(Duration.ofSeconds(10).toMillis()) {
                withContext(Dispatchers.IO) {
                    val httpResponse = httpClient.get(requestUrl)
                    val data: String = httpResponse.body()
                    val format = Json { ignoreUnknownKeys = true }
                    val responseObject = format.decodeFromString<Response>(data)
                    val (lat, lng) = responseObject.results.first().geometry.location
                    val formattedAddress = responseObject.results.first().formattedAddress
                    val latNlg = LatLng(lat, lng)
                    latNlg to formattedAddress
                }
            }
        } catch (e: Exception) {
            null
        }

    @Serializable
    data class Response(val results: List<Result>) {
        @OptIn(ExperimentalSerializationApi::class)
        @Serializable
        data class Result(
            @JsonNames("formatted_address")
            val formattedAddress: String,
            val geometry: Geometry
        )
        @Serializable
        data class Geometry(val location: Location)
        @Serializable
        data class Location(val lat: Double, val lng: Double)
    }
}