package com.tommihirvonen.exifnotes.utilities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.datastructures.DateTime;

/**
 * Helper class to manage the date and time layout onClick events.
 * When the date layout is clicked, a date picker dialog is shown.
 * When the time layout is clicked, a time picker dialog is shown.
 * The DateTime member is managed inside the class.
 */
public class DateTimeLayoutManager {

    private DateTime dateTime;

    public DateTime getDateTime() {
        return dateTime;
    }

    public DateTimeLayoutManager(@NonNull final Activity activity, @NonNull final View dateLayout,
                      @NonNull final View timeLayout, @NonNull final TextView dateTextView,
                      @NonNull final TextView timeTextView, @Nullable final DateTime dateTimeParam,
                      @Nullable final View clearLayout) {
        dateTime = dateTimeParam;
        dateLayout.setOnClickListener(v -> {
            final DateTime dateTimeTemp = dateTime != null ? dateTime : DateTime.CREATOR.fromCurrentTime();
            final DatePickerDialog dialog = new DatePickerDialog(activity, (view, year, month, dayOfMonth) -> {
                dateTime = new DateTime(year, month + 1, dayOfMonth, dateTimeTemp.getHour(), dateTimeTemp.getMinute());
                dateTextView.setText(dateTime.getDateAsText());
                timeTextView.setText(dateTime.getTimeAsText());
            }, dateTimeTemp.getYear(), (dateTimeTemp.getMonth() - 1), dateTimeTemp.getDay());
            dialog.show();
        });
        timeLayout.setOnClickListener(v -> {
            final DateTime dateTimeTemp = dateTime != null ? dateTime : DateTime.CREATOR.fromCurrentTime();
            final TimePickerDialog dialog = new TimePickerDialog(activity, (view, hourOfDay, minute) -> {
                dateTime = new DateTime(dateTimeTemp.getYear(), dateTimeTemp.getMonth(), dateTimeTemp.getDay(), hourOfDay, minute);
                dateTextView.setText(dateTime.getDateAsText());
                timeTextView.setText(dateTime.getTimeAsText());
            }, dateTimeTemp.getHour(), dateTimeTemp.getMinute(), true);
            dialog.show();
        });
        if (clearLayout != null) {
            clearLayout.setOnClickListener(v -> {
                dateTime = null;
                dateTextView.setText(R.string.ClickToSet);
                timeTextView.setText("");
            });
        }
    }

}
