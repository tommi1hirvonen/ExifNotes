package com.tommihirvonen.filmphotonotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Tommi on 27.2.2015.
 */
public class EditFrameInfoDialog extends DialogFragment {


    String title;
    String lens;
    String date;
    int position;
    int count;
    ArrayList<String> lensList;


    static EditFrameInfoDialog newInstance(String title, String lens, int position, int count, String date, ArrayList<String> lensList) {
        EditFrameInfoDialog f = new EditFrameInfoDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("lens", lens);
        args.putInt("position", position);
        args.putInt("count", count);
        args.putString("date", date);
        args.putStringArrayList("lenses", lensList);
        f.setArguments(args);
        return f;
    }

    public static final String TAG = "SetLensDialogFragment";


    //private EditText txtName;
    //private Button btnDone;

    private OnEditSettedCallback callback;




    public interface OnEditSettedCallback {
        void onEditSetted(String new_lens, int position, int new_count, String new_date);
    }


    public EditFrameInfoDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnEditSettedCallback) activity;
        }
        catch(ClassCastException e) {
            Log.e(TAG, "The RollInfo should implement the OnEditSettedCallback interface");
            e.printStackTrace();
        }
    }




    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        title = getArguments().getString("title");
        lens = getArguments().getString("lens");
        position = getArguments().getInt("position");
        date = getArguments().getString("date");
        count = getArguments().getInt("count");
        lensList = getArguments().getStringArrayList("lenses");

        LayoutInflater linf = getActivity().getLayoutInflater();
        final View inflator = linf.inflate(R.layout.frame_info_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(title);

        alert.setView(inflator);

        final TextView b_lens = (TextView) inflator.findViewById(R.id.btn_lens);
        final TextView b_date = (TextView) inflator.findViewById(R.id.btn_date);
        final TextView b_time = (TextView) inflator.findViewById(R.id.btn_time);

        final NumberPicker np = (NumberPicker) inflator.findViewById(R.id.numberPicker);
        b_lens.setText(lens);

        np.setMinValue(0);
        np.setMaxValue(100);
        np.setValue(count);

        // LENS PICK DIALOG
        b_lens.setClickable(true);
        b_lens.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> listItems = new ArrayList<String>();
                for (int i = 0; i < lensList.size(); ++i) {
                    listItems.add(lensList.get(i));
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Choose used lens");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        b_lens.setText(lensList.get(which));
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        // DATE PICK DIALOG

        // The date is in format "YYYY-M-D HH:MM"

        ArrayList<String> dateValue = splitDate(date);
        final int i_year = Integer.parseInt(dateValue.get(0));
        final int i_month = Integer.parseInt(dateValue.get(1));
        final int i_day = Integer.parseInt(dateValue.get(2));
        b_date.setText(i_year + "-" + i_month + "-" + i_day);
        b_date.setClickable(true);

        b_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // DATE PICKER DIALOG IMPLEMENTATION HERE
                DatePickerDialog dialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String newDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                        b_date.setText(newDate);
                    }
                    // One month has to be subtracted from the default shown month, otherwise
                    // the date picker shows one month forward.
                }, i_year, (i_month - 1), i_day);

                dialog.show();

            }
        });

        // TIME PICK DIALOG
        ArrayList<String> timeValue = splitTime(date);
        final int hours = Integer.parseInt(timeValue.get(0));
        final int minutes = Integer.parseInt(timeValue.get(1));
        b_time.setText(hours + ":" + minutes);
        b_time.setClickable(true);

        b_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TIME PICKER DIALOG IMPLEMENTATION HERE
                TimePickerDialog dialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String newTime = "";
                        if (minute < 10) {
                            newTime = hourOfDay + ":0" + minute;
                        }
                        else newTime = hourOfDay + ":" + minute;
                        b_time.setText(newTime);
                    }
                }, hours, minutes, true);

                dialog.show();

            }
        });

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                //lens = et1.getText().toString();
                lens = b_lens.getText().toString();
                try {
                    //count = Integer.parseInt(et3.getText().toString());
                    count = np.getValue();
                }
                catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "Frame count was not changed!\nNew frame count was not a number", Toast.LENGTH_LONG).show();
                }

                // PARSE THE DATE
                date = b_date.getText().toString() + " " + b_time.getText().toString();


                //do operations using s1
                if(!lens.isEmpty()) {
                    // Return the new entered name to the calling activity
                    callback.onEditSetted(lens, position, count, date);
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = alert.create();
        //dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    private ArrayList<String> splitDate(String input) {
        String inputString = input;
        String[] items = inputString.split(" ");
        ArrayList<String> itemList = new ArrayList<String>(Arrays.asList(items));
        // { YYYY-M-D, HH:MM }
        String[] items2 = itemList.get(0).split("-");
        itemList = new ArrayList<String>(Arrays.asList(items2));
        // { YYYY, M, D }
        return itemList;
    }

    private ArrayList<String> splitTime(String input) {
        String inputString = input;
        String[] items = inputString.split(" ");
        ArrayList<String> itemList = new ArrayList<String>(Arrays.asList(items));
        // { YYYY-M-D, HH:MM }
        String[] items2 = itemList.get(1).split(":");
        itemList = new ArrayList<String>(Arrays.asList(items2));
        // { HH, MM }
        return itemList;
    }

}
