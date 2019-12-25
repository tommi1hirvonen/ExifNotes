package com.tommihirvonen.exifnotes.utilities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.tommihirvonen.exifnotes.R;

public class UIColorPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    public static UIColorPreferenceDialogFragment newInstance(String key) {
        final UIColorPreferenceDialogFragment fragment = new UIColorPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }


    /**
     * Holds the index of the selected color option
     */
    private int index = 1;

    /**
     * References to the checkbox views
     */
    private ImageView checkbox1;
    private ImageView checkbox2;
    private ImageView checkbox3;
    private ImageView checkbox4;
    private ImageView checkbox5;
    private ImageView checkbox6;
    private ImageView checkbox7;
    private ImageView checkbox8;


    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ImageView color1 = view.findViewById(R.id.ui_color_option_1);
        ImageView color2 = view.findViewById(R.id.ui_color_option_2);
        ImageView color3 = view.findViewById(R.id.ui_color_option_3);
        ImageView color4 = view.findViewById(R.id.ui_color_option_4);
        ImageView color5 = view.findViewById(R.id.ui_color_option_5);
        ImageView color6 = view.findViewById(R.id.ui_color_option_6);
        ImageView color7 = view.findViewById(R.id.ui_color_option_7);
        ImageView color8 = view.findViewById(R.id.ui_color_option_8);
        checkbox1 = view.findViewById(R.id.checkbox_1);
        checkbox2 = view.findViewById(R.id.checkbox_2);
        checkbox3 = view.findViewById(R.id.checkbox_3);
        checkbox4 = view.findViewById(R.id.checkbox_4);
        checkbox5 = view.findViewById(R.id.checkbox_5);
        checkbox6 = view.findViewById(R.id.checkbox_6);
        checkbox7 = view.findViewById(R.id.checkbox_7);
        checkbox8 = view.findViewById(R.id.checkbox_8);
        color1.setOnClickListener(new ColorOnClickListener());
        color2.setOnClickListener(new ColorOnClickListener());
        color3.setOnClickListener(new ColorOnClickListener());
        color4.setOnClickListener(new ColorOnClickListener());
        color5.setOnClickListener(new ColorOnClickListener());
        color6.setOnClickListener(new ColorOnClickListener());
        color7.setOnClickListener(new ColorOnClickListener());
        color8.setOnClickListener(new ColorOnClickListener());

        DialogPreference preference = getPreference();
        if (preference instanceof UIColorDialogPreference) {
            index = ((UIColorDialogPreference) preference).getSelectedColorIndex();
        }

        updateCheckboxVisibility();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {

            DialogPreference preference = getPreference();

            if (preference instanceof UIColorDialogPreference &&
                    preference.callChangeListener(((UIColorDialogPreference) preference).getSelectedColorData())) {

                ((UIColorDialogPreference) preference).setUIColor(index);
                preference.setSummary(((UIColorDialogPreference) preference).getSelectedColorName());
            }
        }
    }

    private void updateCheckboxVisibility() {
        checkbox1.setVisibility(View.GONE);
        checkbox2.setVisibility(View.GONE);
        checkbox3.setVisibility(View.GONE);
        checkbox4.setVisibility(View.GONE);
        checkbox5.setVisibility(View.GONE);
        checkbox6.setVisibility(View.GONE);
        checkbox7.setVisibility(View.GONE);
        checkbox8.setVisibility(View.GONE);
        switch (index) {
            case 0:
                checkbox1.setVisibility(View.VISIBLE);
                break;
            case 1:
                checkbox2.setVisibility(View.VISIBLE);
                break;
            case 2:
                checkbox3.setVisibility(View.VISIBLE);
                break;
            case 3:
                checkbox4.setVisibility(View.VISIBLE);
                break;
            case 4:
                checkbox5.setVisibility(View.VISIBLE);
                break;
            case 5:
                checkbox6.setVisibility(View.VISIBLE);
                break;
            case 6:
                checkbox7.setVisibility(View.VISIBLE);
                break;
            case 7:
                checkbox8.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private class ColorOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            final int id = view.getId();
            switch (id) {
                case R.id.ui_color_option_1:
                    index = 0;
                    break;
                case R.id.ui_color_option_2:
                    index = 1;
                    break;
                case R.id.ui_color_option_3:
                    index = 2;
                    break;
                case R.id.ui_color_option_4:
                    index = 3;
                    break;
                case R.id.ui_color_option_5:
                    index = 4;
                    break;
                case R.id.ui_color_option_6:
                    index = 5;
                    break;
                case R.id.ui_color_option_7:
                    index = 6;
                    break;
                case R.id.ui_color_option_8:
                    index = 7;
                    break;
                default:
                    break;
            }
            updateCheckboxVisibility();
        }
    }
}
