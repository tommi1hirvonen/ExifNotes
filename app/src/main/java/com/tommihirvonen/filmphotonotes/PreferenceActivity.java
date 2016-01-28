package com.tommihirvonen.filmphotonotes;



import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.Arrays;
import java.util.List;


// Copyright 2015
// Tommi Hirvonen

public class PreferenceActivity extends android.preference.PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    Toolbar bar;
    Toolbar actionbar;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Get preferences to determine UI color
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        final String primaryColor = colors.get(0);
        final String secondaryColor = colors.get(1);

        prefs.registerOnSharedPreferenceChangeListener(this);

        // This is a way to get the action bar in Preferences.
        // It will be done only on Androids > 5.0.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            /*LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
            bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.preference_toolbar , root, false);
            bar = (Toolbar) findViewById(R.id.actionbar);
            bar.setBackgroundColor(Color.parseColor(primaryColor));
            root.addView(bar, 0); // insert at top
            bar.setElevation(0);
            bar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });*/
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor(secondaryColor));
        }
        //else {

            // This is a legacy implementation. All this is needed in order to make
            // the action bar title and icon appear in white. WTF!?
            setContentView(R.layout.activity_settings_legacy);
            actionbar = (Toolbar) findViewById(R.id.actionbar);
            actionbar.setTitle(R.string.Preferences);
            actionbar.setBackgroundColor(Color.parseColor(primaryColor));
            actionbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
            actionbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_mtrl_am_alpha));
            actionbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            getFragmentManager().beginTransaction().add(R.id.rel_layout, new PreferenceFragment()).commit();
        //}
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        // Allow super to try and create a view first
        final View result = super.onCreateView(name, context, attrs);
        if (result != null) {
            return result;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // If we're running pre-L, we need to 'inject' our tint aware Views in place of the
            // standard framework versions
            switch (name) {
                case "EditText":
                    return new AppCompatEditText(this, attrs);
                case "Spinner":
                    return new AppCompatSpinner(this, attrs);
                case "CheckBox":
                    return new AppCompatCheckBox(this, attrs);
                case "RadioButton":
                    return new AppCompatRadioButton(this, attrs);
                case "CheckedTextView":
                    return new AppCompatCheckedTextView(this, attrs);
            }
        }

        return null;
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Get preferences to determine UI color
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        final String primaryColor = colors.get(0);
        final String secondaryColor = colors.get(1);
        // This is a way to get the action bar in Preferences.
        // It will be done only on Androids > 5.0.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bar.setBackgroundColor(Color.parseColor(primaryColor));
            getWindow().setStatusBarColor( Color.parseColor(secondaryColor) );
        }
        else {
            actionbar = (Toolbar) findViewById(R.id.actionbar);
            actionbar.setBackgroundColor(Color.parseColor(primaryColor));
        }
    }
}
