package com.github.marlonbuntjer.pvoutput;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Created by Marlon Buntjer on 2-6-2015.
 */
public class LifetimeFragment extends Fragment {

    private static final String TAG = LifetimeFragment.class.getSimpleName();
    private static final String PREFS_NAME = "SHAREDPREFS";
    private static List<String[]> lifetimeData;
    private String energyGenerated;
    private TextView energygeneration_value;
    private SharedPreferences mSharedPreferences;
    private ListView mListView;
    private StringArrayAdapter mListAdapter;


    public LifetimeFragment() {
    }

    public static LifetimeFragment newInstance() {
        Log.d(TAG, "entering newInstance()");
        LifetimeFragment lifetimeFragment = new LifetimeFragment();

        return lifetimeFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "entering onCreate()");
        super.onCreate(savedInstanceState);

        // Init mSharedPreferences
        mSharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, 0);

        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate: savedInstanceState != null");
            updateMostRecentData();
        } else {
            Log.d(TAG, "onCreate: savedInstanceState == null");

            updateMostRecentData();
            for (String[] data : lifetimeData) {
                Log.v(TAG, "onCreate - " + data[0] + ", " + data[1] + ", " + data[2]);
            }
        }
    }

    private List<String[]> createLifetimeDataArray(String rawData) {

        String[] lifetimeData = rawData.split(",");
        // 0 = Energy Generated
        // 1 = Energy Exported
        // 2 = Average Generation
        // 3 = Minimum Generation
        // 4 = Maximum Generation
        // 5 = Average Efficiency
        // 6 = Outputs
        // 7 = Actual Date From
        // 8 = Actual Date To
        // 9 = Record Efficiency
        // 10 = Record Date

        // set the power values for displaying in the fragment header
        energyGenerated = formatEnergyValueMwh(lifetimeData[0]);

        List<String[]> result = new ArrayList<String[]>();

        SimpleDateFormat sourceFormattedDate = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat targetFormattedDate = new SimpleDateFormat("dd/MM/yyyy");

        try {
            result.add(new String[]{"Energy Exported", formatEnergyValue(lifetimeData[1]), "kWh"});
            result.add(new String[]{"Average Generation", formatEnergyValue(lifetimeData[2]), "kWh"});
            result.add(new String[]{"Minimum Generation", formatEnergyValue(lifetimeData[3]), "kWh"});
            result.add(new String[]{"Maximum Generation", formatEnergyValue(lifetimeData[4]), "kWh"});
            result.add(new String[]{"Average Efficiency", lifetimeData[5], "kWh/kW"});
            result.add(new String[]{"Outputs", lifetimeData[6], ""});
            result.add(new String[]{"Actual Date From",
                    targetFormattedDate.format(sourceFormattedDate.parse(lifetimeData[7])), ""});
            result.add(new String[]{"Actual Date To",
                    targetFormattedDate.format(sourceFormattedDate.parse(lifetimeData[8])), ""});
            result.add(new String[]{"Record Efficiency", lifetimeData[9], "kWh/kW"});
            result.add(new String[]{"Record Date",
                    targetFormattedDate.format(sourceFormattedDate.parse(lifetimeData[10])), ""});

        } catch (NumberFormatException | NullPointerException | ParseException e) {
            Log.d(TAG, "createLifetimeDataArray - Exception: " + e.getMessage());
        }

        return result;
    }

    private String formatEnergyValue(String input) {
        String value;
        // The value NaN will be returned where a numeric value does not exist
        if (input.equals("NaN")) {
            value = "-";
        } else {
            // if the input value could not be parsed then just return the input value
            value = input;
            try {
                // Format the Watts value to kWh, showing 2 decimals
                float energy = (float) (Integer.parseInt(input)) / 1000;
                Locale locale = new Locale("en", "US");
                DecimalFormat decimalFormat = (DecimalFormat)
                        NumberFormat.getNumberInstance(locale);
                decimalFormat.applyPattern("0.00");
                value = decimalFormat.format(energy);
            } catch (NumberFormatException e) {
                Log.d(TAG, "formatEnergyValue - Exception: " + e.getMessage());
            }
        }
        return value;
    }

    private String formatEnergyValueMwh(String input) {
        // if the input value could not be parsed then just return the input value
        String value = input;
        try {
            // Format the Watts value to MWh, showing three decimals
            float energy = (float) (Integer.parseInt(input)) / 1000000;
            Locale locale = new Locale("en", "US");
            DecimalFormat decimalFormat = (DecimalFormat)
                    NumberFormat.getNumberInstance(locale);
            decimalFormat.applyPattern("0.000");
            value = decimalFormat.format(energy);
        } catch (NumberFormatException e) {
            Log.d(TAG, "formatEnergyValueMwh - Exception: " + e.getMessage());
        }
        return value;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.lifetime_fragment_layout, container, false);

        /**
         * The {@link ListView} that displays the content that should be refreshed.
         */
        mListView = (ListView) view.findViewById(android.R.id.list);

        energygeneration_value = (TextView) view.findViewById(R.id.energygeneration_value);
        energygeneration_value.setText(energyGenerated);

        return view;
    }

    public void updateMostRecentData() {
        Log.d(TAG, "entering updateMostRecentData()");

        String rawData = mSharedPreferences.getString("LIFETIMEDATA", "");
        lifetimeData = createLifetimeDataArray(rawData);
    }

    private void updateTotalEnergyInView(String[] values, String rawData) {
        try {
            energygeneration_value.setText(formatEnergyValueMwh(values[0])); // Total Energy
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // Possibly  an incomplete resultset was retrieved from pvoutput.
            Log.d(TAG, "updateTotalEnergyInView - Exception:" + e.getMessage());
            Log.d(TAG, "updateTotalEnergyInView - pvoutput data was:" + rawData);
        }
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
                R.layout.live_row_layout,
                lifetimeData);

        // Set the adapter between the ListView and its backing data.
        mListView.setAdapter(mListAdapter);
    }


}
