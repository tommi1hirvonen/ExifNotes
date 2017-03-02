package com.tommihirvonen.exifnotes.Activities;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.tommihirvonen.exifnotes.Adapters.PagerAdapter;
import com.tommihirvonen.exifnotes.Fragments.CamerasFragment;
import com.tommihirvonen.exifnotes.Fragments.FiltersFragment;
import com.tommihirvonen.exifnotes.Fragments.LensesFragment;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

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

        Utilities.setUiColor(this, true);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.Gear);

        setContentView(R.layout.activity_gear);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        pagerAdapter = new PagerAdapter(getFragmentManager(), this);
        viewPager.setAdapter(pagerAdapter);

        // Give the TabLayout the ViewPager
        tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.white));
        tabLayout.setBackgroundColor(Utilities.getPrimaryUiColor(getBaseContext()));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
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