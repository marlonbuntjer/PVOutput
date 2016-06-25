package com.github.marlonbuntjer.pvoutput;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Marlon Buntjer on 24-6-2015.
 */
class PVOutputApiUrls {

    private Context context;
    private Resources res;
    private SharedPreferences mySharedPreferences;
    private String pvoutput_apikey;
    private String pvoutput_sid;

    public PVOutputApiUrls(Context current) {
        this.context = current;
        res = context.getResources();

        mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(current);

        pvoutput_apikey = mySharedPreferences.getString("pvoutput_apikey", "");
        pvoutput_sid = mySharedPreferences.getString("pvoutput_sid", "");
    }

    private String getLiveDataUrl(String date) {
        return res.getString(R.string.pvoutput_api_url) + ""
                + res.getString(R.string.pvoutput_api_service_status) + "?"
                + "key=" + pvoutput_apikey + "&"
                + "sid=" + pvoutput_sid + "&d="
                + date;
    }

    private String getTodayDataUrl(String date) {
        return res.getString(R.string.pvoutput_api_url) + ""
                + res.getString(R.string.pvoutput_api_service_status) + "?"
                + "key=" + pvoutput_apikey + "&"
                + "sid=" + pvoutput_sid + "&d="
                + date + "&h=1&from=06:00&limit=216";
    }

    private String getDailyDataUrl(String date) {
        return res.getString(R.string.pvoutput_api_url) + ""
                + res.getString(R.string.pvoutput_api_service_output) + "?"
                + "key=" + pvoutput_apikey + "&"
                + "sid=" + pvoutput_sid + "&d="
                + date + "limit=30";
    }


    private String getMonthlyDataUrl(String date) {
        return res.getString(R.string.pvoutput_api_url) + ""
                + res.getString(R.string.pvoutput_api_service_output) + "?"
                + "key=" + pvoutput_apikey + "&"
                + "sid=" + pvoutput_sid + "&d="
                + date + "&a=m";
    }

    private String getYearlyDataUrl(String date) {
        return res.getString(R.string.pvoutput_api_url) + ""
                + res.getString(R.string.pvoutput_api_service_output) + "?"
                + "key=" + pvoutput_apikey + "&"
                + "sid=" + pvoutput_sid + "&d="
                + date + "&a=y";
    }

    private String getLifetimeDataUrl(String date) {
        return res.getString(R.string.pvoutput_api_url) + ""
                + res.getString(R.string.pvoutput_api_service_statistic) + "?"
                + "key=" + pvoutput_apikey + "&"
                + "sid=" + pvoutput_sid + "&d="
                + date;
    }

    private String getSystemDataUrl() {
        return res.getString(R.string.pvoutput_api_url) + ""
                + res.getString(R.string.pvoutput_api_service_system) + "?"
                + "key=" + pvoutput_apikey + "&"
                + "sid=" + pvoutput_sid;
    }

    public List<String> getData() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd", Locale.US);
        Date now = new Date();

        String strDate = sdfDate.format(now);

        /**
         * Before 06:00h, use the date of yesterday
         */
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date yesterday = cal.getTime();

        if (cal.get(Calendar.HOUR_OF_DAY) < 6) {
            strDate = sdfDate.format(yesterday);
        }


        String live = getLiveDataUrl(strDate);
        String today = getTodayDataUrl(strDate);
        String daily = getDailyDataUrl(strDate);
        String monthly = getMonthlyDataUrl(strDate);
        String yearly = getYearlyDataUrl(strDate);
        String lifetime = getLifetimeDataUrl(strDate);
        String system = getSystemDataUrl();

        List<String> urlList = new ArrayList<String>();
        urlList.add(live);
        urlList.add(today);
        urlList.add(daily);
        urlList.add(monthly);
        urlList.add(yearly);
        urlList.add(lifetime);
        urlList.add(system);

        return urlList;

    }

}
