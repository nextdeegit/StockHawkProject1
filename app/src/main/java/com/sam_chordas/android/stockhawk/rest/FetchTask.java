package com.sam_chordas.android.stockhawk.rest;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.sam_chordas.android.stockhawk.ui.DetailActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by hanson on 2/7/17.
 */

public class FetchTask extends AsyncTask<Void, Integer, String> {

    private DetailActivity activity;
    private String symbol;

    public FetchTask(DetailActivity activity, String symbol)
    {
        this.activity = activity;
        this.symbol = symbol;
    }

    @Override
    protected String doInBackground(Void... v)
    {
        Uri uri = buildURI();

        return retrieveJSON(uri.toString());
    }

    @Override
    protected void onPostExecute(String json)
    {
        List<Double> stockValues = parseQuotes(json);

        List<Double> reverse = new ArrayList<>();
        for (Double d : stockValues)
            reverse.add(0, d);

        this.activity.setStockValues(reverse);
    }

    @Override
    protected void onProgressUpdate(Integer... progress)
    {
    }

    private Uri buildURI()
    {
        final String SCHEME = "https";
        final String AUTHORITY = "query.yahooapis.com";
        final String PATH = "v1/public/yql";
        final String QUERY_PARAMETER = "q";
        final String FORMAT_PARAMETER = "format";
        final String FORMAT_VALUE = "json";
        final String ENVIRONMENT_PARAMETER = "env";
        final String ENVIRONMENT_VALUE = "store://datatables.org/alltableswithkeys";
        final int DURATION_IN_MONTHS = 3;

        Calendar calendar = Calendar.getInstance();

        int endMonth = calendar.get(Calendar.MONTH) +1;
        String endDate = calendar.get(Calendar.YEAR) + "-" +
                endMonth + "-" + calendar.get(Calendar.DATE);

        calendar.add(Calendar.MONTH, -DURATION_IN_MONTHS);

        int startMonth = calendar.get(Calendar.MONTH) +1;
        String startDate = calendar.get(Calendar.YEAR) + "-" +
                startMonth + "-" + calendar.get(Calendar.DATE);

        String query = "select * from yahoo.finance.historicaldata where symbol=\""
                + this.symbol + "\" and startDate = \"" + startDate + "\" " +
                "and endDate = \"" + endDate + "\"";

        Uri.Builder builder = new Uri.Builder()
                .scheme(SCHEME)
                .authority(AUTHORITY)
                .appendEncodedPath(PATH)
                .appendQueryParameter(QUERY_PARAMETER, query)
                .appendQueryParameter(FORMAT_PARAMETER, FORMAT_VALUE)
                .appendQueryParameter(ENVIRONMENT_PARAMETER, ENVIRONMENT_VALUE);

        return builder.build();
    }

    private String retrieveJSON(String uri)
    {
        final String REQUEST_METHOD = "GET";

        String result = "";
        InputStream stream = null;
        HttpURLConnection connection = null;

        try
        {
            URL url = new URL(uri);
            connection = (HttpURLConnection)url.openConnection();

            connection.setRequestMethod(REQUEST_METHOD);
            connection.connect();

            stream = connection.getInputStream();
            if (stream != null)
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line;
                while ((line = reader.readLine()) != null)
                    buffer.append(line + "\n");
                result = buffer.toString();
            }
        }
        catch(Exception exception)
        {
            if (exception instanceof UnknownHostException)
                publishProgress(0);
        }
        finally
        {
            try
            {
                if (stream != null)
                    stream.close();
                if (connection != null)
                    connection.disconnect();
            }
            catch(IOException exception){}
        }

        return result;
    }

    private List<Double> parseQuotes(String json)
    {
        final String KEY_QUERY = "query";
        final String KEY_RESULTS = "results";
        final String KEY_QUOTE = "quote";
        final String KEY_OPENING_VALUE = "Open";

        List<Double> values = new ArrayList<>();
        if (json.length() > 0)
        {
            try
            {
                JSONObject jsonObject = new JSONObject(json);
                JSONObject jsonQuery = jsonObject.getJSONObject(KEY_QUERY);
                JSONObject jsonResults = jsonQuery.getJSONObject(KEY_RESULTS);
                JSONArray jsonQuoteArray = jsonResults.getJSONArray(KEY_QUOTE);

                for (int i = 0; i < jsonQuoteArray.length(); i++)
                {
                    JSONObject jsonQuote = (JSONObject)jsonQuoteArray.getJSONObject(i);

                    double openingValue = jsonQuote.getDouble(KEY_OPENING_VALUE);
                    values.add(openingValue);
                }
            }
            catch (Exception e)
            {
                Log.v(getClass().getSimpleName(), e.toString());
            }
        }

        return values;
    }
}
