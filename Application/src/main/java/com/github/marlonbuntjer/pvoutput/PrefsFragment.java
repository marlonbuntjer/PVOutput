package com.github.marlonbuntjer.pvoutput;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Marlon Buntjer on 30-6-2015.
 */
public class PrefsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);


    }

}
