package com.oneui.stocks

import java.util.Locale
import kotlin.math.abs

enum class BadgeMode(val displayLabel: String) {
    MARKET_CAP("Market Cap"),
    PERCENT_CHANGE("Percentage Change"),
    PRICE_CHANGE("Price Change");

    fun next(): BadgeMode = when (this) {
        MARKET_CAP -> PERCENT_CHANGE
        PERCENT_CHANGE -> PRICE_CHANGE
        PRICE_CHANGE -> MARKET_CAP
    }
}

enum class SortMode(val displayLabel: String) {
    MANUAL("Manual"),
    PRICE_CHANGE("Price Change"),
    PERCENT_CHANGE("Percentage Change"),
    MARKET_CAP("Market Cap"),
    SYMBOL("Symbol")
}

enum class ChartRange(
    val label: String,
    val yahooRange: String,
    val yahooInterval: String
) {
    ONE_DAY("1D", "1d", "5m"),
    ONE_WEEK("1W", "5d", "15m"),
    ONE_MONTH("1M", "1mo", "1d"),
    THREE_MONTHS("3M", "3mo", "1d"),
    SIX_MONTHS("6M", "6mo", "1d"),
    YTD("YTD", "ytd", "1d"),
    ONE_YEAR("1Y", "1y", "1wk"),
    TWO_YEARS("2Y", "2y", "1wk")
}

data class StockChartSeries(
    val range: ChartRange,
    val prices: List<Float>,
    val volumes: List<Long>,
    val baselinePrice: Float,
    val xLabels: List<String>,
    val pointLabels: List<String> = emptyList()
) {
    val isPositive: Boolean
        get() = (prices.lastOrNull() ?: baselinePrice) >= baselinePrice
}

data class StockQuote(
    val symbol: String,
    val name: String,
    val price: Double,
    val previousClose: Double,
    val marketCapLabel: String,
    val dayHigh: Double,
    val dayLow: Double,
    val volumeLabel: String,
    val sparkline: List<Float>,
    val openPrice: Double = previousClose,
    val afterHoursPrice: Double = price * 1.0018,
    val afterHoursChangePercent: Double = 0.18,
    val peRatioLabel: String = "--",
    val fiftyTwoWeekHigh: Double = maxOf(dayHigh, price * 1.18),
    val fiftyTwoWeekLow: Double = minOf(dayLow, price * 0.82),
    val avgVolumeLabel: String = volumeLabel,
    val exchangeCurrencyLabel: String = ""
) {
    val change: Double
        get() = price - previousClose

    val changePercent: Double
        get() = if (previousClose != 0.0) ((price - previousClose) / previousClose) * 100.0 else 0.0

    val isPositive: Boolean
        get() = price >= previousClose

    val isForex: Boolean
        get() = symbol.endsWith("=X", ignoreCase = true) || name.contains("/")

    val arrowSymbol: String
        get() = if (isPositive) "▲" else "▼"

    fun resolvedExchangeCurrencyLabel(): String {
        if (exchangeCurrencyLabel.isNotBlank()) return exchangeCurrencyLabel
        return when {
            isForex -> "CCY · FX"
            symbol.endsWith(".AX", ignoreCase = true) -> "ASX · AUD"
            symbol.endsWith("-USD", ignoreCase = true) -> "CCC · USD"
            symbol.startsWith("^") -> "INDEX · US"
            symbol in setOf("SPY", "VOO", "IVV", "VTI", "SCHD", "VUG", "IWM", "DIA", "ARKK", "GLD", "SLV") -> "NYSEArca · USD"
            symbol in setOf("BRK-B", "LLY", "JPM", "V", "MA", "WMT", "CRM", "ORCL", "DIS", "UBER", "NKE", "BA") -> "NYSE · USD"
            else -> "NASDAQ · USD"
        }
    }

    fun marketCapNumericValue(): Double {
        val raw = marketCapLabel.trim().uppercase()
        if (raw == "--" || raw.isEmpty()) return -1.0
        val multiplier = when {
            raw.endsWith("T") -> 1_000_000_000_000.0
            raw.endsWith("B") -> 1_000_000_000.0
            raw.endsWith("M") -> 1_000_000.0
            else -> 1.0
        }
        val numPart = raw.trimEnd('T', 'B', 'M', 'K').toDoubleOrNull() ?: return -1.0
        return numPart * multiplier
    }

    fun formatValue(v: Double): String {
        if (v <= 0.0 || v.isNaN()) return formattedPriceFallback()
        return when {
            isForex && v < 10.0 -> String.format(Locale.US, "%.4f", v).trimEnd('0').let {
                if (it.substringAfter('.', "").length < 3) String.format(Locale.US, "%.3f", v) else it
            }
            v < 10.0 -> String.format(Locale.US, "%.3f", v)
            else -> String.format(Locale.US, "%.2f", v)
        }
    }

    private fun formattedPriceFallback(): String {
        return if (price > 0.0) String.format(Locale.US, "%.3f", price) else "0.00"
    }

    fun formattedPrice(): String {
        return when {
            isForex && price < 10.0 -> String.format(Locale.US, "%.3f", price)
            price < 10.0 -> String.format(Locale.US, "%.3f", price)
            else -> String.format(Locale.US, "%.2f", price)
        }
    }

    fun formattedAfterHoursPrice(): String = formatValue(afterHoursPrice)

    fun formattedAfterHoursPercent(): String {
        val sign = if (afterHoursChangePercent >= 0.0) "+" else ""
        return String.format(Locale.US, "%s%.2f%%", sign, afterHoursChangePercent)
    }

    fun formattedChange(): String {
        val diff = change
        val sign = if (diff >= 0.0) "+" else ""
        return if (isForex && price < 10.0) {
            if (abs(diff) < 0.0005 && abs(diff) > 0.00001) {
                String.format(Locale.US, "%s%.4f", sign, diff)
            } else {
                String.format(Locale.US, "%s%.3f", sign, diff)
            }
        } else {
            String.format(Locale.US, "%s%.2f", sign, diff)
        }
    }

    fun formattedPercentChange(): String {
        val pct = changePercent
        val sign = if (pct >= 0.0) "+" else ""
        return String.format(Locale.US, "%s%.2f%%", sign, pct)
    }

    fun lockScreenChangeLabel(): String {
        return formattedChange()
    }

    fun badgeText(mode: BadgeMode): String {
        return when (mode) {
            BadgeMode.MARKET_CAP -> {
                if (marketCapLabel.isNotBlank() && marketCapLabel != "--") {
                    marketCapLabel
                } else {
                    // Currencies (e.g. AUDUSD=X) & indices don't have a market cap, so show their actual change %
                    formattedPercentChange()
                }
            }
            BadgeMode.PERCENT_CHANGE -> formattedPercentChange()
            BadgeMode.PRICE_CHANGE -> formattedChange()
        }
    }
}

data class NewsItem(
    val title: String,
    val publisher: String,
    val link: String,
    val publishedAgo: String
)

data class SearchSymbolResult(
    val symbol: String,
    val name: String,
    val exchange: String
)
