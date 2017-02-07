package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.FetchTask;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);


        String symbol = getIntent().getStringExtra("symbol");
        FetchTask ft = new FetchTask(this, symbol);
        ft.execute();

        LineChart chart = (LineChart)findViewById(R.id.graph);
        chart.getLegend().setEnabled(false);

        chart.getXAxis().setTextSize(12);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        chart.getAxisLeft().setTextSize(12);
        chart.getAxisRight().setTextSize(12);

        chart.setNoDataText("");

        Description description = new Description();
        description.setText("");
        chart.setDescription(description);
    }

    public void setStockValues(List<Double> values)
    {
        String symbol = getIntent().getStringExtra("symbol");

        LineData data = buildLineData(values, symbol);
        data.setDrawValues(false);

        LineChart graph = (LineChart) findViewById(R.id.graph);

        graph.setData(data);
        graph.invalidate();

        TextView chartSymbol = (TextView) findViewById(R.id.chart_symbol);
        chartSymbol.setText(getString(R.string.symbol) + ": " + symbol);

        String bidValue = getIntent().getStringExtra("price");
        TextView bidPrice = (TextView) findViewById(R.id.bid_price);
        bidPrice.setText(getString(R.string.current_price) + ": " + bidValue);
    }

    private LineData buildLineData(List<Double> values, String symbol)
    {
        List<Entry> dataValues = new ArrayList<>();

        for (int i = 0; i < values.size(); i++)
            dataValues.add(new Entry(i, (float)(values.get(i).doubleValue())));

        LineDataSet set = new LineDataSet(dataValues, symbol);

        set.setDrawCircles(false);
        set.setLineWidth(2);

        LineData data = new LineData(set);

        return data;
    }


}
