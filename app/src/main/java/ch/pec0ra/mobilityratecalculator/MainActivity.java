package ch.pec0ra.mobilityratecalculator;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Calendar;

import com.wdullaer.materialdatetimepicker.time.*;
import com.wdullaer.materialdatetimepicker.date.*;

public class MainActivity extends AppCompatActivity implements ItineraryDialog.NoticeDialogListener, ItineraryConfirmationDialog.NoticeDialogListener{

    private static final String DATE_PICKER_DIALOG = "date_picker_dialog";
    private static final String TIME_PICKER_DIALOG = "time_picker_dialog";
    private static final String ITINERARY_DIALOG = "itinerary_dialog";
    private static final String ITINERARY_CONFIRMATION_DIALOG = "itinerary_confirmation_dialog";

    private static final long HOUR_IN_MS = 3600000;
    private static final long HALF_HOUR_IN_MS = 1800000;
    Button fromField;
    Button toField;
    EditText kmInput;

    Calendar fromDate;
    Calendar toDate;
    Calendar tmpDate;

    ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCalculateJob();
            }
        });

        fromField = (Button)findViewById(R.id.from_field);
        toField = (Button)findViewById(R.id.to_field);
        kmInput = (EditText)findViewById(R.id.kilometers_input);

        fromDate = Calendar.getInstance();
        long roundedTime = (long) (Math.ceil((double)fromDate.getTimeInMillis() / (double)HALF_HOUR_IN_MS) * HALF_HOUR_IN_MS);
        fromDate.setTimeInMillis(roundedTime);
        toDate = Calendar.getInstance();
        toDate.setTimeInMillis(roundedTime + HOUR_IN_MS);

        findViewById(R.id.itinerary_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ItineraryDialog().show(getSupportFragmentManager(), ITINERARY_DIALOG);
            }
        });

        updateFields();

        fromField.setOnClickListener(new MyClickListener(true));
        toField.setOnClickListener(new MyClickListener(false));

    }


    private class MyClickListener implements View.OnClickListener {

        private final boolean isStart;
        private final Calendar cal;

        public MyClickListener(boolean isStart){
            this.isStart = isStart;
            if(isStart){
                this.cal = fromDate;
            } else {
                this.cal = toDate;
            }
        }

        @Override
        public void onClick(View v) {
            DatePickerDialog dialog = DatePickerDialog.newInstance(new MyDateSetListener(isStart), cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dialog.setMinDate(Calendar.getInstance());
            dialog.show(getFragmentManager(), DATE_PICKER_DIALOG);
        }
    };

    private class MyDateSetListener implements DatePickerDialog.OnDateSetListener {
        private final Calendar cal;
        private final boolean isStart;

        public MyDateSetListener(boolean isStart){
            this.isStart = isStart;
            if(isStart){
                this.cal = fromDate;
            } else {
                this.cal = toDate;
            }
        }

        @Override
        public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
            tmpDate = Calendar.getInstance();
            tmpDate.setTime(cal.getTime());
            tmpDate.set(Calendar.YEAR, year);
            tmpDate.set(Calendar.MONTH, monthOfYear);
            tmpDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            TimePickerDialog dialog = new TimePickerDialog().newInstance(new MyTimeSetListener(isStart), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
            dialog.setTimeInterval(1, 30);
            dialog.show(getFragmentManager(), TIME_PICKER_DIALOG);
        }
    }
    private class MyTimeSetListener implements TimePickerDialog.OnTimeSetListener {
        private final Calendar cal;
        private final boolean isStart;

        public MyTimeSetListener(boolean isStart){
            this.isStart = isStart;
            if(isStart){
                this.cal = fromDate;
            } else {
                this.cal = toDate;
            }
        }

        @Override
        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int second) {
            cal.setTime(tmpDate.getTime());
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
            cal.set(Calendar.MINUTE, minute);
            tmpDate = null;

            correctDates(isStart);

            updateFields();
        }
    }

    private void correctDates(boolean isStart) {
        long difference = toDate.getTimeInMillis() - fromDate.getTimeInMillis();
        if(isStart){
            if(difference < HOUR_IN_MS){
                toDate.setTimeInMillis(fromDate.getTimeInMillis() + HOUR_IN_MS);
            }
        } else {
            if(difference < HOUR_IN_MS){
                fromDate.setTimeInMillis(toDate.getTimeInMillis() - HOUR_IN_MS);
            }
        }
    }


    private void updateFields() {
        fromField.setText(DateFormat.format("dd.MM kk:mm", fromDate));
        toField.setText(DateFormat.format("dd.MM kk:mm", toDate));
    }

    private void startCalculateJob() {
        int kms = Integer.parseInt(kmInput.getText().toString());
        final RateCalculator rateCalculator = new RateCalculator(fromDate, toDate, kms);

        startLoad(getString(R.string.calculating));
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                rateCalculator.calculate();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                showPrices(rateCalculator);
                endLoad();
            }
        }.execute();
    }

    private void showPrices(RateCalculator rateCalculator) {
        Intent intent = new Intent(this, PricesActivity.class);
        intent.putExtra(PricesActivity.RATE_CALCULATOR_EXTRA, rateCalculator);
        startActivity(intent);
    }

    private void startLoad(String message){
        if(dialog != null){
            dialog.dismiss();
        }
        dialog = ProgressDialog.show(this, "", message, true, false);
    }
    private void endLoad(){
        if(dialog != null) {
            dialog.dismiss();
        }
        dialog = null;
    }

    @Override
    public void onDialogPositiveClick(final String from, final String to, final boolean isTwoWay) {
        startLoad(getString(R.string.calculating));
        new AsyncTask<Void, Void, DistanceCalculator.Itinerary>() {
            @Override
            protected DistanceCalculator.Itinerary doInBackground(Void... params) {
                return new DistanceCalculator(from, to, isTwoWay, getBaseContext()).calculate();
            }

            @Override
            protected void onPostExecute(final DistanceCalculator.Itinerary itinerary) {
                super.onPostExecute(itinerary);
                endLoad();
                if(itinerary.hasError){
                    Toast.makeText(getBaseContext(), itinerary.errorMessage, Toast.LENGTH_LONG).show();
                    return;
                }

                ItineraryConfirmationDialog confirmDialog = new ItineraryConfirmationDialog();
                confirmDialog.setItinerary(itinerary);
                confirmDialog.show(getSupportFragmentManager(), ITINERARY_CONFIRMATION_DIALOG);

            }
        }.execute();
    }
    @Override
    public void onDialogPositiveClick(int distance) {
        kmInput.setText(String.valueOf(distance));
    }


}
