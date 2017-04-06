package com.tommihirvonen.exifnotes.utilities;

import android.net.Uri;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * AsyncTask which takes a search string as an argument and returns
 * latitude and longitude location as well as a formatted address.
 * The class utilizes Google Maps's geocode api.
 */
public class GeocodingAsyncTask extends AsyncTask<String, Void, String[]> {

    /**
     * Reference to the implementing class's listener
     */
    private AsyncResponse delegate = null;

    /**
     * This interface is implemented in LocationPickActivity's onQueryTextSubmit.
     */
    public interface AsyncResponse {
        void processFinish(String output, String formattedAddress);
    }

    /**
     * Constructor to get reference to the calling class's AsyncResponse listener
     *
     * @param delegate AsyncResponse listener
     */
    public GeocodingAsyncTask(AsyncResponse delegate){
        this.delegate = delegate;
    }

    /**
     * Executed on a background thread after onPreExecute.
     *
     * Get the JSON array from the Google Maps geocode api.
     *
     * @param params String array with one element which is the search string
     * @return String array with one element which contains the JSON array.
     * If the connection was unsuccessful, the element is an empty string.
     */
    @Override
    protected String[] doInBackground(String... params) {
        String response;
        try {
            String queryUrl = new Uri.Builder()
                    .scheme("http")
                    .authority("maps.google.com")
                    .appendPath("maps")
                    .appendPath("api")
                    .appendPath("geocode")
                    .appendPath("json")
                    .appendQueryParameter("address", params[0])
                    .appendQueryParameter("sensor", "false")
                    .build().toString();
            response = getLatLongByURL(queryUrl);
            return new String[]{response};
        } catch (Exception e) {
            return new String[]{"error"};
        }
    }

    /**
     * Executed when doInBackground has finished. The return value from doInBackground
     * is passed as the argument to onPostExecute.
     *
     * Parse the JSON array to get the latitude, longitude and formatted address.
     *
     * @param result String array with one element containing the latitude longitude location
     *               and formatted address in JSON format
     */
    @Override
    protected void onPostExecute(String... result) {
        try {
            JSONObject jsonObject = new JSONObject(result[0]);

            double lng = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lng");

            double lat = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lat");

            String formattedAddress = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                    .getString("formatted_address");

            // Call the implementing class's processFinish to pass the location
            // and formatted address.
            delegate.processFinish(lat + " " + lng, formattedAddress);
        } catch (JSONException e) {
            e.printStackTrace();
            // In the case of an exception, pass empty string to the implementing class.
            delegate.processFinish("", "");
        }

    }

    /**
     * Generate a HTTP request from a request string and return the response string.
     *
     * @param requestURL the request URL string
     * @return response string
     */
    private String getLatLongByURL(String requestURL) {
        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            int responseCode = conn.getResponseCode();

            // If the connection was successful, add the connection result to the response string
            // one line at a time.
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }

                // Else return an empty string.
            } else {
                response = "";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}