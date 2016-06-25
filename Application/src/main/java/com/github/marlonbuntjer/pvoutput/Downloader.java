package com.github.marlonbuntjer.pvoutput;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * Created by Marlon Buntjer on 25-6-2015.
 */
class Downloader {

    private static final String TAG = MonthlyFragment.class.getSimpleName();
    private String errorStreamMessage;

    public Downloader() {
    }

    /**
     * Initiates the fetch operation.
     */
    public String loadFromNetwork(String urlString)
            throws IOException, PVOutputConnectionException {
        InputStream stream = null;
        String str = "";

        try {
            stream = downloadUrl(urlString);
            str = readIt(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        return str;
    }

    /**
     * Given a string representation of a URL, sets up a connection and gets
     * an input stream.
     *
     * @param urlString A string representation of a URL.
     * @return An InputStream retrieved from a successful HttpURLConnection.
     * @throws java.io.IOException
     */
    private InputStream downloadUrl(String urlString)
            throws IOException, PVOutputConnectionException {

        InputStream inputStream;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Start the query
            conn.connect();
            inputStream = conn.getInputStream();
        } catch (SocketTimeoutException ste) {
            throw new PVOutputConnectionException();
        } catch (IOException e) {
            if (conn != null) {
                // Reading the response body cleans up the connection even if
                // you are not interested in the response content itself.
                this.errorStreamMessage = readIt(conn.getErrorStream());

                Log.d(TAG, "responsecode = " + conn.getResponseCode());
                Log.d(TAG, "errorStreamMessage = " + errorStreamMessage);

                // if no livedata and todaydata are uploaded to pvoutput.org the errormessage
                // "Bad request 400: No status found" will be returned
                // the errorstream is returned and will be handled by the caller
                // other url downloads should then continue
                if (errorStreamMessage.equals("Bad request 400: No status found")) {
                    // No Status data found.
                    inputStream = conn.getErrorStream();
                } else {
                    throw new PVOutputConnectionException();
                }
            } else {
                throw new PVOutputConnectionException();
            }
        }
        return inputStream;
    }


    public String getErrorStreamMessage() {
        return errorStreamMessage;
    }

    /**
     * Reads an InputStream and converts it to a String.
     *
     * @param stream InputStream containing HTML from targeted site.
     * @return String concatenated according to len parameter.
     * @throws java.io.IOException
     * @throws java.io.UnsupportedEncodingException
     */
    private String readIt(InputStream stream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
    }
}
