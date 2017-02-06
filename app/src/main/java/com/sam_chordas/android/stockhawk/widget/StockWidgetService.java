package com.sam_chordas.android.stockhawk.widget;


import android.content.Intent;
import android.widget.RemoteViewsService;



/**
 * Created by ESIDEM jnr on 11/14/2016.
 */

public class StockWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetFactory(getApplicationContext(), intent);
    }}