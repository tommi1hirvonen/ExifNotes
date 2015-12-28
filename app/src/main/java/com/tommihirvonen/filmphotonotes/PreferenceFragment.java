package com.tommihirvonen.filmphotonotes;

import android.os.Bundle;

// Copyright 2015
// Tommi Hirvonen

public class PreferenceFragment extends android.preference.PreferenceFragment {


    // Notice that all we need to do is invoke the addPreferencesFromResource(..) method,
    // where we simply provide the reference to the preferences.xml file
    // and Android takes care of the rest for rendering the activity
    // and also saving the values for you.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_preference);
    }




}
