package com.tommihirvonen.exifnotes.adapters;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import com.tommihirvonen.exifnotes.fragments.CamerasFragment;
import com.tommihirvonen.exifnotes.fragments.FiltersFragment;
import com.tommihirvonen.exifnotes.fragments.LensesFragment;
import com.tommihirvonen.exifnotes.R;

/**
 * PagerAdapter links CamerasFragment, LensesFragment and FiltersFragment to GearActivity
 * so that they are displayed in a TabLayout.
 */
public class PagerAdapter extends FragmentPagerAdapter {

    /**
     * The number of pages in this adapter
     */
    private static final int PAGE_COUNT = 3;

    /**
     * Reference to the parent activity
     */
    private final Activity activity;

    /**
     * Private members to hold references to the displayed Fragments.
     */
    private Fragment Lenses, Cameras, Filters;

    /**
     * Call parent class constructor and get reference to the parent activity.
     *
     * @param fm {@inheritDoc}
     * @param activity {@inheritDoc}
     */
    public PagerAdapter(FragmentManager fm, Activity activity) {
        super(fm);
        this.activity = activity;
    }

    /**
     * Get the number of pages for this adapter
     *
     * @return private member PAGE_COUNT
     */
    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    /**
     * Get the current Fragment displayed by this adapter.
     *
     * @param position {@inheritDoc}
     * @return the currently displayed Fragment
     */
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
        return null;
    }

    /**
     * Get the title text for the currently displayed Fragment.
     *
     * @param position {@inheritDoc}
     * @return the title for the currently displayed Fragment
     */
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 2:
                return activity.getResources().getString(R.string.Filters);
            case 1:
                return activity.getResources().getString(R.string.Lenses);
            case 0:
                return activity.getResources().getString(R.string.Cameras);
        }
        return null;
    }
}
