package com.oneui.stocks

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

class StockSymbolTinyWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            WidgetUpdateHelper.updateSymbolTinyWidget(context, appWidgetManager, appWidgetId)
        }
        val pendingResult = goAsync()
        WidgetUpdateHelper.refreshFromYahooAsync(context, pendingResult)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        WidgetUpdateHelper.updateSymbolTinyWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetUpdateHelper.ACTION_REFRESH_WIDGET) {
            val pendingResult = goAsync()
            WidgetUpdateHelper.refreshFromYahooAsync(context, pendingResult)
        }
    }
}
