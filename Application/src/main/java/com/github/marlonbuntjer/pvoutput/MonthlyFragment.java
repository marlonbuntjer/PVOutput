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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by Marlon Buntjer on 10-6-2015.
 */
public class MonthlyFragment extends Fragment {

    private static final String TAG = MonthlyFragment.class.getSimpleName();
    private static final String PREFS_NAME = "SHAREDPREFS";
    private static List<String[]> monthlyData;
    private SharedPreferences mSharedPreferences;
    private boolean mConsumptionEnabled;

    private ListView mListView;
    private StringArrayAdapter mListAdapter;


    public MonthlyFragment() {
    }

    public static MonthlyFragment newInstance() {
        Log.d(TAG, "entering newInstance()");
        MonthlyFragment monthlyFragment = new MonthlyFragment();

        return monthlyFragment;
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
            // refresh the todayData based on the latest downloaded data in the bundle
            updateMostRecentData();
        } else {
            Log.d(TAG, "onCreate: savedInstanceState == null");
            updateMostRecentData();
            for (String[] data : monthlyData) {
                Log.v(TAG, "onCreate - " + data[0] + ", " + data[1] + ", " + data[2] + ", " + data[3]);
            }
        }
    }


    private List<String[]> createMonthlyDataArray(String rawData) {

        String[] tmpArray = rawData.split(";");
        String[] monthData;
        List<String[]> result = new ArrayList<String[]>();

        SimpleDateFormat sourceFormattedDate = new SimpleDateFormat("yyyyMM");
        SimpleDateFormat targetFormattedDate = new SimpleDateFormat("MMMM yyyy", Locale.US);

        // format the date
        for (int i = 0; i < tmpArray.length; i++) {
            monthData = tmpArray[i].split(",");

            // format the date to look nice and only store the data we need
            // 0 = Date
            // 1 = Outputs
            // 2 = Energy Generated
            // 3 = Efficiency
            // 4 = Energy Exported
            // 5 = Energy Used
            try {
                String formattedMonth = targetFormattedDate.format(sourceFormattedDate.parse(monthData[0]));

                String generated = formatEnergyValue(monthData[2]);

                // show Power Consumption in list and chart if enabled in the preferences
                // else just show Energy Generation in the list
                if (mConsumptionEnabled) {
                    String used = formatEnergyValue(monthData[5]);
                    result.add(new String[]{formattedMonth, generated, used, monthData[0]});
                } else {
                    // The StringArrayAdapter will only show the data at position 0, 1 and 2
                    // the monthData[0] is added so this List can be reused for the chart using a different
                    // date formatting
                    result.add(new String[]{formattedMonth, "", generated, monthData[0]});
                }
            } catch (NullPointerException | ParseException e) {
                Log.d(TAG, "createMonthlyDataArray - Exception: " + e.getMessage());
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
        String rawData = mSharedPreferences.getString("MONTHLYDATA", "");
        monthlyData = createMonthlyDataArray(rawData);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "entering onCreateView()");

        View view = inflater.inflate(R.layout.monthly_fragment_layout, container, false);

        // Change the column header in case the power consumption should be shown
        // It will take the place of the Energy Generation (3rd column)
        if (mConsumptionEnabled) {
            TextView secondColumn = (TextView) view.findViewById(R.id.secondColumn);
            secondColumn.setText(R.string.energy);

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
        BarChart mChart = (BarChart) view.findViewById(R.id.monthlychart);

        /**
         * The {@link android.widget.ListView} that displays the content that should be refreshed.
         */
        mListView = (ListView) view.findViewById(android.R.id.list);

        try {
            BarData data = getData(monthlyData);
            setupChart(mChart, data);

        } catch (NullPointerException e) {
            Log.d(TAG, "onCreateView - Exception " + e.getMessage() + ". " + monthlyData);
        }

        return view;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /**
         * Create an ArrayAdapter to contain the data for the ListView. Each item in the ListView
         * uses the system-defined simple_list_item_1 layout that contains one TextView.
         */
        mListAdapter = new StringArrayAdapter(
                getActivity(),
                R.layout.row_layout_monthly,
                monthlyData);

        // Set the adapter between the ListView and its backing data.
        mListView.setAdapter(mListAdapter);
    }


    private void setupChart(BarChart chart, BarData data) {

        // enable / disable grid background
        chart.setDrawGridBackground(false);

        chart.setDescription("");
        chart.setNoDataTextDescription("No PVOutput data found");
        chart.setTouchEnabled(false);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);

        data.setValueTextColor(Color.WHITE);

        // Only show the legend in case of multiple datasets
        if (data.getDataSetCount() > 1) {
            chart.getLegend().setEnabled(true);
            chart.getLegend().setTextColor(Color.WHITE);
            chart.getLegend().setTextSize(7f);
            chart.getLegend().setFormSize(7f);
            chart.getLegend().setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);
        } else {
            chart.getLegend().setEnabled(false);
        }

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelsToSkip(0);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);

        chart.setData(data);

        chart.invalidate();
    }

    private BarData getData(List<String[]> md) {

        ArrayList<String> xVals = new ArrayList<String>(
                Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"));
        ArrayList<BarEntry> yValsCurrentYear = new ArrayList<BarEntry>();
        ArrayList<BarEntry> yValsPreviousYear = new ArrayList<BarEntry>();

        float energy;
        String yearMonth;

        String latestYearMonth = md.get(0)[3];
        int latestYear = Integer.parseInt(latestYearMonth.substring(0, 4));
        int previousYear = latestYear - 1;
        int latestMonth = Integer.parseInt(latestYearMonth.substring(4, 6));

        int monthsAvailable = md.size();
        int maxIterations;
        int monthBucket;
        int yearBucket;

        // populate dataset for the latest year. Either run the for loop on all months of the year
        // when they are all available, or only on the number of months that are in the dataset
        // for newer pvouptut.org users
        if (monthsAvailable >= (latestMonth + 12)) {
            maxIterations = (latestMonth + 12);
        } else {
            maxIterations = monthsAvailable;
        }

        for (int i = 0; i < maxIterations; i++) {
            // if consumption display is enabled, the data is put on index 1
            // to be consistent with the layout of the today fragment
            if (mConsumptionEnabled) {
                energy = Float.parseFloat(md.get(i)[1]);
            } else {
                energy = Float.parseFloat(md.get(i)[2]);
            }
            yearMonth = md.get(i)[3];
            monthBucket = Integer.parseInt(yearMonth.substring(4, 6)) - 1;
            yearBucket = Integer.parseInt(yearMonth.substring(0, 4));
            if (yearBucket == latestYear) {
                yValsCurrentYear.add(new BarEntry(energy, monthBucket));
            } else if (yearBucket == previousYear) {
                yValsPreviousYear.add(new BarEntry(energy, monthBucket));
            }
        }

        Log.v(TAG, "latestYear = " + latestYear);
        Log.v(TAG, "latestMonth = " + latestMonth);
        Log.v(TAG, "monthsAvailable = " + monthsAvailable);
        Log.v(TAG, "maxIterations = " + maxIterations);

        Log.v(TAG, "xVals = " + xVals);
        Log.v(TAG, "yValsCurrentYear = " + yValsCurrentYear);
        Log.v(TAG, "yValsPreviousYear = " + yValsPreviousYear);

        // create a dataset and give it a label
        BarDataSet set1 = new BarDataSet(yValsCurrentYear, String.valueOf(latestYear));

        set1.setBarSpacePercent(25f);
        set1.setColor(Color.WHITE);
        set1.setHighLightColor(Color.WHITE);
        set1.setDrawValues(false);

        ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        dataSets.add(set1);

        // only create and add the dataset for the previous year if it contains values
        if (yValsPreviousYear.size() > 0) {
            BarDataSet set2 = new BarDataSet(yValsPreviousYear, String.valueOf(latestYear - 1));

            set2.setBarSpacePercent(80f);
            set2.setColor(getResources().getColor(R.color.black));
            set2.setHighLightColor(getResources().getColor(R.color.black));
            set2.setDrawValues(false);

            dataSets.add(set2);
        }

        BarData data = new BarData(xVals, dataSets);

        return data;
    }

}
