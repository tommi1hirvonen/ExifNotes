package com.tommihirvonen.exifnotes.utilities

import android.content.Context
import android.net.Uri
import com.tommihirvonen.exifnotes.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class Geocoder(val context: Context) {

    private val apiKey = context.resources.getString(R.string.google_maps_key)

    /**
     * @param coordinatesOrQuery Latitude and longitude coordinates in decimal format or a search query
     * @return Pair object where first is latitude and longitude in decimal format and second is
     * the formatted address
     */
    suspend fun getData(coordinatesOrQuery: String): Pair<String, String> {
        val queryUrl = Uri.Builder() // Requests must be made over SSL.
            .scheme("https")
            .authority("maps.google.com")
            .appendPath("maps")
            .appendPath("api")
            .appendPath("geocode")
            .appendPath("json")
            // Use address parameter for both the coordinates and search string.
            .appendQueryParameter("address", coordinatesOrQuery)
            .appendQueryParameter("sensor", "false")
            // Use key parameter to pass the API key credentials.
            .appendQueryParameter("key", apiKey)
            .build().toString()

        // Here it is safe to suppress the blocking method warning,
        // since we are doing the blocking in the IO thread.
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            val connection = try {
                URL(queryUrl).openConnection() as HttpsURLConnection
            } catch (e: Exception) {
                return@withContext Pair("", "")
            }

            try {
                connection.readTimeout = 15000
                connection.connectTimeout = 15000
                connection.requestMethod = "GET"
                connection.doInput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.doOutput = true

                val data = if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    return@withContext Pair("", "")
                }

                val jsonObject = JSONObject(data)
                val lng = (jsonObject["results"] as JSONArray).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lng")
                val lat = (jsonObject["results"] as JSONArray).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lat")
                val formattedAddress = (jsonObject["results"] as JSONArray).getJSONObject(0)
                    .getString("formatted_address")

                return@withContext Pair("$lat $lng", formattedAddress)
            } catch (e: Exception) {
                return@withContext Pair("", "")
            } finally {
                connection.disconnect()
            }
        }
    }
}