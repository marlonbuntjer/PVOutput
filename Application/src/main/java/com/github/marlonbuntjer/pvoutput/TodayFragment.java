package com.github.marlonbuntjer.pvoutput;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by Marlon Buntjer on 10-6-2015.
 */
public class TodayFragment extends Fragment {

    private static final String TAG = TodayFragment.class.getSimpleName();
    private static final String PREFS_NAME = "SHAREDPREFS";
    private static List<String[]> todayData;
    private LineChart mChart;
    private SharedPreferences mSharedPreferences;
    private boolean mConsumptionEnabled;

    private ListView mListView;
    private StringArrayAdapter mListAdapter;


    public TodayFragment() {
    }

    public static TodayFragment newInstance() {
        Log.d(TAG, "entering newInstance()");
        TodayFragment todayFragment = new TodayFragment();

        return todayFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "entering onCreate()");
        super.onCreate(savedInstanceState);

        // Init mSharedPreferences
        mSharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, 0);

        // Determine once in the preferences if the Power Consumption should be shown.
        // Afterwards the boolean mConsumptionEnabled will be used in the code
        mConsumptionEnabled = PreferenceManager.getDefaultSharedPreferences(
                getActivity()).getBoolean("pref_enable_consumption", false);

        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate: savedInstanceState != null");
            // refresh the todayData based on the latest downloaded data
            updateMostRecentData();
        } else {
            Log.d(TAG, "onCreate: savedInstanceState == null");
            updateMostRecentData();
            for (String[] data : todayData) {
                Log.v(TAG, "onCreate - " + data[0] + ", " + data[1] + ", " + data[2]);
            }
        }
    }

    private List<String[]> createTodayDataArray(String rawData) {

        String[] tmpArray = rawData.split(";");
        String[] dayData;
        String consumedPower;
        List<String[]> result = new ArrayList<String[]>();

        // format the date
        for (int i = 0; i < tmpArray.length; i++) {
            dayData = tmpArray[i].split(",");

            // format the date to look nice and store the data we need
            // 1 = Time
            // 2 = Energy Generation
            // 4 = Power Generation
            // 7 = Energy Consumption
            // 8 = Power Consumption
            try {
                String time;
                // format the time based on the preference from the settings page 12h/24h
                if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_ds_time_12h", false)) {
                    time = new SimpleDateFormat("hh:mm aa").format(new SimpleDateFormat("H:mm").parse(dayData[1]));
                } else {
                    time = dayData[1];
                }

                String kWh = formatEnergyValue(dayData[2]);

                // show Power Consumption in list and chart if enabled in the preferences
                // else just show Energy Generation in the list
                if (mConsumptionEnabled) {
                    if (dayData[8].equals("NaN")) {
                        consumedPower = "0";
                    } else {
                        consumedPower = dayData[8];
                    }
                    result.add(new String[]{time, dayData[4], consumedPower});
                } else {
                    result.add(new String[]{time, dayData[4], kWh});
                }

            } catch (NumberFormatException | NullPointerException | ParseException e) {
                Log.d(TAG, "createDailyDataArray - Exception: " + e.getMessage());
            }
        }

        return result;
    }

    private String formatEnergyValue(String input) {
        // if the input value could not be parsed then just return the input value
        String kWh = input;
        try {
            // Format the Watts value to kWh, showing 2 decimals
            float energy = (float) (Integer.parseInt(input)) / 1000;

            Locale locale = new Locale("en", "US");
            DecimalFormat decimalFormat = (DecimalFormat)
                    NumberFormat.getNumberInstance(locale);
            decimalFormat.applyPattern("0.00");

            kWh = decimalFormat.format(energy);

        } catch (NumberFormatException e) {
            Log.d(TAG, "formatEneryValue - Exception: " + e.getMessage());
        }
        return kWh;
    }

    public void updateMostRecentData() {
        Log.d(TAG, "entering updateMostRecentData()");
        String rawData = mSharedPreferences.getString("TODAYDATA", "");

        // TODO verify if this is working
        // If a listadapter exists, the fragment View has been created and we can try to
        // notify the adapter about the new data
        if (mListAdapter != null) {
            todayData = createTodayDataArray(rawData);
            mListAdapter.notifyDataSetChanged();
        } else {
            // No ListAdapter exists yet, this method was called from the onCreate
            todayData = createTodayDataArray(rawData);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "entering onCreateView()");

        View view = inflater.inflate(R.layout.today_fragment_layout, container, false);

        // Change the column header in case the power consumption should be shown
        // It will take the place of the Energy Generation (3rd column)
        if (mConsumptionEnabled) {
            TextView thirdColumn = (TextView) view.findViewById(R.id.thirdColumn);
            thirdColumn.setText(R.string.power);

            TextView subsecondColumn = (TextView) view.findViewById(R.id.subsecondColumn);
            subsecondColumn.setText(getResources().getString(R.string.generated) + " ("
                    + getResources().getString(R.string.pvoutput_power_uom) + ")");

            TextView subthirdColumn = (TextView) view.findViewById(R.id.subthirdColumn);
            subthirdColumn.setText(getResources().getString(R.string.consumed) + " ("
                    + getResources().getString(R.string.pvoutput_power_uom) + ")");
        }

        // build the chart
        mChart = (LineChart) view.findViewById(R.id.todaychart);

        /**
         * The {@link android.widget.ListView} that displays the content that should be refreshed.
         */
        mListView = (ListView) view.findViewById(android.R.id.list);

        try {
            LineData data = getData(todayData);

            setupChart(mChart, data);

        } catch (NullPointerException e) {
            Log.d(TAG, "onCreateView - Exception " + e.getMessage() + ". " + todayData);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /**
         * Create an ArrayAdapter to contain the data for the ListView. Each item in the ListView
         * uses the system-defined simple_list_item_1 layout that contains one TextView.
         */
        mListAdapter = new StringArrayAdapter(
                getActivity(),
                R.layout.row_layout,
                todayData);

        // Set the adapter between the ListView and its backing data.
        mListView.setAdapter(mListAdapter);
    }


    private void setupChart(LineChart chart, LineData data) {

        // enable / disable grid background
        chart.setDrawGridBackground(false);

        chart.setDescription("");
        chart.setNoDataTextDescription("No PVOutput data found");

        // disable touch gestures
        chart.setTouchEnabled(false);

        chart.setScaleXEnabled(true);
        chart.setScaleYEnabled(false);

        chart.getAxisRight().setEnabled(false);

        // Only show the legend in case of multiple lines (power consumption enabled)
        if (mConsumptionEnabled) {
            chart.getLegend().setEnabled(true);
            chart.getLegend().setTextColor(Color.WHITE);
            chart.getLegend().setTextSize(7f);
            chart.getLegend().setFormSize(7f);
            chart.getLegend().setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);
        } else {
            chart.getLegend().setEnabled(false);
        }

        data.setValueTextColor(Color.WHITE);

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);

        // Determine the amount of xAxis labels to skip based on the current number of dataset entries
        int xValCount = data.getXValCount();
        Log.d(TAG, "The mumber of X axis values for the chart is: " + xValCount);
        if (xValCount > 5 && xValCount <= 15) {
            xAxis.setLabelsToSkip(2); // only show labels for every 15 minutes
        } else if (xValCount > 15 && xValCount <= 30) {
            xAxis.setLabelsToSkip(5); // only show labels for every 30 minutes
        } else if (xValCount > 30 && xValCount <= 60) {
            xAxis.setLabelsToSkip(11); // only show labels for every hour
        } else if (xValCount > 60 && xValCount <= 120) {
            xAxis.setLabelsToSkip(23); // only show labels for every 2 hours
        } else if (xValCount > 120) {
            xAxis.setLabelsToSkip(35); // only show labels for every 3 hours
        }

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);

        chart.setData(data);

        chart.invalidate();
    }

    private LineData getData(List<String[]> td) {

        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<Entry> yValsGen = new ArrayList<Entry>();
        ArrayList<Entry> yValsCons = new ArrayList<Entry>();
        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        float power;

        int maxPos = td.size() - 1;
        for (int i = 0; i < td.size(); i++) {
            try {
                xVals.add(td.get(i)[0]);

                // create Entry list for Power Generation
                power = Float.parseFloat(td.get(i)[1]);
                yValsGen.add(new Entry(power, maxPos - i));

                // if Power Consumption should be displayed, create new Entry list
                if (mConsumptionEnabled) {
                    power = Float.parseFloat(td.get(i)[2]);
                    yValsCons.add(new Entry(power, maxPos - i));
                }

            } catch (NumberFormatException | NullPointerException e) {
                Log.d(TAG, "getData - Exception " + e.getMessage() + ". " + Arrays.toString(td.get(i)));
            }
        }

        // Reverse the arrayList to have the most recent time on the right side of the chart
        Collections.reverse(xVals);

        Log.v(TAG, "xVals = " + xVals);
        Log.v(TAG, "yValsGen = " + yValsGen);
        Log.v(TAG, "yValsCons = " + yValsCons);

        // create a LineDataSet for Power Generation
        LineDataSet setGen = new LineDataSet(yValsGen, "Gen.");
        setGen.setDrawCubic(true);
        setGen.setCubicIntensity(0.2f);
        setGen.setLineWidth(1.75f);
        setGen.setDrawCircles(false);
        setGen.setColor(Color.WHITE);
        setGen.setHighLightColor(Color.WHITE);
        setGen.setDrawValues(false);

        if (mConsumptionEnabled) {
            // create a LineDataSet for Power Consumption
            LineDataSet setCons = new LineDataSet(yValsCons, "Cons.");
            setCons.setDrawCubic(true);
            setCons.setCubicIntensity(0.2f);
            setCons.setLineWidth(1.75f);
            setCons.setDrawCircles(false);
            setCons.setColor(getResources().getColor(R.color.accent));
            setCons.setHighLightColor(getResources().getColor(R.color.accent));
            setCons.setDrawValues(false);

            dataSets.add(setCons);
        }

        dataSets.add(setGen);

        // create and return the data object with the datasets
        return new LineData(xVals, dataSets);
    }

}
