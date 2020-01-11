package com.tommihirvonen.exifnotes.activities;

import android.app.AlertDialog;
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

import android.view.Menu;
import android.view.MenuItem;

import com.tommihirvonen.exifnotes.fragments.CamerasFragment;
import com.tommihirvonen.exifnotes.fragments.FilmStocksFragment;
import com.tommihirvonen.exifnotes.fragments.FiltersFragment;
import com.tommihirvonen.exifnotes.fragments.LensesFragment;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

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

    private List<String> manufacturerFilterList = new ArrayList<>();

    private int filterModeAddedBy = FilmStocksFragment.FILTER_MODE_ALL;

    private static final int POSITION_CAMERAS = 0;
    private static final int POSITION_LENSES = 1;
    private static final int POSITION_FILTERS = 2;
    private static final int POSITION_FILMS = 3;

    /**
     * Inflates the activity, sets the UI, ViewPager and TabLayout.
     *
     * @param savedInstanceState {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        overridePendingTransition(R.anim.enter_from_right, R.anim.hold);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

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
        viewPager.setCurrentItem(prefs.getInt(GEAR_ACTIVITY_SAVED_VIEW, POSITION_CAMERAS));
    }

    /**
     * Saves the index of the current fragment so that when returning to this activity,
     * it will resume from the same fragment.
     */
    @Override
    public void onStop(){
        super.onStop();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.edit().putInt(GEAR_ACTIVITY_SAVED_VIEW, viewPager.getCurrentItem()).apply();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.nothing, R.anim.exit_to_right);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gear_actvity, menu);
        menu.findItem(R.id.sort_mode_film_stock_name).setChecked(true);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Handle home as up press event.
     *
     * @param item {@inheritDoc}
     * @return call to super
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle actions here, since the fragment may be paused and cannot handle actions.
        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.sort_mode_film_stock_name:
                ((FilmStocksFragment) pagerAdapter.getItem(POSITION_FILMS))
                        .setSortMode(FilmStocksFragment.SORT_MODE_NAME, true);
                item.setChecked(true);
                return true;

            case R.id.sort_mode_film_stock_iso:
                ((FilmStocksFragment) pagerAdapter.getItem(POSITION_FILMS))
                        .setSortMode(FilmStocksFragment.SORT_MODE_ISO, true);
                item.setChecked(true);
                return true;

            case R.id.filter_mode_film_manufacturer:
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final String[] manufacturers = FilmDbHelper.getInstance(this)
                        .getAllFilmManufacturers().toArray(new String[0]);
                final boolean[] checkedItems = new boolean[manufacturers.length];
                for (int i = 0; i < manufacturers.length; ++i) {
                    if (manufacturerFilterList.contains(manufacturers[i])) checkedItems[i] = true;
                }

                // Create a temporary list to be updated.
                // If the user cancel's the dialog, the original list will remain unchanged.
                final List<String> manufacturerFilterListTemp = new ArrayList<>(manufacturerFilterList);
                builder.setMultiChoiceItems(manufacturers, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        manufacturerFilterListTemp.add(manufacturers[which]);
                    } else {
                        manufacturerFilterListTemp.remove(manufacturers[which]);
                    }
                });
                builder.setNegativeButton(R.string.Cancel, (dialog, which) -> { /* Do nothing */ });
                builder.setPositiveButton(R.string.FilterNoColon, (dialog, which) -> {
                    manufacturerFilterList = manufacturerFilterListTemp;
                    ((FilmStocksFragment) pagerAdapter.getItem(POSITION_FILMS))
                            .filterFilmStocks(manufacturerFilterList, filterModeAddedBy);
                });
                builder.setNeutralButton(R.string.Reset, (dialog, which) -> {
                    manufacturerFilterList.clear();
                    ((FilmStocksFragment) pagerAdapter.getItem(POSITION_FILMS))
                            .filterFilmStocks(manufacturerFilterList, filterModeAddedBy);
                });
                builder.create().show();
                return true;

            case R.id.filter_mode_added_by:
                final AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                final int checkedItem;
                if (filterModeAddedBy == FilmStocksFragment.FILTER_MODE_PREADDED) {
                    checkedItem = 1;
                } else if (filterModeAddedBy == FilmStocksFragment.FILTER_MODE_ADDED_BY_USER) {
                    checkedItem = 2;
                } else {
                    checkedItem = 0;
                }
                builder1.setSingleChoiceItems(R.array.FilmStocksFilterMode, checkedItem, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            filterModeAddedBy = FilmStocksFragment.FILTER_MODE_ALL;
                            break;
                        case 1:
                            filterModeAddedBy = FilmStocksFragment.FILTER_MODE_PREADDED;
                            break;
                        case 2:
                            filterModeAddedBy = FilmStocksFragment.FILTER_MODE_ADDED_BY_USER;
                            break;
                    }
                    ((FilmStocksFragment) pagerAdapter.getItem(POSITION_FILMS))
                            .filterFilmStocks(manufacturerFilterList, filterModeAddedBy);
                    dialog.dismiss();
                });
                builder1.setNegativeButton(R.string.Cancel, (dialog, which) -> {});
                builder1.create().show();
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // When the options menu is opened, set the correct items to be preselected.
        final FilmStocksFragment fragment = (FilmStocksFragment) pagerAdapter.getItem(3);
        switch (fragment.getSortMode()) {
            case FilmStocksFragment.SORT_MODE_NAME:
                menu.findItem(R.id.sort_mode_film_stock_name).setChecked(true);
                break;
            case FilmStocksFragment.SORT_MODE_ISO:
                menu.findItem(R.id.sort_mode_film_stock_iso).setChecked(true);
                break;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Puts the current ViewPager's index to outState.
     *
     * @param outState used to store the current index of ViewPager
     */
    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(POSITION, tabLayout.getSelectedTabPosition());
    }

    /**
     * Gets the displayed fragment's index from savedInstanceState
     *
     * @param savedInstanceState used to store the current index of ViewPager
     */
    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        viewPager.setCurrentItem(savedInstanceState.getInt(POSITION));
    }

    /**
     * When new mountable combinations are added to the database, other fragments' views
     * have to be updated too to display the changes. This method updates the necessary fragment(s).
     */
    public void updateFragments(){
        final int activeFragment = viewPager.getCurrentItem();

        //If the mountables were changed in one fragment, then update other fragments
        //to reflect the changes.

        //CamerasFragment is active
        if (activeFragment == POSITION_CAMERAS) {
            ((LensesFragment)pagerAdapter.getItem(POSITION_LENSES)).updateFragment();
        }
        //LensesFragment is active
        else if (activeFragment == POSITION_LENSES) {
            ((CamerasFragment)pagerAdapter.getItem(POSITION_CAMERAS)).updateFragment();
            ((FiltersFragment)pagerAdapter.getItem(POSITION_FILTERS)).updateFragment();
        }
        //FiltersFragment is active
        else if (activeFragment == POSITION_FILTERS) {
            ((LensesFragment)pagerAdapter.getItem(POSITION_LENSES)).updateFragment();
        }
    }

    private class PagerAdapter extends FragmentPagerAdapter {

        private static final int PAGE_COUNT = 4;

        private Fragment Lenses, Cameras, Filters, Films;

        private PagerAdapter(@NonNull final FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(final int position) {
            switch (position) {
                case POSITION_FILMS:
                    if (Films == null)
                        Films = new FilmStocksFragment();
                    return Films;
                case POSITION_FILTERS:
                    if(Filters == null)
                        Filters = new FiltersFragment();
                    return Filters;
                case POSITION_LENSES:
                    if(Lenses == null)
                        Lenses = new LensesFragment();
                    return Lenses;
                case POSITION_CAMERAS:
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
        public CharSequence getPageTitle(final int position) {
            switch (position) {
                case POSITION_FILMS:
                    return getApplicationContext().getResources().getString(R.string.FilmStocks);
                case POSITION_FILTERS:
                    return getApplicationContext().getResources().getString(R.string.Filters);
                case POSITION_LENSES:
                    return getApplicationContext().getResources().getString(R.string.Lenses);
                case POSITION_CAMERAS:
                    return getApplicationContext().getResources().getString(R.string.Cameras);
            }
            return null;
        }

    }

}