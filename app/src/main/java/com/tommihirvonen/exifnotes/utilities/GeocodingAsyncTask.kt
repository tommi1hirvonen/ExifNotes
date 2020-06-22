package com.tommihirvonen.exifnotes.utilities

import android.net.Uri
import android.os.AsyncTask
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * AsyncTask which takes a search string and Google Maps API key as arguments and returns
 * latitude and longitude location as well as a formatted address.
 * The class utilizes the http version of Google Maps Geocode API.
 */
class GeocodingAsyncTask(
        private val onResult: (latLngString: String, formattedAddress: String) -> Unit
) : AsyncTask<String, Void?, Array<String>>() {

    /**
     * {@inheritDoc}
     *
     * @param params String array with two elements: first one either coordinates or address and
     * the second one the Google API key of this application.
     * @return String array with one element which contains the JSON array.
     * If the connection was unsuccessful, the element is an empty string.
     */
    override fun doInBackground(vararg params: String): Array<String> {
        // Get the JSON array from the Google Maps geocode API.
        val response: String
        return try {
            val queryUrl = Uri.Builder() // Requests must be made over SSL.
                    .scheme("https")
                    .authority("maps.google.com")
                    .appendPath("maps")
                    .appendPath("api")
                    .appendPath("geocode")
                    .appendPath("json") // Use address parameter for both the coordinates and search string.
                    .appendQueryParameter("address", params[0])
                    .appendQueryParameter("sensor", "false") // Use key parameter to pass the API key credentials.
                    .appendQueryParameter("key", params[1])
                    .build().toString()
            response = getLatLongByURL(queryUrl)
            arrayOf(response)
        } catch (e: Exception) {
            arrayOf("error")
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param result String array with one element containing the latitude longitude location
     * and formatted address in JSON format
     */
    override fun onPostExecute(result: Array<String>) {
        // Parse the JSON array to get the latitude, longitude and formatted address.
        try {
            val jsonObject = JSONObject(result[0])
            val lng = (jsonObject["results"] as JSONArray).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lng")
            val lat = (jsonObject["results"] as JSONArray).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lat")
            val formattedAddress = (jsonObject["results"] as JSONArray).getJSONObject(0)
                    .getString("formatted_address")

            // Call the implementing class's processFinish to pass the location
            // and formatted address.
            onResult("$lat $lng", formattedAddress)
        } catch (e: JSONException) {
            e.printStackTrace()
            // In the case of an exception, pass empty string to the implementing class.
            onResult("", "")
        }
    }

    /**
     * Generate a HTTP request from a request string and return the response string.
     *
     * @param requestURL the request URL string
     * @return response string
     */
    private fun getLatLongByURL(requestURL: String): String {
        val url: URL
        var response = StringBuilder()
        try {
            url = URL(requestURL)
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 15000
            conn.connectTimeout = 15000
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true
            val responseCode = conn.responseCode

            // If the connection was successful, add the connection result to the response string
            // one line at a time.
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                var line: String?
                val br = BufferedReader(InputStreamReader(conn.inputStream))
                while (br.readLine().also { line = it } != null) {
                    response.append(line)
                }
            }
            // Else return an empty string.
            else {
                response = StringBuilder()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return response.toString()
    }

}