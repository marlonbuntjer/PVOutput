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
    private static String liveData, todayData, dailyData, monthlyData, yearlyData, lifetimeData,
            systemData, systemName;
    private ProgressDialog mProgress;
    private AlertDialog alert;
    private SharedPreferences defSharedPreferences;
    private String pvoutput_apikey;
    private String pvoutput_sid;
    private SharedPreferences mSharedPreferences;
    private PVOutputApiUrls urls;
    private List<String> urlList;

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

                urls = new PVOutputApiUrls(this);
                urlList = urls.getData();

                Log.d(TAG, "urlList_0 = " + urlList.get(0));
                Log.d(TAG, "urlList_1 = " + urlList.get(1));
                Log.d(TAG, "urlList_2 = " + urlList.get(2));
                Log.d(TAG, "urlList_3 = " + urlList.get(3));
                Log.d(TAG, "urlList_4 = " + urlList.get(4));
                Log.d(TAG, "urlList_5 = " + urlList.get(5));
                Log.d(TAG, "urlList_6 = " + urlList.get(6));

                // start the first Api call to check the API key and to find the System name
                // for which the data is retrieved
                Log.d(TAG, "Fetching System Name now");
                new DownloadDataTask().execute(urlList.get(6));

            } else {
                // not connected to the network
                Toast toast = Toast.makeText(this, R.string.not_connected, Toast.LENGTH_LONG);
                toast.show();
                // // TODO: 23-3-2016 Add retry option or close the app after X seconds
            }
        }
    }

    private void startMainDownload() {
        String[] splitSystemData = systemData.split(",");

        if (splitSystemData.length > 0) {
            systemName = splitSystemData[0];
            if (systemName.length() > 30) {
                systemName = systemName.substring(0, 30);
            }
        }

        new DownloadDataTask().execute(urlList.get(0), urlList.get(1), urlList.get(2), urlList.get(3), urlList.get(4), urlList.get(5));

        showProgressDialog(systemName);
    }


    private boolean checkNetworkConnection() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void showProgressDialog(String systemName) {
        mProgress = new ProgressDialog(this,
                R.style.AppTheme_Light_Dialog);
        mProgress.setIndeterminate(true);
        mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        String line1 = getString(R.string.loading_message);
        String message;
        if (!systemName.equals("")) {
            message = getString(R.string.loading_message) + "\n" + "for " + systemName;
        } else {
            message = getString(R.string.loading_message);
        }
        mProgress.setMessage(message);
        mProgress.show();
    }


    private void downloadFailedDialog(String errorStreamMessage) {
        if (mProgress != null) {
            mProgress.dismiss();
        }

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
        editor.putString("YEARLYDATA", yearlyData);
        editor.putString("LIFETIMEDATA", lifetimeData);
        editor.putString("SYSTEMNAME", systemName);

        // Store the first loadtime to prevent refreshing too often
        editor.putLong("REFRESHTIME", System.currentTimeMillis());

        // Commit the edits!
        editor.commit();

        Log.d(TAG, "Starting Main Activity now");

        if (mProgress != null) {
            mProgress.dismiss();
        }
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
        private boolean apiKeyOk = false;
        private boolean dataDownloadOk = false;
        private boolean pvOutputConnectionException = false;
        private String downloadFunction = "";
        private String downloadFunctionCompleted = "";

        @Override
        protected String doInBackground(String... urls) {
            Log.d(TAG, "DownloadDataTask - doInBackground");
            downloader = new Downloader();

            // Because pvoutput limits 60 API calls per hour, it is useful to enable the
            // app_testmode. No API calls will be made and only static testdata is used.
            if (app_testmode) {
                downloadFunction = "APP_TESTMODE";
                liveData = testData.liveData;
                todayData = testData.todayData;
                dailyData = testData.dailyData;
                monthlyData = testData.monthlyData;
                yearlyData = testData.yearlyData;
                lifetimeData = testData.lifetimeData;
                downloadFunctionCompleted = "APP_TESTMODE";
            } else {
                // When only one url is passed as parameter, only retrieve the systemdata
                // to make sure the api key is (still) correct
                // When 5 urls are passed the Api key is correct so proceed getting the other data
                if (urls.length == 1) {
                    downloadFunction = "CHECK_API_KEY";
                    try {
                        systemData = downloader.loadFromNetwork(urls[0]);

                        apiKeyOk = true;
                        downloadFunctionCompleted = "CHECK_API_KEY";
                        // catch exceptions -> set booleans so in onPostExecute the proper
                        // error handling can be done.
                    } catch (IOException e) {
                        Log.e(TAG, "DownloadDataTask IOException: " + e.toString() + "");
                    } catch (PVOutputConnectionException pce) {
                        pvOutputConnectionException = true;
                        Log.e(TAG, "DownloadDataTask PVOutputConnectionException: "
                                + pce.toString() + "");
                    }
                } else if (urls.length == 6) {
                    downloadFunction = "DOWNLOAD_DATA";
                    try {
                        liveData = downloader.loadFromNetwork(urls[0]);
                        todayData = downloader.loadFromNetwork(urls[1]);
                        dailyData = downloader.loadFromNetwork(urls[2]);
                        monthlyData = downloader.loadFromNetwork(urls[3]);
                        yearlyData = downloader.loadFromNetwork(urls[4]);
                        lifetimeData = downloader.loadFromNetwork(urls[5]);

                        dataDownloadOk = true;
                        downloadFunctionCompleted = "DOWNLOAD_DATA";

                        // catch exceptions -> set booleans so in onPostExecute the proper
                        // error handling can be done.
                    } catch (IOException e) {
                        Log.e(TAG, "DownloadDataTask IOException: " + e.toString() + "");
                    } catch (PVOutputConnectionException pce) {
                        pvOutputConnectionException = true;
                        Log.e(TAG, "DownloadDataTask PVOutputConnectionException: "
                                + pce.toString() + "");
                    }
                }
            }
            return downloadFunctionCompleted;
        }


        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "DownloadDataTask - onPostExecute");
            String errorMessage = "Could not load PV Output data";

            if (result == "") {
                // Download wasn't complete
                if (!apiKeyOk) {
                    downloadFailedDialog("Invalid API Key");
                } else if (!dataDownloadOk) {
                    downloadFailedDialog(errorMessage);
                }
            } else {
                Log.d(TAG, "Downloadfunction is: " + downloadFunction);
                if (downloadFunction.equals("CHECK_API_KEY")) {
                    Log.d(TAG, "apiKeyOk = " + apiKeyOk);
                    if (!apiKeyOk) {
                        showPrefsFragment();
                    } else {
                        startMainDownload();
                    }
                }

                if (downloadFunction.equals("DOWNLOAD_DATA")) {
                    Log.d(TAG, "dataDownloadOk = " + dataDownloadOk);
                    if (dataDownloadOk) {
                        // continue with main activity
                        startMainActivity();
                    } else {
                        // Could not load pvoutput data

                        if (pvOutputConnectionException) {
                            // format the errormessage (eg. Forbidden 403: Exceeded 60 requests per hour
                            // or Unauthorized 401: Invalid API Key
                            String[] em = downloader.getErrorStreamMessage().split(": ");
                            try {
                                errorMessage = em[1];
                            } catch (NumberFormatException e) {
                                Log.d(TAG, "createDailyDataArray - Exception: " + e.getMessage());
                            } catch (NullPointerException e) {
                                Log.d(TAG, "createDailyDataArray - Exception: " + e.getMessage());
                            } catch (ArrayIndexOutOfBoundsException e) {
                                Log.d(TAG, "createDailyDataArray - Exception: " + e.getMessage());
                            }
                        }

                        downloadFailedDialog(errorMessage);
                    }
                }

                if (downloadFunction.equals("APP_TESTMODE")) {
                    // continue with main activity
                    startMainActivity();
                }
            }
        }
    }
}
