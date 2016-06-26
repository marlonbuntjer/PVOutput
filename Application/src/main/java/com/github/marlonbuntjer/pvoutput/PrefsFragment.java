package com.github.marlonbuntjer.pvoutput;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

/**
 * Created by Marlon Buntjer on 30-6-2015.
 */
public class PrefsFragment extends PreferenceFragment {

    private static final String PREFS_NAME = "SHAREDPREFS";
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        /**
         * Adds the systemname to the sharedpreference screen in the first category
         * The systemname is retrieved in the {@link com.github.marlonbuntjer.pvoutput.LoadscreenActivity}
         * and stored in the preferences
         */
        SharedPreferences mSharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, 0);
        String systemName = mSharedPreferences.getString("SYSTEMNAME", "");

        if (!systemName.equals("")) {
            Preference customPref = findPreference("pvoutput_system");
            customPref.setTitle("PV Output settings for " + systemName);
        } else {
            Log.d(TAG, "No systemname found to use in sharedpreference screen");
        }
    }

}
