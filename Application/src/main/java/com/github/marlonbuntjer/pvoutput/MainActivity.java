/*
 * Copyright 2015 The PV Ouput for Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.marlonbuntjer.pvoutput;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.Date;
import java.util.List;


/**
 * My first PVOutput application able to connect to the pvoutput service api and fetch raw
 * HTML. It uses AsyncTask to do the fetch on a background thread. To establish
 * the network connection, it uses HttpURLConnection.
 */

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SHAREDPREFS";
    private static final String TAG = MainActivity.class.getSimpleName();

    // least amount of seconds before data can be refreshed
    private static long REFRESHTIMEOUT = 300;


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * three primary sections of the app. We use a {@link android.support.v4.app.FragmentPagerAdapter}
     * derivative, which will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private AppSectionsPagerAdapter mAppSectionsPagerAdapter;
    /**
     * The {@link ViewPager} that will display the four primary sections of the app, one at a
     * time.
     */
    private ViewPager mViewPager;
    private LiveFragment liveFragment = null;
    private TodayFragment todayFragment = null;
    private DailyFragment dailyFragment = null;
    private MonthlyFragment monthlyFragment = null;
    private LifetimeFragment lifetimeFragment = null;
    private String refreshedLiveData, refreshedTodayData, refreshedDailyData, refreshedMonthlyData, refreshedLifetimeData;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences mDefaultSharedPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getSharedPreferences(PREFS_NAME, 0);
        Log.d(TAG, "liveData = " + mSharedPreferences.getString("LIVEDATA", ""));
        Log.d(TAG, "todayData = " + mSharedPreferences.getString("TODAYDATA", ""));
        Log.d(TAG, "dailyData = " + mSharedPreferences.getString("DAILYDATA", ""));
        Log.d(TAG, "monthlyData = " + mSharedPreferences.getString("MONTHLYDATA", ""));
        Log.d(TAG, "lifetimeData = " + mSharedPreferences.getString("LIFETIMEDATA", ""));

        mDefaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (mDefaultSharedPrefs.getBoolean("pref_reduced_refreshtimeout", false)) {
            REFRESHTIMEOUT = 60;
        } else {
            REFRESHTIMEOUT = 300;
        }
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Log.d(TAG, "PreferenceChanged: " + key);
                // Set minimum refreshtime to 60 seconds for people who donate to pvoutput
                // The amount of API requests per system is increased to 300 per hour (from 60).
                // each app refresh will generate 5 api calls.
                if (sharedPreferences.getBoolean("pref_reduced_refreshtimeout", false)) {
                    REFRESHTIMEOUT = 60;
                } else {
                    REFRESHTIMEOUT = 300;
                }
            }
        };
        mDefaultSharedPrefs.registerOnSharedPreferenceChangeListener(listener);

        // Create the adapter that will return a fragment for each of the five primary sections
        // of the app.
        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);


        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);

        tabLayout.setTabTextColors(getResources().getColor(R.color.tab_text_unsel),
                getResources().getColor(R.color.tab_text_sel));
        tabLayout.setTabMode(TabLayout.MODE_FIXED); // distributes tabs width evenly
        tabLayout.setupWithViewPager(mViewPager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDefaultSharedPrefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_refresh:
                initiateRefresh();
                return true;
            case R.id.menu_settings:
                showPrefsFragment();
                return true;
            case R.id.menu_about:
                showAboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void showPrefsFragment() {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, SetPreferenceActivity.class);

        startActivity(intent);
    }


    protected void showAboutDialog() {
        // Inflate the about message contents
        View view = getLayoutInflater().inflate(R.layout.about, null, false);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(R.string.app_name);
        builder.setView(view);
        builder.setPositiveButton("OK", null);

        //creating an alert dialog from our builder.
        AlertDialog dialog = builder.create();
        dialog.show();

        Button positive_button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

        if (positive_button != null) {
            positive_button.setTextColor(getResources().getColor(R.color.accent));
        }
    }

    // Added override OnBackPressed because addToBackStack for showDialog wasn't working
    @Override
    public void onBackPressed() {
        Log.d(TAG, "BackStackEntryCount = " + getFragmentManager().getBackStackEntryCount());
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void updateFragments() {
        Log.d(TAG, "Received dataRefreshCompleteNotification");

        liveFragment = (LiveFragment) mAppSectionsPagerAdapter.getRegisteredFragment(0);
        if (liveFragment != null) {
            liveFragment.updateMostRecentData();
            Log.d(TAG, "updateMostRecentData for liveFragment");
        }

        todayFragment = (TodayFragment) mAppSectionsPagerAdapter.getRegisteredFragment(1);
        if (todayFragment != null) {
            todayFragment.updateMostRecentData();
            Log.d(TAG, "updateMostRecentData for todayFragment");
        }

        dailyFragment = (DailyFragment) mAppSectionsPagerAdapter.getRegisteredFragment(2);
        if (dailyFragment != null) {
            dailyFragment.updateMostRecentData();
            Log.d(TAG, "updateMostRecentData for dailyFragment");
        }

        monthlyFragment = (MonthlyFragment) mAppSectionsPagerAdapter.getRegisteredFragment(3);
        if (monthlyFragment != null) {
            monthlyFragment.updateMostRecentData();
            Log.d(TAG, "updateMostRecentData for monthlyFragment");
        }

        lifetimeFragment = (LifetimeFragment) mAppSectionsPagerAdapter.getRegisteredFragment(4);
        if (lifetimeFragment != null) {
            lifetimeFragment.updateMostRecentData();
            Log.d(TAG, "updateMostRecentData for lifetimeFragment");
        }

        //this line will force all pages to be loaded fresh when changing between fragments
        mAppSectionsPagerAdapter.notifyDataSetChanged();
    }

    // method to check if the user is spamming refresh and
    // thus burning up the max 60 api calls per hour
    private boolean allowRefresh() {
        long refreshTimeoutInMillis = REFRESHTIMEOUT * 1000;
        long lastRefreshTime = mSharedPreferences.getLong("REFRESHTIME", 0L);

        if (lastRefreshTime + refreshTimeoutInMillis < System.currentTimeMillis()) {
            Log.d(TAG, "allowRefresh - allowed (min " + REFRESHTIMEOUT + " sec). Last refreshed at: "
                    + lastRefreshTime + " (" + new Date(lastRefreshTime).toString() + ")");
            return true;
        } else {
            Log.d(TAG, "allowRefresh - not allowed (min " + REFRESHTIMEOUT + " sec). Last refreshed at: "
                    + lastRefreshTime + " (" + new Date(lastRefreshTime).toString() + ")");
            return false;
        }
    }

    // Starting point for reloading the latest data from pvoutput.org
    private void initiateRefresh() {
        Log.i(TAG, "initiateRefresh");
        /**
         * Execute the background task, which uses {@link android.os.AsyncTask} to load the data.
         */
        if (allowRefresh()) {
            showProgressDialog();
            PVOutputApiUrls urls = new PVOutputApiUrls(this);
            List<String> urlList = urls.getData();

            new DownloadDataTask().execute(urlList.get(0), urlList.get(1), urlList.get(2), urlList.get(3), urlList.get(4));
        } else {
            Toast toast = Toast.makeText(this, R.string.too_recently_refreshed_message, Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void showProgressDialog() {
        mProgress = new ProgressDialog(this,
                R.style.AppTheme_Light_Dialog);
        mProgress.setIndeterminate(true);
        mProgress.setMessage(getString(R.string.loading_message));
        mProgress.show();
    }

    /**
     * When the AsyncTask finishes, it calls onRefreshComplete(), which updates the data in the
     * ListAdapter and turns off the mProgress bar.
     */
    private void onRefreshComplete() {
        Log.d(TAG, "onRefreshComplete");

        // when the latest data is loaded, put it in the sharedpreferences
        // and overwrite the previous data
        mSharedPreferences = this.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("LIVEDATA", refreshedLiveData);
        editor.putString("TODAYDATA", refreshedTodayData);
        editor.putString("DAILYDATA", refreshedDailyData);
        editor.putString("MONTHLYDATA", refreshedMonthlyData);
        editor.putString("LIFETIMEDATA", refreshedLifetimeData);
        editor.putLong("REFRESHTIME", System.currentTimeMillis());

        // Commit the edits!
        editor.commit();

        // hide the mProgress dialog
        mProgress.dismiss();

        // update the fragments in the viewpager with the latest data
        updateFragments();
    }

    public void onRefreshFailed() {
        // not connected to the network
        mProgress.dismiss();
        Toast toast = Toast.makeText(this, R.string.loading_failed_message, Toast.LENGTH_LONG);
        toast.show();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     * Using SmartFragmentStatePagerAdapter taken from
     * https://guides.codepath.com/android/ViewPager-with-FragmentPagerAdapter
     */
    public class AppSectionsPagerAdapter extends SmartFragmentStatePagerAdapter {

        public AppSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(TAG, "getItem:" + position);
            Bundle args;
            switch (position) {
                case 0:
                    liveFragment = LiveFragment.newInstance();
                    return liveFragment;
                case 1:
                    todayFragment = TodayFragment.newInstance();
                    return todayFragment;
                case 2:
                    dailyFragment = DailyFragment.newInstance();
                    return dailyFragment;
                case 3:
                    monthlyFragment = MonthlyFragment.newInstance();
                    return monthlyFragment;
                case 4:
                    lifetimeFragment = LifetimeFragment.newInstance();
                    return lifetimeFragment;
                default:
                    // The position should always be 0 to 4.
                    // If not, just present the LIVE fragment
                    liveFragment = LiveFragment.newInstance();
                    return liveFragment;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            CharSequence pageTitle;
            switch (position) {
                case 0:
                    pageTitle = "Live";
                    break;
                case 1:
                    pageTitle = "Today";
                    break;
                case 2:
                    pageTitle = "Daily";
                    break;
                case 3:
                    pageTitle = "Monthly";
                    break;
                case 4:
                    pageTitle = "Lifetime";
                    break;
                default:
                    pageTitle = "Live " + (position);
            }
            return pageTitle;
        }

        // When new data is loaded, it can be shown in all fragments except
        // the cached pageradapter fragments which is the current and the next fragment.
        // To overcome this the method notifyDataSetChanged is called when new data is
        // loaded which makes the pageradapter call getItemPosition. Because it returns
        // POSITION_NONE it will recreate the fragment (with the latest data) instead of using
        // the cached version with the old data
        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    /**
     * Implementation of AsyncTask, to fetch the data in the background away from
     * the UI thread.
     */
    private class DownloadDataTask extends AsyncTask<String, Void, String> {

        boolean refreshfailed = false;

        @Override
        protected String doInBackground(String... urls) {
            Log.d(TAG, "DownloadDataTask - doInBackground");
            Downloader downloader = new Downloader();
            try {
                refreshedLiveData = downloader.loadFromNetwork(urls[0]);
                refreshedTodayData = downloader.loadFromNetwork(urls[1]);
                refreshedDailyData = downloader.loadFromNetwork(urls[2]);
                refreshedMonthlyData = downloader.loadFromNetwork(urls[3]);
                refreshedLifetimeData = downloader.loadFromNetwork(urls[4]);
            } catch (IOException | PVOutputConnectionException e) {
                Log.d(TAG, "DownloadDataTask IOException: " + e.toString() + "");
                refreshfailed = true;
            }
            return null;
        }

        // Check code for when no pvoutput that exists on the date
        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "DownloadDataTask - onPostExecute");
            if (refreshfailed) {
                onRefreshFailed();
            } else {
                onRefreshComplete();
            }
        }
    }

}
