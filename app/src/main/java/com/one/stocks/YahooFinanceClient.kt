package com.one.stocks

import android.util.Log
import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object YahooFinanceClient {
    private const val TAG = "YahooFinanceClient"

    private const val USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private val chartCache = ConcurrentHashMap<String, StockChartSeries>()

    private fun cacheKey(symbol: String, range: ChartRange): String =
        "${symbol.trim().uppercase()}:${range.name}"

    fun getCachedChartSeries(quote: StockQuote, range: ChartRange): StockChartSeries? =
        chartCache[cacheKey(quote.symbol, range)]

    // Built-in directory of popular Global, US & Australian Stocks, ETFs, FX, Indices & Crypto for 0ms instant search
    private val LOCAL_CATALOG = listOf(
        // Popular US & Global ETFs
        SearchSymbolResult("VOO", "Vanguard S&P 500 ETF", "ETF • NYSEArca"),
        SearchSymbolResult("SPY", "SPDR S&P 500 ETF Trust", "ETF • NYSEArca"),
        SearchSymbolResult("QQQ", "Invesco QQQ Trust (Nasdaq-100 ETF)", "ETF • NASDAQ"),
        SearchSymbolResult("IVV", "iShares Core S&P 500 ETF", "ETF • NYSEArca"),
        SearchSymbolResult("VTI", "Vanguard Total Stock Market ETF", "ETF • NYSEArca"),
        SearchSymbolResult("SCHD", "Schwab U.S. Dividend Equity ETF", "ETF • NYSEArca"),
        SearchSymbolResult("VUG", "Vanguard Growth ETF", "ETF • NYSEArca"),
        SearchSymbolResult("IWM", "iShares Russell 2000 ETF", "ETF • NYSEArca"),
        SearchSymbolResult("DIA", "SPDR Dow Jones Industrial Average ETF", "ETF • NYSEArca"),
        SearchSymbolResult("ARKK", "ARK Innovation ETF", "ETF • NYSEArca"),
        SearchSymbolResult("SMH", "VanEck Semiconductor ETF", "ETF • NASDAQ"),
        SearchSymbolResult("SOXX", "iShares Semiconductor ETF", "ETF • NASDAQ"),
        SearchSymbolResult("GLD", "SPDR Gold Shares ETF", "ETF • NYSEArca"),
        SearchSymbolResult("SLV", "iShares Silver Trust ETF", "ETF • NYSEArca"),
        SearchSymbolResult("TLT", "iShares 20+ Year Treasury Bond ETF", "ETF • NASDAQ"),
        // Popular Australian (ASX) ETFs
        SearchSymbolResult("VAS.AX", "Vanguard Australian Shares Index ETF", "ETF • ASX"),
        SearchSymbolResult("IVV.AX", "iShares S&P 500 ETF (AUD)", "ETF • ASX"),
        SearchSymbolResult("VGS.AX", "Vanguard MSCI Index International Shares ETF", "ETF • ASX"),
        SearchSymbolResult("NDQ.AX", "Betashares Nasdaq 100 ETF", "ETF • ASX"),
        SearchSymbolResult("VDHG.AX", "Vanguard Diversified High Growth Index ETF", "ETF • ASX"),
        SearchSymbolResult("IOZ.AX", "iShares Core S&P/ASX 200 ETF", "ETF • ASX"),
        SearchSymbolResult("A200.AX", "Betashares Australia 200 ETF", "ETF • ASX"),
        // Major US Stocks
        SearchSymbolResult("AAPL", "Apple Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("GOOG", "Alphabet Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("GOOGL", "Alphabet Inc. (Class A)", "Stock • NASDAQ"),
        SearchSymbolResult("MSFT", "Microsoft Corporation", "Stock • NASDAQ"),
        SearchSymbolResult("NVDA", "NVIDIA Corporation", "Stock • NASDAQ"),
        SearchSymbolResult("AMZN", "Amazon.com, Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("META", "Meta Platforms, Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("TSLA", "Tesla, Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("BRK-B", "Berkshire Hathaway Inc.", "Stock • NYSE"),
        SearchSymbolResult("AVGO", "Broadcom Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("AMD", "Advanced Micro Devices, Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("NFLX", "Netflix, Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("PLTR", "Palantir Technologies Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("COIN", "Coinbase Global, Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("INTC", "Intel Corporation", "Stock • NASDAQ"),
        SearchSymbolResult("CRM", "Salesforce, Inc.", "Stock • NYSE"),
        SearchSymbolResult("ORCL", "Oracle Corporation", "Stock • NYSE"),
        SearchSymbolResult("LLY", "Eli Lilly and Company", "Stock • NYSE"),
        SearchSymbolResult("JPM", "JPMorgan Chase & Co.", "Stock • NYSE"),
        SearchSymbolResult("V", "Visa Inc.", "Stock • NYSE"),
        SearchSymbolResult("MA", "Mastercard Incorporated", "Stock • NYSE"),
        SearchSymbolResult("WMT", "Walmart Inc.", "Stock • NYSE"),
        SearchSymbolResult("COST", "Costco Wholesale Corporation", "Stock • NASDAQ"),
        SearchSymbolResult("DIS", "The Walt Disney Company", "Stock • NYSE"),
        SearchSymbolResult("UBER", "Uber Technologies, Inc.", "Stock • NYSE"),
        SearchSymbolResult("NKE", "NIKE, Inc.", "Stock • NYSE"),
        SearchSymbolResult("BA", "The Boeing Company", "Stock • NYSE"),
        // Major Australian (ASX) Stocks
        SearchSymbolResult("CBA.AX", "Commonwealth Bank of Australia", "Stock • ASX"),
        SearchSymbolResult("BHP.AX", "BHP Group Limited", "Stock • ASX"),
        SearchSymbolResult("CSL.AX", "CSL Limited", "Stock • ASX"),
        SearchSymbolResult("WBC.AX", "Westpac Banking Corporation", "Stock • ASX"),
        SearchSymbolResult("NAB.AX", "National Australia Bank Limited", "Stock • ASX"),
        SearchSymbolResult("ANZ.AX", "ANZ Group Holdings Limited", "Stock • ASX"),
        SearchSymbolResult("WES.AX", "Wesfarmers Limited", "Stock • ASX"),
        SearchSymbolResult("MQG.AX", "Macquarie Group Limited", "Stock • ASX"),
        SearchSymbolResult("WOW.AX", "Woolworths Group Limited", "Stock • ASX"),
        SearchSymbolResult("TLS.AX", "Telstra Group Limited", "Stock • ASX"),
        SearchSymbolResult("RIO.AX", "Rio Tinto Limited", "Stock • ASX"),
        SearchSymbolResult("FMG.AX", "Fortescue Ltd", "Stock • ASX"),
        // Currencies (FX)
        SearchSymbolResult("AUDUSD=X", "AUD/USD", "Currency • FX"),
        SearchSymbolResult("AUDGBP=X", "AUD/GBP", "Currency • FX"),
        SearchSymbolResult("AUDJPY=X", "AUD/JPY", "Currency • FX"),
        SearchSymbolResult("AUDFJD=X", "AUD/FJD", "Currency • FX"),
        SearchSymbolResult("AUDNZD=X", "AUD/NZD", "Currency • FX"),
        SearchSymbolResult("AUDEUR=X", "AUD/EUR", "Currency • FX"),
        SearchSymbolResult("AUDCAD=X", "AUD/CAD", "Currency • FX"),
        SearchSymbolResult("AUDSGD=X", "AUD/SGD", "Currency • FX"),
        SearchSymbolResult("AUDCHF=X", "AUD/CHF", "Currency • FX"),
        SearchSymbolResult("EURUSD=X", "EUR/USD", "Currency • FX"),
        SearchSymbolResult("GBPUSD=X", "GBP/USD", "Currency • FX"),
        SearchSymbolResult("USDJPY=X", "USD/JPY", "Currency • FX"),
        // Crypto & Indices
        SearchSymbolResult("BTC-USD", "Bitcoin USD", "Crypto • CCC"),
        SearchSymbolResult("ETH-USD", "Ethereum USD", "Crypto • CCC"),
        SearchSymbolResult("SOL-USD", "Solana USD", "Crypto • CCC"),
        SearchSymbolResult("^GSPC", "S&P 500 Index", "Index • US"),
        SearchSymbolResult("^IXIC", "NASDAQ Composite", "Index • US"),
        SearchSymbolResult("^DJI", "Dow Jones Industrial Average", "Index • US"),
        SearchSymbolResult("^AXJO", "S&P/ASX 200 Index", "Index • ASX")
    )

    private val KNOWN_NAMES = LOCAL_CATALOG.associate { it.symbol.uppercase() to it.name }

    fun searchLocalCatalog(query: String): List<SearchSymbolResult> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return LOCAL_CATALOG.filter { item ->
            item.symbol.lowercase().contains(q) ||
                item.name.lowercase().contains(q) ||
                item.exchange.lowercase().contains(q)
        }.sortedByDescending { item ->
            when {
                item.symbol.equals(q, ignoreCase = true) -> 3
                item.symbol.lowercase().startsWith(q) -> 2
                item.name.lowercase().startsWith(q) -> 1
                else -> 0
            }
        }.take(15)
    }

    fun syncAll(context: Context): Result<List<StockQuote>> {
        return runCatching {
            val symbols = StockPreferences.getWatchlistSymbols(context)
            val existingMap = StockPreferences.getWatchlistQuotes(context).associateBy { it.symbol.uppercase() }
            val updated = mutableListOf<StockQuote>()

            for (sym in symbols) {
                val fetched = runCatching { fetchSingleQuote(sym, existingMap[sym]) }.getOrNull()
                if (fetched != null) {
                    updated.add(fetched)
                } else if (existingMap[sym] != null) {
                    updated.add(existingMap.getValue(sym))
                }
            }

            // Market cap, P/E, extended-hours price and average volume aren't in the chart
            // response, so fetch them in one batch. Anything Yahoo doesn't return stays "--".
            val stats = runCatching { fetchQuoteStatistics(updated.map { it.symbol }) }
                .onFailure { Log.w(TAG, "Quote statistics fetch failed", it) }
                .getOrDefault(emptyMap())
            val merged = updated.map { q -> stats[q.symbol.uppercase()]?.let { q.withStatistics(it) } ?: q }

            if (merged.isNotEmpty()) {
                StockPreferences.saveQuotes(context, merged)
            }

            runCatching {
                // Yahoo's search returns news for simple queries only, so ask per symbol
                // (plus a general market query) and merge the results.
                val queries = symbols
                    .filterNot { it.contains("=") }
                    .take(3) + "stock market"
                val news = queries
                    .flatMap { q ->
                        runCatching { fetchMarketNews(q) }
                            .onFailure { Log.w(TAG, "News fetch failed for \"$q\"", it) }
                            .getOrDefault(emptyList())
                    }
                    .distinctBy { it.title }
                    .take(12)
                if (news.isNotEmpty()) {
                    StockPreferences.saveNewsItems(context, news)
                } else {
                    Log.w(TAG, "No news returned for $queries")
                }
            }

            StockPreferences.getWatchlistQuotes(context)
        }
    }

    fun fetchSingleQuote(symbol: String, fallback: StockQuote? = null): StockQuote {
        val cleanSymbol = symbol.trim().uppercase()
        val encoded = URLEncoder.encode(cleanSymbol, "UTF-8")
        val urlStr = "https://query1.finance.yahoo.com/v8/finance/chart/$encoded?interval=5m&range=1d&includePrePost=true"
        val body = httpGet(urlStr)
        val root = JSONObject(body)
        val resultArr = root.getJSONObject("chart").getJSONArray("result")
        val resultObj = resultArr.getJSONObject(0)
        val meta = resultObj.getJSONObject("meta")

        val price = meta.optDouble("regularMarketPrice", Double.NaN)
        val prevClose = meta.optDouble(
            "chartPreviousClose",
            meta.optDouble("previousClose", Double.NaN)
        )
        val dayHigh = meta.optDouble("regularMarketDayHigh", Double.NaN)
        val dayLow = meta.optDouble("regularMarketDayLow", Double.NaN)
        val rawVolume = meta.optLong("regularMarketVolume", 0L)
        val fiftyTwoHigh = meta.optDouble("fiftyTwoWeekHigh", Double.NaN)
        val fiftyTwoLow = meta.optDouble("fiftyTwoWeekLow", Double.NaN)

        val exchName = meta.optString("fullExchangeName", meta.optString("exchangeName", "")).trim()
        val currency = meta.optString("currency", "USD").trim()
        val normalizedExch = when {
            exchName.contains("Nasdaq", ignoreCase = true) || exchName.equals("NMS", ignoreCase = true) -> "NASDAQ"
            exchName.contains("NYSEArca", ignoreCase = true) || exchName.equals("PCX", ignoreCase = true) -> "NYSEArca"
            exchName.contains("NYSE", ignoreCase = true) || exchName.equals("NYQ", ignoreCase = true) -> "NYSE"
            exchName.contains("ASX", ignoreCase = true) -> "ASX"
            exchName.isNotEmpty() -> exchName
            else -> ""
        }
        val exchCurrencyLabel = if (normalizedExch.isNotEmpty() && currency.isNotEmpty()) {
            "$normalizedExch · $currency"
        } else {
            ""
        }

        val rawName = meta.optString("shortName", meta.optString("longName", "")).trim()
        val displayName = KNOWN_NAMES[cleanSymbol]
            ?: rawName.ifEmpty { fallback?.name ?: cleanSymbol }

        val isFx = cleanSymbol.endsWith("=X") || displayName.contains("/")

        val sparkPoints = mutableListOf<Float>()
        var openVal = Double.NaN
        val indicators = resultObj.optJSONObject("indicators")
        val quoteArr = indicators?.optJSONArray("quote")
        if (quoteArr != null && quoteArr.length() > 0) {
            val quoteObj = quoteArr.getJSONObject(0)
            val openArr = quoteObj.optJSONArray("open")
            if (openArr != null) {
                for (i in 0 until openArr.length()) {
                    val o = openArr.optDouble(i, Double.NaN)
                    if (!o.isNaN() && o > 0.0) {
                        openVal = o
                        break
                    }
                }
            }
            val closeArr = quoteObj.optJSONArray("close")
            if (closeArr != null) {
                val step = maxOf(1, closeArr.length() / 36)
                var idx = 0
                while (idx < closeArr.length()) {
                    val v = closeArr.optDouble(idx, Double.NaN)
                    if (!v.isNaN() && v > 0.0) {
                        sparkPoints.add(v.toFloat())
                    }
                    idx += step
                }
            }
        }
        if (price > 0.0) {
            sparkPoints.add(price.toFloat())
        }
        val finalSpark = if (sparkPoints.size >= 2) sparkPoints else emptyList()

        val volStr = if (isFx) "24h FX" else formatCompactNumber(rawVolume)

        // Market cap, P/E, extended-hours price and average volume come from
        // fetchQuoteStatistics(); until then they show as "--".
        return StockQuote(
            symbol = cleanSymbol,
            name = displayName,
            price = price,
            previousClose = prevClose,
            marketCapLabel = if (isFx) "--" else fallback?.marketCapLabel ?: "--",
            dayHigh = dayHigh,
            dayLow = dayLow,
            volumeLabel = volStr,
            sparkline = finalSpark,
            openPrice = openVal,
            afterHoursPrice = Double.NaN,
            afterHoursChangePercent = Double.NaN,
            peRatioLabel = if (isFx) "--" else fallback?.peRatioLabel ?: "--",
            fiftyTwoWeekHigh = fiftyTwoHigh,
            fiftyTwoWeekLow = fiftyTwoLow,
            avgVolumeLabel = if (isFx) "24h FX" else fallback?.avgVolumeLabel ?: "--",
            exchangeCurrencyLabel = exchCurrencyLabel
        )
    }

    /** Live figures from Yahoo's quote endpoint that the chart endpoint doesn't include. */
    data class QuoteStatistics(
        val marketCap: Double,
        val trailingPe: Double,
        val postMarketPrice: Double,
        val postMarketChangePercent: Double,
        val averageVolume: Long,
        val fiftyTwoWeekHigh: Double,
        val fiftyTwoWeekLow: Double
    )

    private fun StockQuote.withStatistics(st: QuoteStatistics): StockQuote = copy(
        marketCapLabel = if (isForex) "--" else formatMarketCap(st.marketCap),
        peRatioLabel = if (isForex || st.trailingPe.isNaN() || st.trailingPe <= 0.0) {
            "--"
        } else {
            String.format(Locale.US, "%.2f", st.trailingPe)
        },
        afterHoursPrice = st.postMarketPrice,
        afterHoursChangePercent = st.postMarketChangePercent,
        avgVolumeLabel = if (isForex) "24h FX" else formatCompactNumber(st.averageVolume),
        fiftyTwoWeekHigh = if (fiftyTwoWeekHigh.isNaN()) st.fiftyTwoWeekHigh else fiftyTwoWeekHigh,
        fiftyTwoWeekLow = if (fiftyTwoWeekLow.isNaN()) st.fiftyTwoWeekLow else fiftyTwoWeekLow
    )

    @Volatile
    private var crumb: String? = null

    init {
        // Yahoo's quote endpoint needs a session cookie plus a matching "crumb" token.
        if (java.net.CookieHandler.getDefault() == null) {
            java.net.CookieHandler.setDefault(java.net.CookieManager())
        }
    }

    private fun obtainCrumb(forceRefresh: Boolean): String {
        if (!forceRefresh) crumb?.let { return it }
        // This page answers 404 but sets the session cookie, so the status is ignored.
        runCatching {
            val conn = (URL("https://fc.yahoo.com/").openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try { conn.responseCode } finally { conn.disconnect() }
        }
        // The crumb is plain text; asking for JSON gets a 406.
        val fresh = httpGet("https://query1.finance.yahoo.com/v1/test/getcrumb", accept = "*/*").trim()
        check(fresh.isNotEmpty() && !fresh.contains(" ") && fresh.length < 40) { "Unexpected crumb response" }
        crumb = fresh
        return fresh
    }

    fun fetchQuoteStatistics(symbols: List<String>): Map<String, QuoteStatistics> {
        if (symbols.isEmpty()) return emptyMap()
        val joined = URLEncoder.encode(symbols.joinToString(",") { it.trim().uppercase() }, "UTF-8")
        fun request(token: String): String = httpGet(
            "https://query1.finance.yahoo.com/v7/finance/quote?symbols=$joined&crumb=" +
                URLEncoder.encode(token, "UTF-8")
        )
        val body = runCatching { request(obtainCrumb(forceRefresh = false)) }
            .getOrElse { request(obtainCrumb(forceRefresh = true)) }

        val results = JSONObject(body).getJSONObject("quoteResponse").optJSONArray("result")
            ?: return emptyMap()
        val out = mutableMapOf<String, QuoteStatistics>()
        for (i in 0 until results.length()) {
            val q = results.getJSONObject(i)
            val sym = q.optString("symbol").uppercase()
            if (sym.isEmpty()) continue
            out[sym] = QuoteStatistics(
                marketCap = q.optDouble("marketCap", Double.NaN),
                trailingPe = q.optDouble("trailingPE", Double.NaN),
                postMarketPrice = q.optDouble("postMarketPrice", Double.NaN),
                postMarketChangePercent = q.optDouble("postMarketChangePercent", Double.NaN),
                averageVolume = q.optLong("averageDailyVolume3Month", 0L),
                fiftyTwoWeekHigh = q.optDouble("fiftyTwoWeekHigh", Double.NaN),
                fiftyTwoWeekLow = q.optDouble("fiftyTwoWeekLow", Double.NaN)
            )
        }
        return out
    }

    private fun formatMarketCap(value: Double): String {
        if (value.isNaN() || value <= 0.0) return "--"
        return when {
            value >= 1e12 -> String.format(Locale.US, "%.3fT", value / 1e12)
            value >= 1e9 -> String.format(Locale.US, "%.1fB", value / 1e9)
            value >= 1e6 -> String.format(Locale.US, "%.1fM", value / 1e6)
            else -> String.format(Locale.US, "%.0f", value)
        }
    }

    /** Real price history from Yahoo, or null if it couldn't be loaded. Failures aren't cached. */
    fun fetchChartSeries(quote: StockQuote, range: ChartRange): StockChartSeries? {
        val key = cacheKey(quote.symbol, range)
        chartCache[key]?.let { return it }

        val fetched = runCatching {
            val encoded = URLEncoder.encode(quote.symbol.trim().uppercase(), "UTF-8")
            val urlStr =
                "https://query1.finance.yahoo.com/v8/finance/chart/$encoded?interval=${range.yahooInterval}&range=${range.yahooRange}"
            val body = httpGet(urlStr)
            val root = JSONObject(body)
            val resultObj = root.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
            val meta = resultObj.getJSONObject("meta")
            val timestamps = resultObj.optJSONArray("timestamp")
            val quoteObj = resultObj.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
            val closeArr = quoteObj.optJSONArray("close")
            val volArr = quoteObj.optJSONArray("volume")

            val rawPrices = mutableListOf<Float>()
            val rawVolumes = mutableListOf<Long>()
            val rawTimes = mutableListOf<Long>()

            if (closeArr != null) {
                val total = closeArr.length()
                val step = maxOf(1, total / 72)
                var i = 0
                while (i < total) {
                    val p = closeArr.optDouble(i, Double.NaN)
                    if (!p.isNaN() && p > 0.0) {
                        rawPrices.add(p.toFloat())
                        val v = volArr?.optLong(i, 0L) ?: 0L
                        rawVolumes.add(maxOf(0L, v))
                        val ts = timestamps?.optLong(i, 0L) ?: 0L
                        rawTimes.add(ts)
                    }
                    i += step
                }
            }

            if (rawPrices.size < 3) {
                return@runCatching null
            }

            val baseline = if (range == ChartRange.ONE_DAY) {
                meta.optDouble("chartPreviousClose", rawPrices.first().toDouble()).toFloat()
            } else {
                rawPrices.first()
            }

            val labels = buildAxisLabelsFromTimestamps(rawTimes, range)
            val pointLabels = buildPointLabelsFromTimestamps(rawTimes, rawPrices.size, range)
            StockChartSeries(
                range = range,
                prices = rawPrices,
                volumes = if (rawVolumes.any { it > 0L }) rawVolumes else emptyList(),
                baselinePrice = baseline,
                xLabels = labels,
                pointLabels = pointLabels
            )
        }.onFailure { Log.w(TAG, "Chart fetch failed for ${quote.symbol} ${range.label}", it) }
            .getOrNull()

        if (fetched != null) chartCache[key] = fetched
        return fetched
    }

    private fun buildPointLabelsFromTimestamps(
        timestamps: List<Long>,
        count: Int,
        range: ChartRange
    ): List<String> {
        if (timestamps.size < count || timestamps.all { it <= 0L }) {
            return List(count) { "" }
        }
        val fmt = when (range) {
            ChartRange.ONE_DAY -> SimpleDateFormat("h:mm a", Locale.US)
            ChartRange.ONE_WEEK -> SimpleDateFormat("EEE h:mm a", Locale.US)
            ChartRange.ONE_MONTH, ChartRange.THREE_MONTHS -> SimpleDateFormat("d MMM", Locale.US)
            ChartRange.SIX_MONTHS, ChartRange.YTD, ChartRange.ONE_YEAR -> SimpleDateFormat("d MMM yyyy", Locale.US)
            ChartRange.TWO_YEARS -> SimpleDateFormat("d MMM yyyy", Locale.US)
        }
        return timestamps.map { ts ->
            if (ts > 0L) fmt.format(Date(ts * 1000L)) else ""
        }
    }

    private fun buildAxisLabelsFromTimestamps(timestamps: List<Long>, range: ChartRange): List<String> {
        val valid = timestamps.filter { it > 0L }
        if (valid.size < 6) {
            return List(6) { "" }
        }
        val fmt = when (range) {
            ChartRange.ONE_DAY -> SimpleDateFormat("h", Locale.US)
            ChartRange.ONE_WEEK -> SimpleDateFormat("EEE", Locale.US)
            ChartRange.ONE_MONTH, ChartRange.THREE_MONTHS -> SimpleDateFormat("d MMM", Locale.US)
            ChartRange.SIX_MONTHS, ChartRange.YTD, ChartRange.ONE_YEAR -> SimpleDateFormat("MMM", Locale.US)
            ChartRange.TWO_YEARS -> SimpleDateFormat("MMM yy", Locale.US)
        }
        val out = mutableListOf<String>()
        for (slot in 1..6) {
            val idx = ((valid.size - 1) * slot / 7).coerceIn(0, valid.lastIndex)
            out.add(fmt.format(Date(valid[idx] * 1000L)))
        }
        return out
    }

    fun searchSymbols(query: String): List<SearchSymbolResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val combined = mutableListOf<SearchSymbolResult>()
        combined.addAll(searchLocalCatalog(trimmed))

        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        val endpoints = listOf(
            "https://query1.finance.yahoo.com/v1/finance/search?q=$encoded&lang=en-US&region=US&quotesCount=15&newsCount=0",
            "https://query2.finance.yahoo.com/v1/finance/search?q=$encoded&quotesCount=15&newsCount=0"
        )

        for (urlStr in endpoints) {
            val fetched = runCatching {
                val body = httpGet(urlStr)
                val root = JSONObject(body)
                val quotesArr = root.optJSONArray("quotes")
                val list = mutableListOf<SearchSymbolResult>()
                if (quotesArr != null) {
                    for (i in 0 until quotesArr.length()) {
                        val item = quotesArr.getJSONObject(i)
                        val sym = item.optString("symbol").trim()
                        if (sym.isEmpty()) continue
                        val name = item.optString(
                            "shortname",
                            item.optString("longname", sym)
                        ).trim()
                        val typeDisp = item.optString("typeDisp", "")
                        val exchDisp = item.optString("exchDisp", "Market")
                        val exchLabel = if (typeDisp.isNotEmpty()) "$typeDisp • $exchDisp" else exchDisp
                        list.add(SearchSymbolResult(symbol = sym, name = name, exchange = exchLabel))
                    }
                }
                list
            }.getOrDefault(emptyList())

            if (fetched.isNotEmpty()) {
                combined.addAll(fetched)
                break
            }
        }

        // If user typed a specific ticker not yet in list, verify via v8 chart API
        val upper = trimmed.uppercase()
        if (combined.none { it.symbol.equals(upper, ignoreCase = true) } && !upper.contains(" ")) {
            runCatching {
                val quote = fetchSingleQuote(upper)
                combined.add(0, SearchSymbolResult(quote.symbol, quote.name, "Yahoo Finance"))
            }
        }

        return combined.distinctBy { it.symbol.uppercase() }.take(20)
    }

    fun fetchMarketNews(query: String): List<NewsItem> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val urlStr = "https://query2.finance.yahoo.com/v1/finance/search?q=$encoded&quotesCount=0&newsCount=10"
        val body = httpGet(urlStr)
        val root = JSONObject(body)
        val newsArr = root.optJSONArray("news") ?: return emptyList()
        val list = mutableListOf<NewsItem>()
        val nowSec = System.currentTimeMillis() / 1000L
        for (i in 0 until newsArr.length()) {
            val obj = newsArr.getJSONObject(i)
            val title = obj.optString("title").trim()
            if (title.isEmpty()) continue
            val publisher = obj.optString("publisher", "Yahoo Finance").trim()
            val link = obj.optString("link", "https://finance.yahoo.com").trim()
            val pubSec = obj.optLong("providerPublishTime", nowSec)
            val diffMin = maxOf(1L, (nowSec - pubSec) / 60L)
            val ago = when {
                diffMin < 60L -> "${diffMin}m ago"
                diffMin < 1440L -> "${diffMin / 60L}h ago"
                else -> "${diffMin / 1440L}d ago"
            }
            list.add(
                NewsItem(
                    title = title,
                    publisher = "$publisher • Yahoo Finance",
                    link = link,
                    publishedAgo = ago
                )
            )
        }
        return list
    }

    private fun formatCompactNumber(value: Long): String {
        if (value <= 0L) return "--"
        return when {
            value >= 1_000_000_000L -> String.format(Locale.US, "%.2fB", value / 1_000_000_000.0)
            value >= 1_000_000L -> String.format(Locale.US, "%.2fM", value / 1_000_000.0)
            value >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    private fun httpGet(urlStr: String, accept: String = "application/json"): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", accept)
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code for $urlStr")
            }
            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                return sb.toString()
            }
        } finally {
            conn.disconnect()
        }
    }
}
