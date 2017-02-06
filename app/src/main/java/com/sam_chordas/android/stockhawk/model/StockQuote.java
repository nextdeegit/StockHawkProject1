package com.sam_chordas.android.stockhawk.model;

import com.google.gson.annotations.SerializedName;
/**
 * Created by ESIDEM jnr on 11/11/2016.
 */

public class StockQuote {

    @SerializedName("Change")
    private String mChange;

    @SerializedName("symbol")
    private String mSymbol;

    @SerializedName("Name")
    private String mName;

    @SerializedName("Bid")
    private String mBid;

    @SerializedName("ChangeinPercent")
    private String mChangeInPercent;

    public String getChange() {
        return mChange;
    }

    public String getBid() {
        return mBid;
    }

    public String getSymbol() {
        return mSymbol;
    }

    public String getChangeInPercent() {
        return mChangeInPercent;
    }

    public String getName() {
        return mName;
    }
}
