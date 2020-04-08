package com.tommihirvonen.exifnotes.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

        final FilmStocksFragment filmStocksFragment = (FilmStocksFragment) pagerAdapter.getItem(POSITION_FILMS);

        // Handle actions here, since the fragment may be paused and cannot handle actions.
        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.sort_mode_film_stock_name:
                filmStocksFragment.setSortMode(FilmStocksFragment.SORT_MODE_NAME, true);
                item.setChecked(true);
                return true;

            case R.id.sort_mode_film_stock_iso:
                filmStocksFragment.setSortMode(FilmStocksFragment.SORT_MODE_ISO, true);
                item.setChecked(true);
                return true;

            case R.id.filter_mode_film_manufacturer:
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final String[] manufacturers = FilmDbHelper.getInstance(this)
                        .getAllFilmManufacturers().toArray(new String[0]);
                final List<String> manufacturerFilterList = filmStocksFragment.getManufacturerFilterList();
                final boolean[] checkedItems = new boolean[manufacturers.length];
                for (int i = 0; i < manufacturers.length; ++i) {
                    if (manufacturerFilterList.contains(manufacturers[i])) checkedItems[i] = true;
                }

                // Create a temporary list to be updated.
                // If the user cancels the dialog, the original list will remain unchanged.
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
                    filmStocksFragment.setManufacturerFilterList(manufacturerFilterListTemp);
                    filmStocksFragment.filterFilmStocks();
                });
                builder.setNeutralButton(R.string.Reset, (dialog, which) -> {
                    filmStocksFragment.getManufacturerFilterList().clear();
                    filmStocksFragment.filterFilmStocks();
                });
                builder.create().show();
                return true;

            case R.id.filter_mode_added_by:
                final AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                final int checkedItem;
                final int filterModeAddedBy = filmStocksFragment.getAddedByFilterMode();
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
                            filmStocksFragment.setAddedByFilterMode(FilmStocksFragment.FILTER_MODE_ALL);
                            break;
                        case 1:
                            filmStocksFragment.setAddedByFilterMode(FilmStocksFragment.FILTER_MODE_PREADDED);
                            break;
                        case 2:
                            filmStocksFragment.setAddedByFilterMode(FilmStocksFragment.FILTER_MODE_ADDED_BY_USER);
                            break;
                    }
                    filmStocksFragment.filterFilmStocks();
                    dialog.dismiss();
                });
                builder1.setNegativeButton(R.string.Cancel, (dialog, which) -> {});
                builder1.create().show();
                return true;

            case R.id.filter_mode_film_iso:
                final AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                final List<Integer> isoValues = filmStocksFragment.possibleIsoValues();
                final List<Integer> isoFilterList = filmStocksFragment.getIsoFilterList();
                final String[] isoValueStrings = new String[isoValues.size()];
                final boolean[] checkedItems2 = new boolean[isoValues.size()];
                for (int i = 0; i < isoValues.size(); i++) {
                    isoValueStrings[i] = isoValues.get(i).toString();
                    if (isoFilterList.contains(isoValues.get(i))) {
                        checkedItems2[i] = true;
                    }
                }

                // Create a temporary list to be updated.
                // If the user cancels the dialog, the original list will remain unchanged.
                final List<Integer> isoFilterListTemp = new ArrayList<>(isoFilterList);
                builder2.setMultiChoiceItems(isoValueStrings, checkedItems2, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        isoFilterListTemp.add(isoValues.get(which));
                    } else {
                        isoFilterListTemp.remove(isoValues.get(which));
                    }
                });
                builder2.setNegativeButton(R.string.Cancel, (dialog, which) -> {/*Do nothing*/});
                builder2.setPositiveButton(R.string.FilterNoColon, (dialog, which) -> {
                    filmStocksFragment.setIsoFilterList(isoFilterListTemp);
                    filmStocksFragment.filterFilmStocks();
                });
                builder2.setNeutralButton(R.string.Reset, (dialog, which) -> {
                    filmStocksFragment.getIsoFilterList().clear();
                    filmStocksFragment.filterFilmStocks();
                });
                builder2.create().show();
                return true;

            case R.id.filter_mode_film_type:
                final AlertDialog.Builder filmTypeBuilder = new AlertDialog.Builder(this);
                final String[] filmTypes = getResources().getStringArray(R.array.FilmTypes);
                final List<Integer> filmTypeFilterList = filmStocksFragment.getFilmTypeFilterList();
                final boolean[] filmTypeCheckedItems = new boolean[filmTypes.length];
                for (int i = 0; i < filmTypes.length; ++i) {
                    if (filmTypeFilterList.contains(i)) filmTypeCheckedItems[i] = true;
                }

                // Create a temporary list to be updated.
                // If the user cancels the dialog, the original list will remain unchanged.
                final List<Integer> filmTypeFilterListTemp = new ArrayList<>(filmTypeFilterList);
                filmTypeBuilder.setMultiChoiceItems(filmTypes, filmTypeCheckedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        filmTypeFilterListTemp.add(which);
                    } else {
                        filmTypeFilterListTemp.remove(which);
                    }
                });
                filmTypeBuilder.setNegativeButton(R.string.Cancel, (dialog, which) -> { /* Do nothing */ });
                filmTypeBuilder.setPositiveButton(R.string.FilterNoColon, (dialog, which) -> {
                    filmStocksFragment.setFilmTypeFilterList(filmTypeFilterListTemp);
                    filmStocksFragment.filterFilmStocks();
                });
                filmTypeBuilder.setNeutralButton(R.string.Reset, (dialog, which) -> {
                    filmStocksFragment.getFilmTypeFilterList().clear();
                    filmStocksFragment.filterFilmStocks();
                });
                filmTypeBuilder.create().show();
                return true;
                
            case R.id.filter_mode_film_process:
                final AlertDialog.Builder filmProcessBuilder = new AlertDialog.Builder(this);
                final String[] filmProcess = getResources().getStringArray(R.array.FilmProcesses);
                final List<Integer> filmProcessFilterList = filmStocksFragment.getFilmProcessFilterList();
                final boolean[] filmProcessCheckedItems = new boolean[filmProcess.length];
                for (int i = 0; i < filmProcess.length; ++i) {
                    if (filmProcessFilterList.contains(i)) filmProcessCheckedItems[i] = true;
                }

                // Create a temporary list to be updated.
                // If the user cancels the dialog, the original list will remain unchanged.
                final List<Integer> filmProcessFilterListTemp = new ArrayList<>(filmProcessFilterList);
                filmProcessBuilder.setMultiChoiceItems(filmProcess, filmProcessCheckedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        filmProcessFilterListTemp.add(which);
                    } else {
                        filmProcessFilterListTemp.remove(which);
                    }
                });
                filmProcessBuilder.setNegativeButton(R.string.Cancel, (dialog, which) -> { /* Do nothing */ });
                filmProcessBuilder.setPositiveButton(R.string.FilterNoColon, (dialog, which) -> {
                    filmStocksFragment.setFilmProcessFilterList(filmProcessFilterListTemp);
                    filmStocksFragment.filterFilmStocks();
                });
                filmProcessBuilder.setNeutralButton(R.string.Reset, (dialog, which) -> {
                    filmStocksFragment.getFilmProcessFilterList().clear();
                    filmStocksFragment.filterFilmStocks();
                });
                filmProcessBuilder.create().show();
                return true;
                
            case R.id.filter_mode_reset:
                filmStocksFragment.resetFilters();
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If a fragment other than FilmStocksFragment is being shown, disable the film stock
        // filtering and sorting options. This is also done so that the filter and sort methods
        // of FilmStocksFragment aren't called when its late init members are not initialized because
        // its onCreate() hasn't yet been called.
        if (viewPager.getCurrentItem() != POSITION_FILMS) {
            menu.findItem(R.id.sort_mode_film_stock_name).setEnabled(false);
            menu.findItem(R.id.sort_mode_film_stock_iso).setEnabled(false);
            menu.findItem(R.id.filter_mode_film_manufacturer).setEnabled(false);
            menu.findItem(R.id.filter_mode_added_by).setEnabled(false);
            menu.findItem(R.id.filter_mode_film_iso).setEnabled(false);
            menu.findItem(R.id.filter_mode_film_type).setEnabled(false);
            menu.findItem(R.id.filter_mode_film_process).setEnabled(false);
            menu.findItem(R.id.filter_mode_reset).setEnabled(false);
        } else {
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