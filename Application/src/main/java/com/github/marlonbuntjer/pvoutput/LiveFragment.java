package com.github.marlonbuntjer.pvoutput;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
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
public class LiveFragment extends Fragment {

    private static final String TAG = LiveFragment.class.getSimpleName();
    private static final String PREFS_NAME = "SHAREDPREFS";
    private static List<String[]> liveData;
    private static String lastUpdateTime = "---";
    private String powerGeneration, powerConsumption;
    private TextView powergeneration_value, powerconsumption_value, lastupdated;
    private SharedPreferences mSharedPreferences;
    private ListView mListView;
    private StringArrayAdapter mListAdapter;
    private boolean mConsumptionEnabled;


    public LiveFragment() {
    }

    public static LiveFragment newInstance() {
        Log.d(TAG, "entering newInstance()");
        return new LiveFragment();
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
            updateMostRecentData();
        } else {
            Log.d(TAG, "onCreate: savedInstanceState == null");

            updateMostRecentData();
            for (String[] data : liveData) {
                Log.v(TAG, "onCreate - " + data[0] + ", " + data[1] + ", " + data[2]);
            }
        }
    }

    private List<String[]> createLiveDataArray(String rawData) {

        String[] liveData;

        // if no data is downloaded, use an initial dataset
        // most likely cause of no data is when someone doesn't upload live data, but only daily
        if (rawData == null || rawData.equals("")) {
            liveData = "00000000,00:00,0,0,0,0,0,0,0,0,0".split(",");
        } else {
            liveData = rawData.split(",");
            // 0 = Date
            // 1 = Time
            // 2 = Energy Generation
            // 3 = Energy Efficiency
            // 4 = Instantaneous Power
            // 5 = Average Power
            // 6 = Normalised Output
            // 7 = Energy Consumption
            // 8 = Power Consumption
            // 9 = Temperature
            // 10 = Voltage
        }

        // set the power values for displaying in the fragment header
        powerGeneration = liveData[4];
        if (liveData[8].equals("NaN")) {
            powerConsumption = "0";
        } else {
            powerConsumption = liveData[8];
        }

        List<String[]> result = new ArrayList<String[]>();

        try {
            result.add(new String[]{"Energy Generation", formatEnergyValue(liveData[2]), "kWh"});
            if (mConsumptionEnabled) {
                result.add(new String[]{"Energy Consumption", formatEnergyValue(liveData[7]), "kWh"});
            }
            result.add(new String[]{"Efficiency", liveData[3], "kWh/kW"});
            result.add(new String[]{"Normalised", liveData[6], "kW/kW"});
            result.add(new String[]{"Temperature", liveData[9], " \u2103"});
            result.add(new String[]{"Voltage", String.format("%4.0f", Double.parseDouble(liveData[10])), "Volts"}); // Voltage

            // format the time based on the preference from the settings page 12h/24h
            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_ds_time_12h", false)) {
                lastUpdateTime = new SimpleDateFormat("hh:mm aa").format(new SimpleDateFormat("H:mm").parse(liveData[1]));
            } else {
                lastUpdateTime = liveData[1];
            }

        } catch (NumberFormatException | NullPointerException | ParseException e) {
            Log.d(TAG, "createLiveDataArray - Exception: " + e.getMessage());
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

        View view = inflater.inflate(R.layout.live_fragment_layout, container, false);

        /**
         * The {@link android.widget.ListView} that displays the content that should be refreshed.
         */
        mListView = (ListView) view.findViewById(android.R.id.list);

        powergeneration_value = (TextView) view.findViewById(R.id.powergeneration_value);
        powergeneration_value.setText(powerGeneration);

        powerconsumption_value = (TextView) view.findViewById(R.id.powerconsumption_value);
        powerconsumption_value.setText(powerConsumption);

        lastupdated = (TextView) view.findViewById(R.id.lastupdated);
        lastupdated.setText("Last updated: " + lastUpdateTime);

        // don't show the power consumption view group if the preference setting is switched off
        if (!mConsumptionEnabled) {
            RelativeLayout rlCons = (RelativeLayout) view.findViewById(R.id.fragmentheader_cons);
            rlCons.setVisibility(View.GONE);

            // if no consumption data is displayed, move the generation value more to the right
            RelativeLayout rlGen = (RelativeLayout) view.findViewById(R.id.fragmentheader_gen);
            rlGen.setPadding(
                    rlGen.getPaddingLeft() + (int) getResources().getDimension(R.dimen.margin_large),
                    rlGen.getPaddingLeft(),
                    rlGen.getPaddingLeft(),
                    rlGen.getPaddingLeft());

            // show full title
            TextView powergeneration = (TextView) view.findViewById(R.id.powergeneration);
            powergeneration.setText(getResources().getText(R.string.pvoutput_power_generation));
        }

        return view;
    }

    public void updateMostRecentData() {
        Log.d(TAG, "entering updateMostRecentData()");

        String rawData = mSharedPreferences.getString("LIVEDATA", "");
        liveData = createLiveDataArray(rawData);
    }

    private void updateTotalEnergyInView(String[] values, String rawData) {
        try {
            powergeneration_value.setText(formatEnergyValueMwh(values[0])); // Total Energy
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
                liveData);

        // Set the adapter between the ListView and its backing data.
        mListView.setAdapter(mListAdapter);
    }


}
