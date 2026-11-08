package com.oneui.stocks

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object StockPreferences {
    private const val PREFS_NAME = "oneui_stocks_prefs"
    private const val KEY_SYMBOLS = "watchlist_symbols"
    private const val KEY_QUOTES_JSON = "cached_quotes_json"
    private const val KEY_NEWS_JSON = "cached_news_json"
    private const val KEY_FEATURED_SYMBOL = "featured_lock_symbol"
    private const val KEY_BADGE_MODE = "badge_mode"
    private const val KEY_SORT_MODE = "sort_mode"
    private const val KEY_RECENT_SEARCHES = "recent_searches_json"
    private const val KEY_LAST_SYNC_MS = "last_sync_ms"

    val DEFAULT_SYMBOLS = listOf(
        "AUDUSD=X",
        "AUDGBP=X",
        "AUDJPY=X",
        "AUDFJD=X",
        "AUDNZD=X",
        "AAPL",
        "GOOG"
    )

    private val SEED_QUOTES: Map<String, StockQuote> = mapOf(
        "AUDUSD=X" to StockQuote(
            symbol = "AUDUSD=X",
            name = "AUD/USD",
            price = 0.687,
            previousClose = 0.691,
            marketCapLabel = "--",
            dayHigh = 0.692,
            dayLow = 0.685,
            volumeLabel = "FX",
            sparkline = listOf(
                0.691f, 0.6905f, 0.6895f, 0.6885f, 0.688f, 0.6872f, 0.6876f, 0.6868f,
                0.6873f, 0.6869f, 0.6865f, 0.6878f, 0.6871f, 0.6869f, 0.687f
            ),
            openPrice = 0.690,
            afterHoursPrice = 0.688,
            afterHoursChangePercent = 0.14,
            peRatioLabel = "--",
            fiftyTwoWeekHigh = 0.714,
            fiftyTwoWeekLow = 0.636,
            avgVolumeLabel = "FX",
            exchangeCurrencyLabel = "CCY · USD"
        ),
        "AUDGBP=X" to StockQuote(
            symbol = "AUDGBP=X",
            name = "AUD/GBP",
            price = 0.514,
            previousClose = 0.516,
            marketCapLabel = "--",
            dayHigh = 0.517,
            dayLow = 0.512,
            volumeLabel = "FX",
            sparkline = listOf(
                0.516f, 0.5152f, 0.5148f, 0.5142f, 0.5136f, 0.5139f, 0.5145f, 0.5158f,
                0.5144f, 0.5147f, 0.5141f, 0.5138f, 0.5142f, 0.514f
            ),
            openPrice = 0.516,
            afterHoursPrice = 0.515,
            afterHoursChangePercent = 0.11,
            peRatioLabel = "--",
            fiftyTwoWeekHigh = 0.538,
            fiftyTwoWeekLow = 0.498,
            avgVolumeLabel = "FX",
            exchangeCurrencyLabel = "CCY · GBP"
        ),
        "AUDJPY=X" to StockQuote(
            symbol = "AUDJPY=X",
            name = "AUD/JPY",
            price = 108.79,
            previousClose = 109.15,
            marketCapLabel = "--",
            dayHigh = 109.32,
            dayLow = 108.45,
            volumeLabel = "FX",
            sparkline = listOf(
                109.15f, 109.10f, 108.85f, 108.92f, 108.55f, 108.78f, 108.95f, 108.80f,
                108.90f, 108.72f, 108.84f, 108.76f, 108.79f
            ),
            openPrice = 109.12,
            afterHoursPrice = 108.91,
            afterHoursChangePercent = 0.11,
            peRatioLabel = "--",
            fiftyTwoWeekHigh = 109.85,
            fiftyTwoWeekLow = 93.60,
            avgVolumeLabel = "FX",
            exchangeCurrencyLabel = "CCY · JPY"
        ),
        "AUDFJD=X" to StockQuote(
            symbol = "AUDFJD=X",
            name = "AUD/FJD",
            price = 1.546,
            previousClose = 1.548,
            marketCapLabel = "--",
            dayHigh = 1.549,
            dayLow = 1.545,
            volumeLabel = "FX",
            sparkline = listOf(
                1.546f, 1.5461f, 1.5462f, 1.5463f, 1.5464f, 1.5465f, 1.5465f, 1.5464f,
                1.5463f, 1.5462f, 1.5461f, 1.546f
            ),
            openPrice = 1.548,
            afterHoursPrice = 1.547,
            afterHoursChangePercent = 0.06,
            peRatioLabel = "--",
            fiftyTwoWeekHigh = 1.585,
            fiftyTwoWeekLow = 1.472,
            avgVolumeLabel = "FX",
            exchangeCurrencyLabel = "CCY · FJD"
        ),
        "AUDNZD=X" to StockQuote(
            symbol = "AUDNZD=X",
            name = "AUD/NZD",
            price = 1.238,
            previousClose = 1.240,
            marketCapLabel = "--",
            dayHigh = 1.242,
            dayLow = 1.236,
            volumeLabel = "FX",
            sparkline = listOf(
                1.240f, 1.2395f, 1.2398f, 1.2375f, 1.2392f, 1.2396f, 1.2390f, 1.2405f,
                1.2391f, 1.2388f, 1.2384f, 1.238f
            ),
            openPrice = 1.240,
            afterHoursPrice = 1.239,
            afterHoursChangePercent = 0.08,
            peRatioLabel = "--",
            fiftyTwoWeekHigh = 1.262,
            fiftyTwoWeekLow = 1.074,
            avgVolumeLabel = "FX",
            exchangeCurrencyLabel = "CCY · NZD"
        ),
        "AAPL" to StockQuote(
            symbol = "AAPL",
            name = "Apple Inc.",
            price = 342.37,
            previousClose = 344.20,
            marketCapLabel = "4.975T",
            dayHigh = 345.10,
            dayLow = 338.90,
            volumeLabel = "41.84M",
            sparkline = listOf(
                339.2f, 341.5f, 344.8f, 343.9f, 344.1f, 343.5f, 344.4f, 344.0f,
                343.2f, 343.7f, 342.9f, 342.37f
            ),
            openPrice = 343.10,
            afterHoursPrice = 343.12,
            afterHoursChangePercent = 0.22,
            peRatioLabel = "31.40",
            fiftyTwoWeekHigh = 368.50,
            fiftyTwoWeekLow = 216.20,
            avgVolumeLabel = "44.1M",
            exchangeCurrencyLabel = "NASDAQ · USD"
        ),
        "GOOG" to StockQuote(
            symbol = "GOOG",
            name = "Alphabet Inc.",
            price = 283.93,
            previousClose = 279.80,
            marketCapLabel = "3.491T",
            dayHigh = 289.22,
            dayLow = 283.00,
            volumeLabel = "24.62M",
            sparkline = listOf(
                285.8f, 289.1f, 286.8f, 287.6f, 285.2f, 287.1f, 286.4f, 287.4f,
                286.5f, 285.3f, 285.8f, 284.5f, 285.4f, 284.8f, 285.3f, 283.93f
            ),
            openPrice = 283.42,
            afterHoursPrice = 284.43,
            afterHoursChangePercent = 0.18,
            peRatioLabel = "14.83",
            fiftyTwoWeekHigh = 404.20,
            fiftyTwoWeekLow = 271.10,
            avgVolumeLabel = "19.2M",
            exchangeCurrencyLabel = "NASDAQ · USD"
        )
    )

    private val SEED_NEWS = listOf(
        NewsItem(
            title = "Australian Dollar Steadies Against Majors as RBA Holds Policy Outlook",
            publisher = "Yahoo Finance",
            link = "https://finance.yahoo.com/currencies",
            publishedAgo = "24m ago"
        ),
        NewsItem(
            title = "Apple and Alphabet Lead Mega-Cap Tech Momentum Amid AI Infrastructure Push",
            publisher = "Reuters • Yahoo Finance",
            link = "https://finance.yahoo.com/topic/stock-market-news",
            publishedAgo = "48m ago"
        ),
        NewsItem(
            title = "Global FX Markets Watch US Treasury Yields and Cross-Pacific Trade Flows",
            publisher = "Bloomberg • Yahoo Finance",
            link = "https://finance.yahoo.com/currencies",
            publishedAgo = "1h ago"
        ),
        NewsItem(
            title = "Semiconductor & Cloud Demand Lifts Nasdaq Futures Ahead of Earnings",
            publisher = "MarketWatch • Yahoo Finance",
            link = "https://finance.yahoo.com/topic/tech",
            publishedAgo = "2h ago"
        ),
        NewsItem(
            title = "AUD/JPY and AUD/GBP Volume Surges on Bank of Japan & BoE Rate Commentary",
            publisher = "FXStreet • Yahoo Finance",
            link = "https://finance.yahoo.com/quote/AUDJPY=X",
            publishedAgo = "3h ago"
        ),
        NewsItem(
            title = "ASX 200 Miners & Financials Gain as Iron Ore and Pacific Currencies Firm",
            publisher = "AFR • Yahoo Finance",
            link = "https://au.finance.yahoo.com",
            publishedAgo = "4h ago"
        ),
        NewsItem(
            title = "Institutional Investors Rotate Into High-Cash-Flow Big Tech Leaders",
            publisher = "Barron's • Yahoo Finance",
            link = "https://finance.yahoo.com/quote/AAPL",
            publishedAgo = "5h ago"
        ),
        NewsItem(
            title = "Oil and Gold Consolidate as Traders Weigh Global Inflation Data",
            publisher = "CNBC • Yahoo Finance",
            link = "https://finance.yahoo.com/commodities",
            publishedAgo = "6h ago"
        )
    )

    private val EXTRA_MOVER_SEEDS = listOf(
        StockQuote(
            symbol = "NVDA",
            name = "NVIDIA Corp.",
            price = 148.62,
            previousClose = 141.80,
            marketCapLabel = "3.610T",
            dayHigh = 149.50,
            dayLow = 142.10,
            volumeLabel = "68.4M",
            sparkline = listOf(141.8f, 143.2f, 144.9f, 144.1f, 146.5f, 147.8f, 148.62f),
            openPrice = 142.50,
            afterHoursPrice = 149.15,
            afterHoursChangePercent = 0.36,
            peRatioLabel = "46.20",
            fiftyTwoWeekHigh = 152.89,
            fiftyTwoWeekLow = 90.69,
            avgVolumeLabel = "61.5M",
            exchangeCurrencyLabel = "NASDAQ · USD"
        ),
        StockQuote(
            symbol = "TSLA",
            name = "Tesla, Inc.",
            price = 321.40,
            previousClose = 331.15,
            marketCapLabel = "1.110T",
            dayHigh = 332.00,
            dayLow = 318.50,
            volumeLabel = "52.1M",
            sparkline = listOf(331.15f, 329.0f, 326.4f, 327.1f, 323.8f, 322.0f, 321.4f),
            openPrice = 329.80,
            afterHoursPrice = 322.05,
            afterHoursChangePercent = 0.20,
            peRatioLabel = "64.10",
            fiftyTwoWeekHigh = 358.64,
            fiftyTwoWeekLow = 138.80,
            avgVolumeLabel = "58.0M",
            exchangeCurrencyLabel = "NASDAQ · USD"
        ),
        StockQuote(
            symbol = "MSFT",
            name = "Microsoft Corp.",
            price = 446.85,
            previousClose = 440.10,
            marketCapLabel = "3.380T",
            dayHigh = 448.20,
            dayLow = 439.80,
            volumeLabel = "19.7M",
            sparkline = listOf(440.1f, 441.5f, 443.0f, 442.4f, 445.1f, 446.85f),
            openPrice = 441.00,
            afterHoursPrice = 447.60,
            afterHoursChangePercent = 0.17,
            peRatioLabel = "34.80",
            fiftyTwoWeekHigh = 468.35,
            fiftyTwoWeekLow = 385.58,
            avgVolumeLabel = "21.3M",
            exchangeCurrencyLabel = "NASDAQ · USD"
        )
    )

    fun getBigMoversToday(context: Context): List<StockQuote> {
        val watchlistQuotes = getWatchlistQuotes(context)
        val combined = (watchlistQuotes + EXTRA_MOVER_SEEDS)
            .distinctBy { it.symbol.uppercase() }
            .sortedByDescending { kotlin.math.abs(it.changePercent) }
        return combined.take(5)
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
            SortMode.PRICE_CHANGE -> base.sortedByDescending { it.change }
            SortMode.PERCENT_CHANGE -> base.sortedByDescending { it.changePercent }
            SortMode.MARKET_CAP -> base.sortedByDescending { it.marketCapNumericValue() }
            SortMode.SYMBOL -> base.sortedBy { it.symbol }
        }
    }

    fun getWatchlistQuotes(context: Context): List<StockQuote> {
        val symbols = getWatchlistSymbols(context)
        val cachedMap = loadCachedQuotesMap(context)
        return symbols.map { sym ->
            cachedMap[sym] ?: SEED_QUOTES[sym] ?: StockQuote(
                symbol = sym,
                name = sym,
                price = 100.0,
                previousClose = 100.0,
                marketCapLabel = "--",
                dayHigh = 101.0,
                dayLow = 99.0,
                volumeLabel = "--",
                sparkline = listOf(100f, 100.2f, 99.9f, 100.1f, 100f)
            )
        }
    }

    fun getQuoteForSymbol(context: Context, symbol: String): StockQuote {
        val upper = symbol.trim().uppercase()
        val cachedMap = loadCachedQuotesMap(context)
        return cachedMap[upper] ?: SEED_QUOTES[upper] ?: getWatchlistQuotes(context).first()
    }

    fun saveQuotes(context: Context, quotes: List<StockQuote>) {
        val existing = loadCachedQuotesMap(context).toMutableMap()
        for (q in quotes) {
            existing[q.symbol.uppercase()] = q
        }
        val arr = JSONArray()
        for (q in existing.values) {
            val obj = JSONObject().apply {
                put("symbol", q.symbol)
                put("name", q.name)
                put("price", q.price)
                put("previousClose", q.previousClose)
                put("marketCapLabel", q.marketCapLabel)
                put("dayHigh", q.dayHigh)
                put("dayLow", q.dayLow)
                put("volumeLabel", q.volumeLabel)
                put("openPrice", q.openPrice)
                put("afterHoursPrice", q.afterHoursPrice)
                put("afterHoursChangePercent", q.afterHoursChangePercent)
                put("peRatioLabel", q.peRatioLabel)
                put("fiftyTwoWeekHigh", q.fiftyTwoWeekHigh)
                put("fiftyTwoWeekLow", q.fiftyTwoWeekLow)
                put("avgVolumeLabel", q.avgVolumeLabel)
                put("exchangeCurrencyLabel", q.exchangeCurrencyLabel)
                val sparkArr = JSONArray()
                for (pt in q.sparkline) {
                    sparkArr.put(pt.toDouble())
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
        val json = prefs.getString(KEY_QUOTES_JSON, null) ?: return SEED_QUOTES
        return runCatching {
            val map = SEED_QUOTES.toMutableMap()
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val sym = obj.optString("symbol").uppercase()
                val sparkArr = obj.optJSONArray("sparkline")
                val sparkList = mutableListOf<Float>()
                if (sparkArr != null) {
                    for (j in 0 until sparkArr.length()) {
                        val v = sparkArr.optDouble(j, Double.NaN)
                        if (!v.isNaN()) sparkList.add(v.toFloat())
                    }
                }
                val seed = SEED_QUOTES[sym]
                val price = obj.optDouble("price", seed?.price ?: 0.0)
                val rawPrevClose = obj.optDouble("previousClose", seed?.previousClose ?: price)
                val prevClose = when {
                    kotlin.math.abs(price - rawPrevClose) >= 0.0001 && rawPrevClose > 0.0 -> rawPrevClose
                    sparkList.isNotEmpty() && kotlin.math.abs(price - sparkList.first().toDouble()) >= 0.0001 ->
                        sparkList.first().toDouble()
                    seed != null && kotlin.math.abs(price - seed.previousClose) >= 0.0001 ->
                        seed.previousClose
                    price > 0.0 -> price * 0.9965
                    else -> rawPrevClose
                }
                val dayHigh = obj.optDouble("dayHigh", seed?.dayHigh ?: price)
                val dayLow = obj.optDouble("dayLow", seed?.dayLow ?: price)
                val volLabel = obj.optString("volumeLabel", seed?.volumeLabel ?: "--")

                map[sym] = StockQuote(
                    symbol = sym,
                    name = obj.optString("name", seed?.name ?: sym),
                    price = price,
                    previousClose = prevClose,
                    marketCapLabel = obj.optString("marketCapLabel", seed?.marketCapLabel ?: "--"),
                    dayHigh = dayHigh,
                    dayLow = dayLow,
                    volumeLabel = volLabel,
                    sparkline = if (sparkList.size >= 2) sparkList else (seed?.sparkline ?: listOf(1f, 1f)),
                    openPrice = obj.optDouble("openPrice", seed?.openPrice ?: prevClose),
                    afterHoursPrice = obj.optDouble("afterHoursPrice", seed?.afterHoursPrice ?: (price * 1.0018)),
                    afterHoursChangePercent = obj.optDouble("afterHoursChangePercent", seed?.afterHoursChangePercent ?: 0.18),
                    peRatioLabel = obj.optString("peRatioLabel", seed?.peRatioLabel ?: "--"),
                    fiftyTwoWeekHigh = obj.optDouble("fiftyTwoWeekHigh", seed?.fiftyTwoWeekHigh ?: maxOf(dayHigh, price * 1.18)),
                    fiftyTwoWeekLow = obj.optDouble("fiftyTwoWeekLow", seed?.fiftyTwoWeekLow ?: minOf(dayLow, price * 0.82)),
                    avgVolumeLabel = obj.optString("avgVolumeLabel", seed?.avgVolumeLabel ?: volLabel),
                    exchangeCurrencyLabel = obj.optString("exchangeCurrencyLabel", seed?.exchangeCurrencyLabel ?: "")
                )
            }
            map
        }.getOrDefault(SEED_QUOTES)
    }

    fun getNewsItems(context: Context): List<NewsItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_NEWS_JSON, null) ?: return SEED_NEWS
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
            if (list.isEmpty()) SEED_NEWS else list
        }.getOrDefault(SEED_NEWS)
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
}
