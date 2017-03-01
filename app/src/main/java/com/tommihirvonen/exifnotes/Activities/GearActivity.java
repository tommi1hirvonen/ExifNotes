package com.tommihirvonen.exifnotes.Activities;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.WindowManager;

import com.tommihirvonen.exifnotes.Adapters.PagerAdapter;
import com.tommihirvonen.exifnotes.Fragments.CamerasFragment;
import com.tommihirvonen.exifnotes.Fragments.FiltersFragment;
import com.tommihirvonen.exifnotes.Fragments.LensesFragment;
import com.tommihirvonen.exifnotes.R;

import java.util.Arrays;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class GearActivity extends AppCompatActivity {


    TabLayout tabLayout;
    ViewPager viewPager;

    PagerAdapter pagerAdapter;
    final static String GEAR_ACTIVITY_SAVED_VIEW = "GEAR_ACTIVITY_SAVED_VIEW";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ********** Commands to get the action bar and color it **********
        // Get preferences to determine UI color
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
            getSupportActionBar().setElevation(0);
            getSupportActionBar().setTitle(R.string.Gear);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(primaryColor)));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor(secondaryColor));
        }
        // *****************************************************************


        setContentView(R.layout.activity_gear);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        pagerAdapter = new PagerAdapter(getFragmentManager(), this);
        viewPager.setAdapter(pagerAdapter);

        // Give the TabLayout the ViewPager
        tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.white));
        tabLayout.setBackgroundColor(Color.parseColor(primaryColor));
        viewPager.setCurrentItem(prefs.getInt(GEAR_ACTIVITY_SAVED_VIEW, 0));
    }

    public void onStop(){
        super.onStop();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.edit().putInt(GEAR_ACTIVITY_SAVED_VIEW, viewPager.getCurrentItem()).apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static String POSITION = "POSITION";

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(POSITION, tabLayout.getSelectedTabPosition());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        viewPager.setCurrentItem(savedInstanceState.getInt(POSITION));
    }

    public void updateFragments(){
        int activeFragment = viewPager.getCurrentItem();

        //If the mountables were changed in one fragment, then update other fragments
        //to reflect the changes.

        //CamerasFragment is active
        if (activeFragment == 0) {
            if ((pagerAdapter.getItem(1)) != null) ((LensesFragment)pagerAdapter.getItem(1)).updateFragment();
        }
        //LensesFragment is active
        else if (activeFragment == 1) {
            if ((pagerAdapter.getItem(0)) != null) ((CamerasFragment)pagerAdapter.getItem(0)).updateFragment();
            if ((pagerAdapter.getItem(2)) != null) ((FiltersFragment)pagerAdapter.getItem(2)).updateFragment();
        }
        //FiltersFragment is active
        else if (activeFragment == 2) {
            if ((pagerAdapter.getItem(1)) != null) ((LensesFragment)pagerAdapter.getItem(1)).updateFragment();
        }
    }
}