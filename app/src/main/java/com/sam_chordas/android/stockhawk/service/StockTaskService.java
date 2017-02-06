package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteDetailColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.model.ResponseGetStock;
import com.sam_chordas.android.stockhawk.model.ResponseGetStocks;
import com.sam_chordas.android.stockhawk.model.StockDetail;
import com.sam_chordas.android.stockhawk.model.StockQuote;
import com.sam_chordas.android.stockhawk.model.StockService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private static String LOG_TAG = StockTaskService.class.getSimpleName();
  private final static String INIT_QUOTES = "\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\"";
  public final static String TAG_PERIODIC = "periodic";

  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean mIsUpdate;

  public StockTaskService(Context context) {
    mContext = context;
  }

  @SuppressWarnings("unused")
  public StockTaskService() {
  }

  @Override
  public int onRunTask(TaskParams params) {

    if (mContext == null) {
      return GcmNetworkManager.RESULT_FAILURE;
    }
    try {

      // Load relevant data about stocks
      Retrofit retrofit = new Retrofit.Builder()
              .baseUrl(StockService.BASE_URL)
              .addConverterFactory(GsonConverterFactory.create())
              .build();
      StockService service = retrofit.create(StockService.class);
      String query = "select * from yahoo.finance.quotes where symbol in ("
              + buildUrl(params)
              + ")";

      // UGLY : JSON is different, if we request data for multiple stocks and single stock.
      if (params.getTag().equals(StockIntentService.ACTION_INIT)) {
        Call<ResponseGetStocks> call = service.getStocks(query);
        retrofit2.Response<ResponseGetStocks> response = call.execute();
        ResponseGetStocks responseGetStocks = response.body();
        saveQuotes2Database(responseGetStocks.getStockQuotes());
      } else {
        Call<ResponseGetStock> call = service.getStock(query);
        retrofit2.Response<ResponseGetStock> response = call.execute();
        ResponseGetStock responseGetStock = response.body();
        saveQuotes2Database(responseGetStock.getStockQuotes());
      }

      return GcmNetworkManager.RESULT_SUCCESS;

    } catch (IOException | RemoteException | OperationApplicationException e) {
      Log.e(LOG_TAG, e.getMessage(), e);
      return GcmNetworkManager.RESULT_FAILURE;
    }
  }

  private String buildUrl(TaskParams params) throws UnsupportedEncodingException {
    ContentResolver resolver = mContext.getContentResolver();
    if (params.getTag().equals(StockIntentService.ACTION_INIT)
            || params.getTag().equals(TAG_PERIODIC)) {
      mIsUpdate = true;
      Cursor cursor = resolver.query(QuoteProvider.Quotes.CONTENT_URI,
              new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
              null, null);

      if (cursor != null && cursor.getCount() == 0 || cursor == null) {
        // Init task. Populates DB with quotes for the symbols seen below
        return INIT_QUOTES;
      } else {
        DatabaseUtils.dumpCursor(cursor);
        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++) {
          mStoredSymbols.append("\"");
          mStoredSymbols.append(cursor.getString(
                  cursor.getColumnIndex(QuoteColumns.SYMBOL)));
          mStoredSymbols.append("\",");
          cursor.moveToNext();
        }
        mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), "");
        return mStoredSymbols.toString();
      }
    } else if (params.getTag().equals(StockIntentService.ACTION_ADD)) {
      mIsUpdate = false;
      // Get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString(StockIntentService.EXTRA_SYMBOL);
      return "\"" + stockInput + "\"";
    } else {
      throw new IllegalStateException("Action not specified in TaskParams.");
    }
  }

  private void saveQuotes2Database(List<StockQuote> quotes) throws RemoteException, OperationApplicationException {
    ContentResolver resolver = mContext.getContentResolver();

    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    for (StockQuote quote : quotes) {

      batchOperations.add(QuoteProvider.buildBatchOperation(quote));
    }

    // Update is_current to 0 (false), so new data is current.
    if (mIsUpdate) {
      ContentValues contentValues = new ContentValues();
      contentValues.put(QuoteColumns.ISCURRENT, 0);
      resolver.update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
              null, null);
    }

    resolver.applyBatch(QuoteProvider.AUTHORITY, batchOperations);

    for (StockQuote quote : quotes) {
      // Load historical data for the quote
      try {
        loadHistoricalData(quote);
      } catch (IOException | RemoteException | OperationApplicationException e) {
        Log.e(LOG_TAG, e.getMessage(), e);
      }
    }
  }

  private void loadHistoricalData(StockQuote quote) throws IOException, RemoteException,
          OperationApplicationException {

    // Load historic stock data
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    Date currentDate = new Date();

    Calendar calEnd = Calendar.getInstance();
    calEnd.setTime(currentDate);
    calEnd.add(Calendar.DATE, 0);

    Calendar calStart = Calendar.getInstance();
    calStart.setTime(currentDate);
    calStart.add(Calendar.MONTH, -1);

    String startDate = dateFormat.format(calStart.getTime());
    String endDate = dateFormat.format(calEnd.getTime());

    String query = "select * from yahoo.finance.historicaldata where symbol=\"" +
            quote.getSymbol() +
            "\" and startDate=\"" + startDate + "\" and endDate=\"" + endDate + "\"";

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(StockService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    StockService service = retrofit.create(StockService.class);
    Call<StockDetail> call = service.getStockHistoricalData(query);
    retrofit2.Response<StockDetail> response;
    response = call.execute();
    StockDetail responseGetHistoricalData = response.body();
    if (responseGetHistoricalData != null) {
      saveQuoteHistoricalData2Database(responseGetHistoricalData.getDetailData());
    }
  }

  private void saveQuoteHistoricalData2Database(List<StockDetail.Quote> quotes)
          throws RemoteException, OperationApplicationException {
    ContentResolver resolver = mContext.getContentResolver();
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    for (StockDetail.Quote quote : quotes) {

      // First, we have to delete outdated date from DB.
      resolver.delete(QuoteProvider.QuotesDetailsData.CONTENT_URI,
              QuoteDetailColumns.SYMBOL + " = \"" + quote.getSymbol() + "\"", null);

      batchOperations.add(QuoteProvider.buildBatchOperation(quote));
    }

    resolver.applyBatch(QuoteProvider.AUTHORITY, batchOperations);
  }
}

