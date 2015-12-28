package com.tommihirvonen.filmphotonotes;



import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import java.util.Locale;


// Copyright 2015
// Tommi Hirvonen
public class PreferenceActivity extends android.preference.PreferenceActivity {



    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.preference_toolbar, root, false);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


    // THIS METHOD CAN BE USED IF THE SETTINGS OPTIONS SHOULD BE CATEGORIZED
//    @Override
//    public void onBuildHeaders(List<Header> target)
//    {
//        loadHeadersFromResource(R.xml.headers_preference, target);
//    }

    @Override
    protected boolean isValidFragment(String fragmentName)
    {
        return PreferenceFragment.class.getName().equals(fragmentName);
    }

}
