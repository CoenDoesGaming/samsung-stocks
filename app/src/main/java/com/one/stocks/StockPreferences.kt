package com.one.stocks

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object StockPreferences {
    private const val PREFS_NAME = "oneui_stocks_prefs"
    private const val KEY_SYMBOLS = "watchlist_symbols"
    private const val KEY_QUOTES_JSON = "cached_quotes_v2_json"
    private const val KEY_NEWS_JSON = "cached_news_json"
    private const val KEY_FEATURED_SYMBOL = "featured_lock_symbol"
    private const val KEY_BADGE_MODE = "badge_mode"
    private const val KEY_SORT_MODE = "sort_mode"
    private const val KEY_RECENT_SEARCHES = "recent_searches_json"
    private const val KEY_LAST_SYNC_MS = "last_sync_ms"
    private const val KEY_WELCOME_SEEN = "welcome_seen"

    val DEFAULT_SYMBOLS = listOf(
        "AUDUSD=X",
        "AUDGBP=X",
        "AUDJPY=X",
        "AUDFJD=X",
        "AUDNZD=X",
        "AAPL",
        "GOOG"
    )

    /** Biggest movers among the user's own watchlist, by absolute percentage change. */
    fun getBigMoversToday(context: Context): List<StockQuote> {
        return getWatchlistQuotes(context)
            .filter { !it.changePercent.isNaN() }
            .distinctBy { it.symbol.uppercase() }
            .sortedByDescending { kotlin.math.abs(it.changePercent) }
            .take(5)
    }

    fun getWatchlistSymbols(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SYMBOLS, null) ?: return DEFAULT_SYMBOLS
        val list = raw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        return if (list.isEmpty()) DEFAULT_SYMBOLS else list
    }

    fun setWatchlistSymbols(context: Context, symbols: List<String>) {
        val cleaned = symbols.map { it.trim().uppercase() }.filter { it.isNotEmpty() }.distinct()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SYMBOLS, cleaned.joinToString(","))
            .apply()
    }

    fun addSymbol(context: Context, symbol: String) {
        val current = getWatchlistSymbols(context).toMutableList()
        val upper = symbol.trim().uppercase()
        if (upper.isNotEmpty() && !current.contains(upper)) {
            current.add(upper)
            setWatchlistSymbols(context, current)
        }
    }

    fun removeSymbol(context: Context, symbol: String) {
        val current = getWatchlistSymbols(context).toMutableList()
        current.removeAll { it.equals(symbol.trim(), ignoreCase = true) }
        if (current.isNotEmpty()) {
            setWatchlistSymbols(context, current)
        }
    }

    fun getFeaturedSymbol(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sym = prefs.getString(KEY_FEATURED_SYMBOL, null)
        val watchlist = getWatchlistSymbols(context)
        return if (!sym.isNullOrBlank() && watchlist.contains(sym)) {
            sym
        } else {
            watchlist.firstOrNull() ?: "AUDUSD=X"
        }
    }

    fun setFeaturedSymbol(context: Context, symbol: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FEATURED_SYMBOL, symbol.trim().uppercase())
            .apply()
    }

    fun getBadgeMode(context: Context): BadgeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_BADGE_MODE, BadgeMode.MARKET_CAP.name)
        return runCatching { BadgeMode.valueOf(name ?: BadgeMode.MARKET_CAP.name) }
            .getOrDefault(BadgeMode.MARKET_CAP)
    }

    fun setBadgeMode(context: Context, mode: BadgeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BADGE_MODE, mode.name)
            .apply()
    }

    fun getSortMode(context: Context): SortMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_SORT_MODE, SortMode.MANUAL.name)
        return runCatching { SortMode.valueOf(name ?: SortMode.MANUAL.name) }
            .getOrDefault(SortMode.MANUAL)
    }

    fun setSortMode(context: Context, mode: SortMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SORT_MODE, mode.name)
            .apply()
    }

    fun getRecentSearches(context: Context): List<SearchSymbolResult> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RECENT_SEARCHES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            val list = mutableListOf<SearchSymbolResult>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SearchSymbolResult(
                        symbol = obj.optString("symbol"),
                        name = obj.optString("name"),
                        exchange = obj.optString("exchange", "Market")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun addRecentSearch(context: Context, item: SearchSymbolResult) {
        val current = getRecentSearches(context).toMutableList()
        current.removeAll { it.symbol.equals(item.symbol, ignoreCase = true) }
        current.add(0, item)
        val trimmed = current.take(10)
        val arr = JSONArray()
        for (r in trimmed) {
            arr.put(
                JSONObject().apply {
                    put("symbol", r.symbol)
                    put("name", r.name)
                    put("exchange", r.exchange)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT_SEARCHES, arr.toString())
            .apply()
    }

    fun clearRecentSearches(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_RECENT_SEARCHES)
            .apply()
    }

    fun getSortedWatchlistQuotes(context: Context): List<StockQuote> {
        val base = getWatchlistQuotes(context)
        return when (getSortMode(context)) {
            SortMode.MANUAL -> base
            // Symbols with no data yet sort to the end rather than the top.
            SortMode.PRICE_CHANGE -> base.sortedByDescending { it.change.takeUnless(Double::isNaN) ?: Double.NEGATIVE_INFINITY }
            SortMode.PERCENT_CHANGE -> base.sortedByDescending { it.changePercent.takeUnless(Double::isNaN) ?: Double.NEGATIVE_INFINITY }
            SortMode.MARKET_CAP -> base.sortedByDescending { it.marketCapNumericValue() }
            SortMode.SYMBOL -> base.sortedBy { it.symbol }
        }
    }

    fun getWatchlistQuotes(context: Context): List<StockQuote> {
        val symbols = getWatchlistSymbols(context)
        val cachedMap = loadCachedQuotesMap(context)
        return symbols.map { sym -> cachedMap[sym] ?: placeholderQuote(sym) }
    }

    fun getQuoteForSymbol(context: Context, symbol: String): StockQuote {
        val upper = symbol.trim().uppercase()
        return loadCachedQuotesMap(context)[upper] ?: placeholderQuote(upper)
    }

    /** A symbol with no data loaded yet: every value shows as "--" until Yahoo responds. */
    private fun placeholderQuote(symbol: String) = StockQuote(
        symbol = symbol,
        name = symbol,
        price = Double.NaN,
        previousClose = Double.NaN,
        marketCapLabel = "--",
        dayHigh = Double.NaN,
        dayLow = Double.NaN,
        volumeLabel = "--",
        sparkline = emptyList()
    )

    fun saveQuotes(context: Context, quotes: List<StockQuote>) {
        val existing = loadCachedQuotesMap(context).toMutableMap()
        for (q in quotes) {
            existing[q.symbol.uppercase()] = q
        }
        val arr = JSONArray()
        for (q in existing.values) {
            val obj = JSONObject().apply {
                // JSONObject rejects NaN, so unknown numbers are simply left out.
                fun putNum(key: String, v: Double) {
                    if (!v.isNaN() && !v.isInfinite()) put(key, v)
                }
                put("symbol", q.symbol)
                put("name", q.name)
                putNum("price", q.price)
                putNum("previousClose", q.previousClose)
                put("marketCapLabel", q.marketCapLabel)
                putNum("dayHigh", q.dayHigh)
                putNum("dayLow", q.dayLow)
                put("volumeLabel", q.volumeLabel)
                putNum("openPrice", q.openPrice)
                putNum("afterHoursPrice", q.afterHoursPrice)
                putNum("afterHoursChangePercent", q.afterHoursChangePercent)
                put("peRatioLabel", q.peRatioLabel)
                putNum("fiftyTwoWeekHigh", q.fiftyTwoWeekHigh)
                putNum("fiftyTwoWeekLow", q.fiftyTwoWeekLow)
                put("avgVolumeLabel", q.avgVolumeLabel)
                put("exchangeCurrencyLabel", q.exchangeCurrencyLabel)
                val sparkArr = JSONArray()
                for (pt in q.sparkline) {
                    if (!pt.isNaN()) sparkArr.put(pt.toDouble())
                }
                put("sparkline", sparkArr)
            }
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_QUOTES_JSON, arr.toString())
            .putLong(KEY_LAST_SYNC_MS, System.currentTimeMillis())
            .apply()
    }

    private fun loadCachedQuotesMap(context: Context): Map<String, StockQuote> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_QUOTES_JSON, null) ?: return emptyMap()
        return runCatching {
            val map = mutableMapOf<String, StockQuote>()
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val sym = obj.optString("symbol").uppercase()
                if (sym.isEmpty()) continue
                val sparkArr = obj.optJSONArray("sparkline")
                val sparkList = mutableListOf<Float>()
                if (sparkArr != null) {
                    for (j in 0 until sparkArr.length()) {
                        val v = sparkArr.optDouble(j, Double.NaN)
                        if (!v.isNaN()) sparkList.add(v.toFloat())
                    }
                }
                map[sym] = StockQuote(
                    symbol = sym,
                    name = obj.optString("name", sym),
                    price = obj.optDouble("price", Double.NaN),
                    previousClose = obj.optDouble("previousClose", Double.NaN),
                    marketCapLabel = obj.optString("marketCapLabel", "--"),
                    dayHigh = obj.optDouble("dayHigh", Double.NaN),
                    dayLow = obj.optDouble("dayLow", Double.NaN),
                    volumeLabel = obj.optString("volumeLabel", "--"),
                    sparkline = sparkList,
                    openPrice = obj.optDouble("openPrice", Double.NaN),
                    afterHoursPrice = obj.optDouble("afterHoursPrice", Double.NaN),
                    afterHoursChangePercent = obj.optDouble("afterHoursChangePercent", Double.NaN),
                    peRatioLabel = obj.optString("peRatioLabel", "--"),
                    fiftyTwoWeekHigh = obj.optDouble("fiftyTwoWeekHigh", Double.NaN),
                    fiftyTwoWeekLow = obj.optDouble("fiftyTwoWeekLow", Double.NaN),
                    avgVolumeLabel = obj.optString("avgVolumeLabel", "--"),
                    exchangeCurrencyLabel = obj.optString("exchangeCurrencyLabel", "")
                )
            }
            map
        }.getOrDefault(emptyMap())
    }

    fun getNewsItems(context: Context): List<NewsItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_NEWS_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            val list = mutableListOf<NewsItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    NewsItem(
                        title = obj.optString("title"),
                        publisher = obj.optString("publisher", "Yahoo Finance"),
                        link = obj.optString("link", "https://finance.yahoo.com"),
                        publishedAgo = obj.optString("publishedAgo", "Today")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun saveNewsItems(context: Context, news: List<NewsItem>) {
        if (news.isEmpty()) return
        val arr = JSONArray()
        for (item in news) {
            arr.put(
                JSONObject().apply {
                    put("title", item.title)
                    put("publisher", item.publisher)
                    put("link", item.link)
                    put("publishedAgo", item.publishedAgo)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NEWS_JSON, arr.toString())
            .apply()
    }

    fun getLastSyncTimeMs(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SYNC_MS, 0L)
    }

    fun isWelcomeSeen(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WELCOME_SEEN, false)
    }

    fun setWelcomeSeen(context: Context, seen: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WELCOME_SEEN, seen)
            .apply()
    }
}
