package com.oneui.stocks

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.PowerManager
import android.widget.RemoteViews
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object WidgetUpdateHelper {
    const val ACTION_REFRESH_WIDGET = "com.oneui.stocks.ACTION_REFRESH_WIDGET"
    private val executor = Executors.newSingleThreadExecutor()
    private val isSyncInFlight = AtomicBoolean(false)

    private val colorPositive = Color.parseColor("#FF30D158")
    private val colorNegative = Color.parseColor("#FFFF453A")

    fun renderAllWidgetsFromCache(context: Context) {
        AutoSyncScheduler.ensureScheduled(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // 1. Symbol with Graph (2x1 Lock Screen Widget)
        val graphSmallIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, StockGraphSmallWidgetProvider::class.java)
        )
        for (id in graphSmallIds) {
            updateGraphSmallWidget(context, appWidgetManager, id)
        }

        // 2. Symbol (1x1 Tiny Lock Screen Widget)
        val symbolTinyIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, StockSymbolTinyWidgetProvider::class.java)
        )
        for (id in symbolTinyIds) {
            updateSymbolTinyWidget(context, appWidgetManager, id)
        }

        // 3. Symbol (2x1 Small Lock Screen Widget)
        val symbolSmallIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, StockSymbolSmallWidgetProvider::class.java)
        )
        for (id in symbolSmallIds) {
            updateSymbolSmallWidget(context, appWidgetManager, id)
        }

        // 4. Watchlist (2x1 Small Lock Screen Widget)
        val watchlistSmallIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, StockWatchlistSmallWidgetProvider::class.java)
        )
        for (id in watchlistSmallIds) {
            updateWatchlistSmallWidget(context, appWidgetManager, id)
        }

        // 5. Home Screen 2x1 Pill Widget (Shows Featured Lock Screen Symbol)
        val home2x1Ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, StockHome2x1WidgetProvider::class.java)
        )
        for (id in home2x1Ids) {
            updateHome2x1Widget(context, appWidgetManager, id)
        }

        // 6. Home Screen 2x2 Card Widget (Shows Featured Lock Screen Symbol)
        val home2x2Ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, StockHome2x2WidgetProvider::class.java)
        )
        for (id in home2x2Ids) {
            updateHome2x2Widget(context, appWidgetManager, id)
        }

        // 7. Home Screen 4x1 Wide Pill Widget (Shows Featured Lock Screen Symbol)
        val home4x1Ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, StockHome4x1WidgetProvider::class.java)
        )
        for (id in home4x1Ids) {
            updateHome4x1Widget(context, appWidgetManager, id)
        }

        // 8. Home Screen 4x2 Large Card Widget (Shows Featured Lock Screen Symbol)
        val home4x2Ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, StockHome4x2WidgetProvider::class.java)
        )
        for (id in home4x2Ids) {
            updateHome4x2Widget(context, appWidgetManager, id)
        }
    }

    fun refreshFromYahooAsync(
        context: Context,
        pendingResult: BroadcastReceiver.PendingResult? = null,
        onComplete: ((Result<List<StockQuote>>) -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        renderAllWidgetsFromCache(appContext)

        if (!isSyncInFlight.compareAndSet(false, true) && onComplete == null) {
            pendingResult?.finish()
            return
        }

        executor.execute {
            val powerManager = appContext.getSystemService(PowerManager::class.java)
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "OneUIStocks:YahooSyncWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(12_000L)
            }
            try {
                val result = YahooFinanceClient.syncAll(appContext)
                result.onSuccess {
                    renderAllWidgetsFromCache(appContext)
                }
                onComplete?.invoke(result)
            } finally {
                isSyncInFlight.set(false)
                AutoSyncScheduler.ensureScheduled(appContext)
                runCatching {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                }
                pendingResult?.finish()
            }
        }
    }

    // 1. Symbol with Graph (2x1 Small Lock Screen Widget)
    fun updateGraphSmallWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val featuredSym = StockPreferences.getFeaturedSymbol(context)
        val quote = StockPreferences.getQuoteForSymbol(context, featuredSym)
        val clickIntent = createOpenAppPendingIntent(context, appWidgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_graph_small)
        views.setTextViewText(R.id.widget_symbol_title, "${quote.arrowSymbol} ${quote.symbol}")
        views.setTextViewText(R.id.widget_symbol_change, quote.lockScreenChangeLabel())

        val bitmap = SparklineRenderer.createLockWidgetGraphBitmap(
            points = quote.sparkline,
            previousClose = quote.previousClose.toFloat()
        )
        views.setImageViewBitmap(R.id.widget_graph_image, bitmap)

        views.setOnClickPendingIntent(android.R.id.background, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_symbol_title, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_symbol_change, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_graph_image, clickIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // 2. Symbol (1x1 Tiny Lock Screen Widget)
    fun updateSymbolTinyWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val featuredSym = StockPreferences.getFeaturedSymbol(context)
        val quote = StockPreferences.getQuoteForSymbol(context, featuredSym)
        val clickIntent = createOpenAppPendingIntent(context, appWidgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_symbol_tiny)
        views.setTextViewText(R.id.widget_tiny_arrow, quote.arrowSymbol)
        views.setTextViewText(R.id.widget_tiny_symbol, quote.symbol)
        views.setTextViewText(R.id.widget_tiny_sub, quote.lockScreenChangeLabel())

        views.setOnClickPendingIntent(android.R.id.background, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_tiny_arrow, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_tiny_symbol, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_tiny_sub, clickIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // 3. Symbol (2x1 Small Lock Screen Widget)
    fun updateSymbolSmallWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val featuredSym = StockPreferences.getFeaturedSymbol(context)
        val quote = StockPreferences.getQuoteForSymbol(context, featuredSym)
        val clickIntent = createOpenAppPendingIntent(context, appWidgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_symbol_small)
        views.setTextViewText(R.id.widget_symbol_header, "${quote.arrowSymbol} ${quote.symbol}")
        views.setTextViewText(R.id.widget_symbol_change, quote.lockScreenChangeLabel())
        views.setTextViewText(R.id.widget_symbol_price, quote.formattedPrice())

        views.setOnClickPendingIntent(android.R.id.background, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_symbol_header, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_symbol_change, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_symbol_price, clickIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // 4. Watchlist (2x1 Small Lock Screen Widget)
    fun updateWatchlistSmallWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val quotes = StockPreferences.getWatchlistQuotes(context)
        val q1 = quotes.getOrNull(0) ?: StockPreferences.getQuoteForSymbol(context, "AUDUSD=X")
        val q2 = quotes.getOrNull(1) ?: StockPreferences.getQuoteForSymbol(context, "AUDGBP=X")
        val q3 = quotes.getOrNull(2) ?: StockPreferences.getQuoteForSymbol(context, "AUDJPY=X")
        val clickIntent = createOpenAppPendingIntent(context, appWidgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_watchlist_small)
        views.setTextViewText(R.id.widget_wl_sym_1, "${q1.arrowSymbol} ${q1.symbol}")
        views.setTextViewText(R.id.widget_wl_price_1, q1.formattedPrice())

        views.setTextViewText(R.id.widget_wl_sym_2, "${q2.arrowSymbol} ${q2.symbol}")
        views.setTextViewText(R.id.widget_wl_price_2, q2.formattedPrice())

        views.setTextViewText(R.id.widget_wl_sym_3, "${q3.arrowSymbol} ${q3.symbol}")
        views.setTextViewText(R.id.widget_wl_price_3, q3.formattedPrice())

        views.setOnClickPendingIntent(android.R.id.background, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_wl_sym_1, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_wl_price_1, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_wl_sym_2, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_wl_price_2, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_wl_sym_3, clickIntent)
        views.setOnClickPendingIntent(R.id.widget_wl_price_3, clickIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // 5. Home Screen 2x1 Pill Widget (Shows Featured Lock Screen Symbol)
    fun updateHome2x1Widget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val featuredSym = StockPreferences.getFeaturedSymbol(context)
        val quote = StockPreferences.getQuoteForSymbol(context, featuredSym)
        val clickIntent = createOpenAppPendingIntent(context, appWidgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_home_2x1)
        views.setTextViewText(R.id.home_2x1_price, quote.formattedPrice())
        views.setTextViewText(
            R.id.home_2x1_sub,
            "${quote.arrowSymbol} ${quote.symbol}  ${quote.formattedPercentChange()}"
        )
        views.setTextColor(
            R.id.home_2x1_sub,
            if (quote.isPositive) colorPositive else colorNegative
        )

        views.setOnClickPendingIntent(android.R.id.background, clickIntent)
        views.setOnClickPendingIntent(R.id.home_2x1_price, clickIntent)
        views.setOnClickPendingIntent(R.id.home_2x1_sub, clickIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // 6. Home Screen 2x2 Squircle Card Widget (Shows Featured Lock Screen Symbol)
    fun updateHome2x2Widget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val featuredSym = StockPreferences.getFeaturedSymbol(context)
        val quote = StockPreferences.getQuoteForSymbol(context, featuredSym)
        val clickIntent = createOpenAppPendingIntent(context, appWidgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_home_2x2)
        views.setTextViewText(R.id.home_2x2_symbol, "${quote.arrowSymbol} ${quote.symbol}")
        views.setTextViewText(R.id.home_2x2_name, quote.name)
        views.setTextViewText(R.id.home_2x2_price, quote.formattedPrice())
        views.setTextViewText(R.id.home_2x2_change, quote.formattedPercentChange())
        views.setTextColor(
            R.id.home_2x2_change,
            if (quote.isPositive) colorPositive else colorNegative
        )

        val graphBmp = SparklineRenderer.createColorWidgetGraphBitmap(
            points = quote.sparkline,
            previousClose = quote.previousClose.toFloat(),
            isPositive = quote.isPositive,
            widthPx = 300,
            heightPx = 100
        )
        views.setImageViewBitmap(R.id.home_2x2_graph, graphBmp)

        views.setOnClickPendingIntent(android.R.id.background, clickIntent)
        views.setOnClickPendingIntent(R.id.home_2x2_symbol, clickIntent)
        views.setOnClickPendingIntent(R.id.home_2x2_name, clickIntent)
        views.setOnClickPendingIntent(R.id.home_2x2_graph, clickIntent)
        views.setOnClickPendingIntent(R.id.home_2x2_price, clickIntent)
        views.setOnClickPendingIntent(R.id.home_2x2_change, clickIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // 7. Home Screen 4x1 Wide Pill Widget (Shows Featured Lock Screen Symbol)
    fun updateHome4x1Widget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val featuredSym = StockPreferences.getFeaturedSymbol(context)
        val quote = StockPreferences.getQuoteForSymbol(context, featuredSym)
        val clickIntent = createOpenAppPendingIntent(context, appWidgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_home_4x1)
        views.setTextViewText(R.id.home_4x1_symbol, "${quote.arrowSymbol} ${quote.symbol}")
        views.setTextViewText(R.id.home_4x1_name, quote.name)
        views.setTextViewText(R.id.home_4x1_price, quote.formattedPrice())
        views.setTextViewText(R.id.home_4x1_change, quote.formattedPercentChange())
        views.setTextColor(
            R.id.home_4x1_change,
            if (quote.isPositive) colorPositive else colorNegative
        )

        val graphBmp = SparklineRenderer.createColorWidgetGraphBitmap(
            points = quote.sparkline,
            previousClose = quote.previousClose.toFloat(),
            isPositive = quote.isPositive,
            widthPx = 240,
            heightPx = 84
        )
        views.setImageViewBitmap(R.id.home_4x1_graph, graphBmp)

        views.setOnClickPendingIntent(android.R.id.background, clickIntent)
        views.setOnClickPendingIntent(R.id.home_4x1_symbol, clickIntent)
        views.setOnClickPendingIntent(R.id.home_4x1_name, clickIntent)
        views.setOnClickPendingIntent(R.id.home_4x1_graph, clickIntent)
        views.setOnClickPendingIntent(R.id.home_4x1_price, clickIntent)
        views.setOnClickPendingIntent(R.id.home_4x1_change, clickIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // 8. Home Screen 4x2 Large Card Widget (Shows Featured Lock Screen Symbol)
    fun updateHome4x2Widget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val featuredSym = StockPreferences.getFeaturedSymbol(context)
        val quote = StockPreferences.getQuoteForSymbol(context, featuredSym)
        val clickIntent = createOpenAppPendingIntent(context, appWidgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_home_4x2)
        views.setTextViewText(
            R.id.home_4x2_header,
            "${quote.arrowSymbol} ${quote.symbol} · ${quote.name}"
        )
        views.setTextViewText(R.id.home_4x2_price, quote.formattedPrice())
        views.setTextViewText(
            R.id.home_4x2_change,
            "${quote.formattedChange()} (${quote.formattedPercentChange()})"
        )
        views.setTextColor(
            R.id.home_4x2_change,
            if (quote.isPositive) colorPositive else colorNegative
        )

        val graphBmp = SparklineRenderer.createColorWidgetGraphBitmap(
            points = quote.sparkline,
            previousClose = quote.previousClose.toFloat(),
            isPositive = quote.isPositive,
            widthPx = 520,
            heightPx = 110
        )
        views.setImageViewBitmap(R.id.home_4x2_graph, graphBmp)

        views.setOnClickPendingIntent(android.R.id.background, clickIntent)
        views.setOnClickPendingIntent(R.id.home_4x2_header, clickIntent)
        views.setOnClickPendingIntent(R.id.home_4x2_price, clickIntent)
        views.setOnClickPendingIntent(R.id.home_4x2_change, clickIntent)
        views.setOnClickPendingIntent(R.id.home_4x2_graph, clickIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createOpenAppPendingIntent(
        context: Context,
        appWidgetId: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
