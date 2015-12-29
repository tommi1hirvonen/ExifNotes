package com.tommihirvonen.filmphotonotes;



import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;


// Copyright 2015
// Tommi Hirvonen
public class PreferenceActivity extends android.preference.PreferenceActivity {



    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // This is a way to get the action bar in Preferences
        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.preference_toolbar , root, false);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor( ContextCompat.getColor(this, R.color.dark_orange) );
        }

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
