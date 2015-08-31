package com.github.marlonbuntjer.pvoutput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Marlon Buntjer on 25-6-2015.
 */
public class Downloader {

    private String errorStreamMessage;

    public Downloader() {
    }

    /**
     * Initiates the fetch operation.
     */
    public String loadFromNetwork(String urlString) throws IOException, PVOutputConnectionException {
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
    private InputStream downloadUrl(String urlString) throws IOException, PVOutputConnectionException {
        // BEGIN_INCLUDE(get_inputstream)
        InputStream inputStream;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Start the query
            conn.connect();
            inputStream = conn.getInputStream();
        } catch (IOException e) {
            if (conn != null) {
                this.errorStreamMessage = readIt(conn.getErrorStream());
            }
            throw new PVOutputConnectionException();
        }
        return inputStream;
        // END_INCLUDE(get_inputstream)
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
