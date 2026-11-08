package com.oneui.stocks

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

abstract class BaseStockHomeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdateHelper.renderAllWidgetsFromCache(context)
        WidgetUpdateHelper.refreshFromYahooAsync(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetUpdateHelper.ACTION_REFRESH_WIDGET) {
            val pending = goAsync()
            WidgetUpdateHelper.refreshFromYahooAsync(context, pending)
        }
    }
}

class StockHome2x1WidgetProvider : BaseStockHomeWidgetProvider()
class StockHome2x2WidgetProvider : BaseStockHomeWidgetProvider()
class StockHome4x1WidgetProvider : BaseStockHomeWidgetProvider()
class StockHome4x2WidgetProvider : BaseStockHomeWidgetProvider()
