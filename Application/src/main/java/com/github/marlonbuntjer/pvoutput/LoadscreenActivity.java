package com.github.marlonbuntjer.pvoutput;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;


public class LoadscreenActivity extends Activity {

    private static final String TAG = LoadscreenActivity.class.getSimpleName();
    private static final String PREFS_NAME = "SHAREDPREFS";
    private static String liveData, todayData, dailyData, monthlyData, lifetimeData;
    private ProgressDialog mProgress;
    private AlertDialog alert;
    private SharedPreferences defSharedPreferences;
    private String pvoutput_apikey;
    private String pvoutput_sid;
    private SharedPreferences mSharedPreferences;

    // this boolean is used for testing without actual api calls
    private boolean app_testmode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_loadscreen);

        // check if the preferences are filled with an api key and sid
        // if not, show the preference fragment
        defSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        pvoutput_apikey = defSharedPreferences.getString("pvoutput_apikey", "");
        pvoutput_sid = defSharedPreferences.getString("pvoutput_sid", "");

        if (pvoutput_apikey.equals("") || pvoutput_sid.equals("")) {
            showPrefsFragment();
        } else {
            // only attempt to download the data when a network connection is present
            if (checkNetworkConnection()) {

                PVOutputApiUrls urls = new PVOutputApiUrls(this);
                List<String> urlList = urls.getData();

                Log.d(TAG, "urlList_0 = " + urlList.get(0));
                Log.d(TAG, "urlList_1 = " + urlList.get(1));
                Log.d(TAG, "urlList_2 = " + urlList.get(2));
                Log.d(TAG, "urlList_3 = " + urlList.get(3));
                Log.d(TAG, "urlList_4 = " + urlList.get(4));

                new DownloadDataTask().execute(urlList.get(0), urlList.get(1), urlList.get(2), urlList.get(3), urlList.get(4));

                showProgressDialog();
            } else {
                // not connected to the network
                Toast toast = Toast.makeText(this, R.string.not_connected, Toast.LENGTH_LONG);
                toast.show();
                // // TODO: 23-3-2016 Add retry option or close the app after X seconds
            }
        }
    }

    private boolean checkNetworkConnection() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void showProgressDialog() {
        mProgress = new ProgressDialog(this,
                R.style.AppTheme_Light_Dialog);
        mProgress.setIndeterminate(true);
        mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgress.setMessage(getString(R.string.loading_message));
        mProgress.show();
    }


    private void downloadFailedDialog(String errorStreamMessage) {
        mProgress.dismiss();

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.alertDialogIcon, typedValue, true);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.loading_failed_message));
        builder.setIcon(typedValue.resourceId);

        if (errorStreamMessage.equalsIgnoreCase("Invalid API Key")) {
            Log.d(TAG, "downloadFailedDialog - Failed to download pvoutput data. " +
                    " Reason: invalid api key");
            builder.setMessage(getString(R.string.invalid_api_key_message));
            // define the 'Settings' button
            builder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    showPrefsFragment();
                }
            });
        } else {
            Log.d(TAG, "downloadFailedDialog - Failed to download pvoutput data. Reason: other");
            builder.setMessage(getString(R.string.loading_failed_try_again)
                    + " \n\n\n" + errorStreamMessage);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { // define the 'OK' button
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
        }

        //creating an alert dialog from our builder.
        alert = builder.create();
        alert.show();

        Button positive_button = alert.getButton(DialogInterface.BUTTON_POSITIVE);

        if (positive_button != null) {
            positive_button.setTextColor(ContextCompat.getColor(this, R.color.accent));
        }

    }

    private void showPrefsFragment() {
        Intent intent = new Intent();
        intent.setClass(LoadscreenActivity.this, SetPreferenceActivity.class);

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onRestart() {
        Log.v(TAG, "onRestart");

        // When the back button is pressed in the preference fragment, this activity should
        // be restarted to try again with the latest preference values
        restartActivity();

        super.onRestart();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();

        if (mProgress != null)
            mProgress.dismiss();

        if (alert != null)
            alert.dismiss();

    }

    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);

    }

    private void startMainActivity() {

        Intent i = new Intent(LoadscreenActivity.this, MainActivity.class);

        // store all the retrieved pvoutput data in the sharedpreferences
        // the fragments will pick it up from there and format it for presentation.
        mSharedPreferences = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = mSharedPreferences.edit();

        editor.putString("LIVEDATA", liveData);
        editor.putString("TODAYDATA", todayData);
        editor.putString("DAILYDATA", dailyData);
        editor.putString("MONTHLYDATA", monthlyData);
        editor.putString("LIFETIMEDATA", lifetimeData);

        // Store the first loadtime to prevent refreshing too often
        editor.putLong("REFRESHTIME", System.currentTimeMillis());

        // Commit the edits!
        editor.commit();

        Log.d(TAG, "Starting Main Activity now");

        mProgress.dismiss();
        startActivity(i);

        // Remove this loading activity
        finish();
    }

    /**
     * Implementation of AsyncTask, to fetch the data in the background away from
     * the UI thread.
     */
    private class DownloadDataTask extends AsyncTask<String, Void, String> {

        Downloader downloader;
        private boolean connOK = true;
        private boolean pvOutputConnectionException = false;

        @Override
        protected String doInBackground(String... urls) {
            Log.d(TAG, "DownloadDataTask - doInBackground");
            downloader = new Downloader();

            // Because pvoutput limits 60 API calls per hour, it is useful to enable the
            // app_testmode. No API calls will be made and only static testdata is used.
            if (app_testmode) {
                liveData = testData.liveData;
                todayData = testData.todayData;
                dailyData = testData.dailyData;
                monthlyData = testData.monthlyData;
                lifetimeData = testData.lifetimeData;
            } else {
                try {
                    liveData = downloader.loadFromNetwork(urls[0]);
                    todayData = downloader.loadFromNetwork(urls[1]);
                    dailyData = downloader.loadFromNetwork(urls[2]);
                    monthlyData = downloader.loadFromNetwork(urls[3]);
                    lifetimeData = downloader.loadFromNetwork(urls[4]);

                    // catch exceptions -> set booleans so in onPostExecute the proper
                    // error handling can be done.
                } catch (IOException e) {
                    connOK = false;
                    Log.e(TAG, "DownloadDataTask IOException: " + e.toString() + "");
                } catch (PVOutputConnectionException pce) {
                    connOK = false;
                    pvOutputConnectionException = true;
                    Log.e(TAG, "DownloadDataTask PVOutputConnectionException: "
                            + pce.toString() + "");
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "DownloadDataTask - onPostExecute");

            if (connOK) {
                // continue with main activity
                startMainActivity();
            } else {
                // Could not load pvoutput data
                String errorMessage = "Could not load PV Output data";

                if (pvOutputConnectionException) {
                    // format the errormessage (eg. Forbidden 403: Exceeded 60 requests per hour
                    // or Unauthorized 401: Invalid API Key
                    String[] em = downloader.getErrorStreamMessage().split(": ");
                    try {
                        errorMessage = em[1];
                    } catch (NumberFormatException | NullPointerException
                            | ArrayIndexOutOfBoundsException e) {
                        Log.d(TAG, "createDailyDataArray - Exception: " + e.getMessage());
                    }
                }

                downloadFailedDialog(errorMessage);
            }
        }
    }
}
