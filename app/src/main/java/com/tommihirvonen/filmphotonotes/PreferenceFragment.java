package com.tommihirvonen.filmphotonotes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;

// Copyright 2015
// Tommi Hirvonen

public class PreferenceFragment extends android.preference.PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {


    // Notice that all we need to do is invoke the addPreferencesFromResource(..) method,
    // where we simply provide the reference to the preferences.xml file
    // and Android takes care of the rest for rendering the activity
    // and also saving the values for you.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_preference);

        // Set summaries for the list preferences
        Preference shutterIncrements = findPreference("ShutterIncrements");
        shutterIncrements.setSummary(((ListPreference) shutterIncrements).getEntry());
        Preference apertureIncrements = findPreference("ApertureIncrements");
        apertureIncrements.setSummary(((ListPreference) apertureIncrements).getEntry());
        Preference UIColor = findPreference("UIColor");
        UIColor.setSummary(((ListPreference) UIColor).getEntry());
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Set summaries for the list preferences
        Preference shutterIncrements = findPreference("ShutterIncrements");
        shutterIncrements.setSummary(((ListPreference) shutterIncrements).getEntry());
        Preference apertureIncrements = findPreference("ApertureIncrements");
        apertureIncrements.setSummary(((ListPreference) apertureIncrements).getEntry());
        Preference UIColor = findPreference("UIColor");
        UIColor.setSummary(((ListPreference) UIColor).getEntry());
    }
}
