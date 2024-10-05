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

import com.google.android.gms.maps.model.LatLng
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import java.time.Duration

class GeocoderRequest(private val httpClient: HttpClient, val requestUrl: String) {
    /**
     * @return Pair object where first is latitude and longitude in decimal format and second is
     * the formatted address
     */
    suspend fun getResponse(): GeocoderResponse =
        try {
            withTimeout(Duration.ofSeconds(10).toMillis()) {
                withContext(Dispatchers.IO) {
                    val httpResponse = httpClient.get(requestUrl)
                    val data: String = httpResponse.body()
                    val format = Json { ignoreUnknownKeys = true }
                    val responseObject = format.decodeFromString<Response>(data)
                    if (responseObject.results.isEmpty()) {
                        return@withContext GeocoderResponse.NotFound
                    }
                    val (lat, lng) = responseObject.results.first().geometry.location
                    val formattedAddress = responseObject.results.first().formattedAddress
                    val latNlg = LatLng(lat, lng)
                    GeocoderResponse.Success(latNlg, formattedAddress)
                }
            }
        } catch (_: TimeoutCancellationException) {
            GeocoderResponse.Timeout
        } catch (_: Exception) {
            GeocoderResponse.Error
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