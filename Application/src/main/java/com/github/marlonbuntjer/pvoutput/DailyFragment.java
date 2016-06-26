package com.github.marlonbuntjer.pvoutput;


import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
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
public class DailyFragment extends Fragment {

    private static final String TAG = DailyFragment.class.getSimpleName();
    private static final String PREFS_NAME = "SHAREDPREFS";
    private static List<String[]> dailyData;
    private SharedPreferences mSharedPreferences;
    private boolean mConsumptionEnabled;

    private ListView mListView;
    private StringArrayAdapterDaily mListAdapter;


    public DailyFragment() {
    }

    public static DailyFragment newInstance() {
        Log.d(TAG, "entering newInstance()");
        DailyFragment dailyFragment = new DailyFragment();

        return dailyFragment;
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
            // refresh the todayData based on the latest downloaded
            updateMostRecentData();
        } else {
            Log.d(TAG, "onCreate: savedInstanceState == null");
            updateMostRecentData();
            for (String[] data : dailyData) {
                Log.v(TAG, "onCreate - " + data[0] + ", " + data[1] + ", " + data[2]);
            }
        }
    }

    private List<String[]> createDailyDataArray(String rawData) {

        String[] tmpArray = rawData.split(";");
        String[] dayData;
        List<String[]> result = new ArrayList<String[]>();

        SimpleDateFormat sourceFormattedDate = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat targetFormattedDate = new SimpleDateFormat("dd/MM");

        // format the date
        for (int i = 0; i < tmpArray.length; i++) {
            dayData = tmpArray[i].split(",");

            // format the date to look nice and only store the data we need
            // 0 = Date
            // 1 = Energy Generated
            // 2 = Efficiency
            // 3 = Energy Exported
            // 4 = Energy Used
            // 5 = Peak Power
            // 6 = Peak Time
            // 7 = Condition
            // 8 = Min. Temperature
            // 9 = Max. Temperature
            try {
                String reformattedStr = targetFormattedDate.format(sourceFormattedDate.parse(dayData[0]));

                String generated = formatEnergyValue(dayData[1]);

                // show Power Consumption in list and chart if enabled in the preferences
                // else just show Energy Generation in the list
                if (mConsumptionEnabled) {
                    String used = formatEnergyValue(dayData[4]);
                    result.add(new String[]{reformattedStr, generated, used, dayData[7]});
                } else {
                    result.add(new String[]{reformattedStr, generated, dayData[5], dayData[7]});
                }

            } catch (ParseException | NullPointerException e) {
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
        String rawData = mSharedPreferences.getString("DAILYDATA", "");
        dailyData = createDailyDataArray(rawData);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "entering onCreateView()");

        View view = inflater.inflate(R.layout.daily_fragment_layout, container, false);

        // Change the column header in case the power consumption should be shown
        // It will take the place of the Energy Generation (3rd column)
        if (mConsumptionEnabled) {
            TextView thirdColumn = (TextView) view.findViewById(R.id.thirdColumn);
            thirdColumn.setText(R.string.energy);

            TextView subsecondColumn = (TextView) view.findViewById(R.id.subsecondColumn);
            subsecondColumn.setText(getResources().getString(R.string.generated) + " ("
                    + getResources().getString(R.string.pvoutput_energy_uom) + ")");

            TextView subthirdColumn = (TextView) view.findViewById(R.id.subthirdColumn);
            subthirdColumn.setText(getResources().getString(R.string.consumed) + " ("
                    + getResources().getString(R.string.pvoutput_energy_uom) + ")");
        }

        // build the chart
        CombinedChart mChart = (CombinedChart) view.findViewById(R.id.dailychart);

        /**
         * The {@link android.widget.ListView} that displays the content that should be refreshed.
         */
        mListView = (ListView) view.findViewById(android.R.id.list);

        /**
         * The onItemClickListener is already defined for possible future use
         */
        /*mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String[] temp = (String[]) parent.getItemAtPosition(position);
                Log.d(TAG, "Item clicked at position " + position + " array length = " + temp.length);
                for (int i = 0; i < temp.length; i++) {
                    Log.d(TAG, "value at " + i + " is " + temp[i]);
                }

                Toast.makeText(getContext(), "Showing daily data for " + temp[0], Toast.LENGTH_SHORT).show();
            }
        });*/


        try {

            CombinedData data = new CombinedData(getXValues(dailyData));
            data.setData(getBarData(dailyData));

            if (mConsumptionEnabled) {
                data.setData(getLineData(dailyData));
            }

            setupChart(mChart, data);

        } catch (NullPointerException e) {
            Log.d(TAG, "onCreateView - Exception " + e.getMessage() + ". " + dailyData);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /**
         * Create an ArrayAdapter to contain the data for the ListView.
         */
        mListAdapter = new StringArrayAdapterDaily(
                getActivity(),
                R.layout.row_layout_daily,
                dailyData);

        // Set the adapter between the ListView and its backing data.
        mListView.setAdapter(mListAdapter);
    }

    private void setupChart(CombinedChart chart, CombinedData data) {

        // enable / disable grid background
        chart.setDrawGridBackground(false);

        chart.setDescription("");
        chart.setNoDataTextDescription("No PVOutput data found");
        chart.setTouchEnabled(false);

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
        leftAxis.setSpaceTop(35f);

        chart.setData(data);

        chart.invalidate();
    }

    private ArrayList<String> getXValues(List<String[]> dd) {

        ArrayList<String> xVals = new ArrayList<String>();

        int showNumberOfdays = 30;
        if (dd.size() < 30) {
            showNumberOfdays = dd.size();
        }

        for (int i = 0; i < showNumberOfdays; i++) {
            try {
                xVals.add(dd.get(i)[0]);
            } catch (NullPointerException e) {
                Log.d(TAG, "getXValues - Exception " + e.getMessage() + ". " + Arrays.toString(dd.get(i)));
            }
        }

        // Reverse the arrayList to have the most recent date on the right side of the chart
        Collections.reverse(xVals);

        Log.v(TAG, "xVals = " + xVals);

        return xVals;
    }

    /**
     * Create the BarData set for energy generation
     */
    private BarData getBarData(List<String[]> dd) {

        ArrayList<BarEntry> yValsGen = new ArrayList<BarEntry>();
        float energyGenerated;

        int showNumberOfdays = 30;
        if (dd.size() < 30) {
            showNumberOfdays = dd.size();
        }

        for (int i = 0; i < showNumberOfdays; i++) {
            try {
                energyGenerated = Float.parseFloat(dd.get(i)[1]);
                yValsGen.add(new BarEntry(energyGenerated, (showNumberOfdays - 1) - i));
            } catch (NumberFormatException | NullPointerException e) {
                Log.d(TAG, "getBarData - Exception " + e.getMessage() + ". " + Arrays.toString(dd.get(i)));
            }
        }

        Log.v(TAG, "yValsGen = " + yValsGen);

        // create a dataset and give it a type
        BarDataSet set1 = new BarDataSet(yValsGen, "Gen.");

        set1.setBarSpacePercent(50f);
        set1.setColor(Color.WHITE);
        set1.setHighLightColor(Color.WHITE);
        set1.setDrawValues(false);

        //ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        //dataSets.add(set1);

        BarData d = new BarData();
        d.addDataSet(set1);

        return d;
    }

    /**
     * Create the LineData set for energy consumption
     */
    private LineData getLineData(List<String[]> dd) {

        ArrayList<Entry> yValsCons = new ArrayList<Entry>();
        float energyUsed;

        int showNumberOfdays = 30;
        if (dd.size() < 30) {
            showNumberOfdays = dd.size();
        }

        for (int i = 0; i < showNumberOfdays; i++) {
            try {
                energyUsed = Float.parseFloat(dd.get(i)[2]);
                yValsCons.add(new Entry(energyUsed, (showNumberOfdays - 1) - i));
            } catch (NumberFormatException | NullPointerException e) {
                Log.d(TAG, "getLineData - Exception " + e.getMessage() + ". " + Arrays.toString(dd.get(i)));
            }
        }

        Log.v(TAG, "yValsCons = " + yValsCons);


        // create a LineDataSet for Energy Consumption
        LineDataSet setCons = new LineDataSet(yValsCons, "Cons.");
        setCons.setDrawCubic(true);
        setCons.setCubicIntensity(0.2f);
        setCons.setLineWidth(1.75f);
        setCons.setDrawCircles(false);
        setCons.setColor(ContextCompat.getColor(getContext(), R.color.accent));
        setCons.setHighLightColor(ContextCompat.getColor(getContext(), R.color.accent));
        setCons.setDrawValues(false);


        LineData d = new LineData();

        d.addDataSet(setCons);

        return d;
    }


}
