package com.tommihirvonen.filmphotonotes;

// Copyright 2015
// Tommi Hirvonen

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

public class GeocodingAsyncTask extends AsyncTask<String, Void, String[]> {
    @Override
    protected void onPreExecute() {
        super.onPreExecute();

    }

    public interface AsyncResponse {
        void processFinish(String output, String formattedAddress);
    }
    public AsyncResponse delegate = null;

    public GeocodingAsyncTask(AsyncResponse delegate){
        this.delegate = delegate;
    }

    @Override
    protected String[] doInBackground(String... params) {
        String response;
        try {
            String queryUrl = new Uri.Builder().scheme("http").authority("maps.google.com").appendPath("maps").appendPath("api").appendPath("geocode").appendPath("json").appendQueryParameter("address", params[0]).appendQueryParameter("sensor", "false").build().toString();
            response = getLatLongByURL(queryUrl);
            return new String[]{response};
        } catch (Exception e) {
            return new String[]{"error"};
        }
    }

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
            delegate.processFinish(lat + " " + lng, formattedAddress);
        } catch (JSONException e) {
            e.printStackTrace();
            delegate.processFinish("", "");
        }

    }



    public String getLatLongByURL(String requestURL) {
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

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else {
                response = "";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}