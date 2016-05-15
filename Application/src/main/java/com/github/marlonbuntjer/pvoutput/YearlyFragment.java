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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Marlon Buntjer on 10-6-2015.
 */
public class YearlyFragment extends Fragment {

    private static final String TAG = YearlyFragment.class.getSimpleName();
    private static final String PREFS_NAME = "SHAREDPREFS";
    private static List<String[]> yearlyData;
    private SharedPreferences mSharedPreferences;
    private boolean mConsumptionEnabled;

    private ListView mListView;
    private StringArrayAdapter mListAdapter;


    public YearlyFragment() {
    }

    public static YearlyFragment newInstance() {
        Log.d(TAG, "entering newInstance()");
        YearlyFragment yearlyFragment = new YearlyFragment();

        return yearlyFragment;
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
            for (String[] data : yearlyData) {
                Log.v(TAG, "onCreate - " + data[0] + ", " + data[1] + ", " + data[2] + ", " + data[3]);
            }
        }
    }


    private List<String[]> createYearlyDataArray(String rawData) {

        String[] splittedDataArray = rawData.split(";");
        String[] yearData;
        List<String[]> result = new ArrayList<String[]>();

        SimpleDateFormat sourceFormattedDate = new SimpleDateFormat("yyyyMM");
        SimpleDateFormat targetFormattedDate = new SimpleDateFormat("MMMM yyyy", Locale.US);

        // format the date
        for (int i = 0; i < splittedDataArray.length; i++) {
            yearData = splittedDataArray[i].split(",");

            // format the date to look nice and only store the data we need
            // 0 = Year
            // 1 = number of Outputs
            // 2 = Energy Generated
            // 3 = Efficiency
            // 4 = Energy Exported
            // 5 = Energy Used

            //TODO catch ArrayIndexOutOfBounds exception
            String year = yearData[0];
            String generated = formatEnergyValue(yearData[2]);

            // show Power Consumption in list and chart if enabled in the preferences
            // else just show Energy Generation in the list
            if (mConsumptionEnabled) {
                String used = formatEnergyValue(yearData[5]);
                result.add(new String[]{year, generated, used, yearData[0]});
            } else {
                // The StringArrayAdapter will only show the data at position 0, 1 and 2
                // the monthData[0] is added so this List can be reused for the chart using a different
                // date formatting
                result.add(new String[]{year, "", generated, yearData[0]});
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
            decimalFormat.applyPattern("0");

            kWh = decimalFormat.format(energy);

        } catch (NumberFormatException e) {
            Log.d(TAG, "formatEnergyValue - Exception: " + e.getMessage());
        }
        return kWh;
    }

    public void updateMostRecentData() {
        Log.d(TAG, "entering updateMostRecentData()");
        // get the most recently stored data and put it in a nice list for displaying
        String rawData = mSharedPreferences.getString("YEARLYDATA", "");
        Log.d(TAG, "Raw Yearly data is: " + rawData);
        yearlyData = createYearlyDataArray(rawData);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "entering onCreateView()");

        View view = inflater.inflate(R.layout.yearly_fragment_layout, container, false);

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
        BarChart mChart = (BarChart) view.findViewById(R.id.yearlychart);

        /**
         * The {@link android.widget.ListView} that displays the content that should be refreshed.
         */
        mListView = (ListView) view.findViewById(android.R.id.list);

        try {
            BarData data = getData(yearlyData);
            setupChart(mChart, data);

        } catch (NullPointerException e) {
            Log.d(TAG, "onCreateView - Exception " + e.getMessage() + ". " + yearlyData);
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
                R.layout.row_layout_yearly,
                yearlyData);

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

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelsToSkip(0);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinValue(0f);

        chart.setData(data);

        chart.invalidate();
    }

    private BarData getData(List<String[]> dataset) {

        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<BarEntry> yVals = new ArrayList<BarEntry>();
        float energy;
        int numberOfYearsAvailable = dataset.size();
        int maxIterations;

        // Show up till maximum 25 years
        if (numberOfYearsAvailable > 25) {
            maxIterations = 25;
        } else {
            maxIterations = numberOfYearsAvailable;
        }

        for (int i = 0; i < maxIterations; i++) {
            // if consumption display is enabled, the data is put on index 1
            // to be consistent with the layout of the today fragment
            if (mConsumptionEnabled) {
                energy = Float.parseFloat(dataset.get(i)[1]);
            } else {
                energy = Float.parseFloat(dataset.get(i)[2]);
            }
            xVals.add(dataset.get(i)[0]);
            yVals.add(new BarEntry(energy, i));
        }

        Log.v(TAG, "numberOfYearsAvailable = " + numberOfYearsAvailable);
        Log.v(TAG, "maxIterations = " + maxIterations);
        Log.v(TAG, "yVals = " + yVals);

        // create a dataset and give it a label
        BarDataSet set1 = new BarDataSet(yVals, "Yearly");

        set1.setBarSpacePercent(25f);
        set1.setColor(Color.WHITE);
        set1.setHighLightColor(Color.WHITE);
        set1.setDrawValues(false);

        ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
        dataSets.add(set1);

        BarData data = new BarData(xVals, dataSets);

        return data;
    }

}
