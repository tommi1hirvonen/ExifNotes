package com.tommihirvonen.exifnotes.activities;

import android.content.SharedPreferences;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import com.tommihirvonen.exifnotes.fragments.CamerasFragment;
import com.tommihirvonen.exifnotes.fragments.FiltersFragment;
import com.tommihirvonen.exifnotes.fragments.LensesFragment;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

/**
 * GearActivity contains fragments for adding, editing and removing cameras, lenses and filters.
 */
public class GearActivity extends AppCompatActivity {

    /**
     * Android TabLayout member
     */
    private TabLayout tabLayout;

    /**
     * ViewPager is responsible for changing the layout when the user swipes or clicks on a tab.
     */
    private ViewPager viewPager;

    /**
     * PagerAdapter adapts fragments to show in the ViewPager
     */
    private PagerAdapter pagerAdapter;

    /**
     * Tag for the index of the current view to store in SharedPreferences
     */
    private final static String GEAR_ACTIVITY_SAVED_VIEW = "GEAR_ACTIVITY_SAVED_VIEW";

    /**
     * Tag for the index of the current view to store while the activity is paused
     */
    private static final String POSITION = "POSITION";

    /**
     * Inflates the activity, sets the UI, ViewPager and TabLayout.
     *
     * @param savedInstanceState {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        overridePendingTransition(R.anim.enter_from_right, R.anim.hold);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (Utilities.isAppThemeDark(getBaseContext())) {
            setTheme(R.style.Theme_AppCompat);
        }

        super.onCreate(savedInstanceState);

        Utilities.setUiColor(this, true);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.Gear);

        setContentView(R.layout.activity_gear);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = findViewById(R.id.viewpager);
        pagerAdapter = new PagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        // Give the TabLayout the ViewPager
        tabLayout = findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.white));
        tabLayout.setBackgroundColor(Utilities.getPrimaryUiColor(getBaseContext()));

        //Get the index for the view which was last shown.
        viewPager.setCurrentItem(prefs.getInt(GEAR_ACTIVITY_SAVED_VIEW, 0));
    }

    /**
     * Saves the index of the current fragment so that when returning to this activity,
     * it will resume from the same fragment.
     */
    @Override
    public void onStop(){
        super.onStop();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.edit().putInt(GEAR_ACTIVITY_SAVED_VIEW, viewPager.getCurrentItem()).apply();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.nothing, R.anim.exit_to_right);
    }

    /**
     * Handle home as up press event.
     *
     * @param item {@inheritDoc}
     * @return call to super
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Puts the current ViewPager's index to outState.
     *
     * @param outState used to store the current index of ViewPager
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(POSITION, tabLayout.getSelectedTabPosition());
    }

    /**
     * Gets the displayed fragment's index from savedInstanceState
     *
     * @param savedInstanceState used to store the current index of ViewPager
     */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        viewPager.setCurrentItem(savedInstanceState.getInt(POSITION));
    }

    /**
     * When new mountable combinations are added to the database, other fragments' views
     * have to be updated too to display the changes. This method updates the necessary fragment(s).
     */
    public void updateFragments(){
        int activeFragment = viewPager.getCurrentItem();

        //If the mountables were changed in one fragment, then update other fragments
        //to reflect the changes.

        //CamerasFragment is active
        if (activeFragment == 0) {
            ((LensesFragment)pagerAdapter.getItem(1)).updateFragment();
        }
        //LensesFragment is active
        else if (activeFragment == 1) {
            ((CamerasFragment)pagerAdapter.getItem(0)).updateFragment();
            ((FiltersFragment)pagerAdapter.getItem(2)).updateFragment();
        }
        //FiltersFragment is active
        else if (activeFragment == 2) {
            ((LensesFragment)pagerAdapter.getItem(1)).updateFragment();
        }
    }

    private class PagerAdapter extends FragmentPagerAdapter {

        private static final int PAGE_COUNT = 3;

        private Fragment Lenses, Cameras, Filters;

        private PagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 2:
                    if(Filters == null)
                        Filters = new FiltersFragment();
                    return Filters;
                case 1:
                    if(Lenses == null)
                        Lenses = new LensesFragment();
                    return Lenses;
                case 0:
                    if(Cameras == null)
                        Cameras = new CamerasFragment();
                    return Cameras;
            }
            return new Fragment();
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 2:
                    return getApplicationContext().getResources().getString(R.string.Filters);
                case 1:
                    return getApplicationContext().getResources().getString(R.string.Lenses);
                case 0:
                    return getApplicationContext().getResources().getString(R.string.Cameras);
            }
            return null;
        }

    }

}