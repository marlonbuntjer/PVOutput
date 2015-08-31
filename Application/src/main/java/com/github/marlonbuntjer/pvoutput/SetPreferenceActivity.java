package com.github.marlonbuntjer.pvoutput;

/**
 * Created by Marlon Buntjer on 1-7-2015.
 */

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

public class SetPreferenceActivity extends AppCompatActivity {

    private static final String TAG = SetPreferenceActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(android.R.id.content, new PrefsFragment());
        transaction.commit();

        // enable the back button

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
            Log.d(TAG, "Could not get the SupportActionBar to set the home button");
        }
    }

    /**
     * Let's the user tap the back button.
     * Requires setHomeButtonEnabled() in onCreate().
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                // goto parent activity.
                this.finish();
                return true;
        }
        return (super.onOptionsItemSelected(menuItem));
    }

}
