package com.tommihirvonen.exifnotes.di.location

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.di.http.HttpClientAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeZoneService(private val httpClient: HttpClient, private val apiKey: String) {
    private val json = Json { ignoreUnknownKeys = true }

    @Inject
    constructor(@ApplicationContext context: Context, httpClientAdapter: HttpClientAdapter) : this(
        httpClientAdapter.client,
        context.getString(R.string.google_maps_key)
    )

    /** Resolve the zone only; offsets must be calculated from the frame's local date/time. */
    suspend fun getTimeZone(location: LatLng, date: LocalDateTime): String? {
        if (apiKey.isBlank() || !location.latitude.isFinite() || !location.longitude.isFinite()) {
            return null
        }
        return try {
            withTimeoutOrNull(10_000) {
                withContext(Dispatchers.IO) {
                    val response = httpClient.get("https://maps.googleapis.com/maps/api/timezone/json") {
                        parameter("location", "${location.latitude},${location.longitude}")
                        // The API needs a timestamp, but its raw/DST offsets are deliberately
                        // ignored: the frame is a local wall time, not a known UTC instant.
                        parameter("timestamp", date.toEpochSecond(ZoneOffset.UTC))
                        parameter("key", apiKey)
                    }
                    if (response.status.value !in 200..299) return@withContext null
                    val result = json.decodeFromString<Response>(response.bodyAsText())
                    if (result.status != "OK") return@withContext null
                    result.timeZoneId?.let { ZoneId.of(it).id }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    @Serializable
    private data class Response(val status: String, val timeZoneId: String? = null)
}
