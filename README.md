# Samsung Stocks (One UI Edition)

A modern **Samsung One UI 7 / One UI 8** Stocks, ETF, and Forex tracker for Android, powered by **Yahoo Finance**, featuring native **Samsung One UI Lock Screen & AOD Blur Widgets** and **One UI Home Screen Widgets**.

---

## ✨ Features

### 1. Samsung One UI Design & Ergonomics
- **Viewing Area vs. Interaction Area:** Expandable top Viewing Area (`AppBarLayout` + `CollapsingToolbarLayout`) with a large centered **Stocks** header and current date, collapsing smoothly as you scroll while keeping the **Search**, **More (`⋮`)**, and **My Symbols** watchlist cards in easy one-handed thumb reach.
- **Floating Bottom Navigation Bar:** Compact 2-tab One UI capsule navigation bar (`Stocks` and `News`) with stadium pill selection and haptic feedback.
- **One UI Focus Blocks:** High-contrast OLED black (`#000000`) background paired with `26dp` rounded dark Focus Block containers (`#151817`).
- **Portrait-Locked Layout:** Designed specifically for one-handed portrait use.

### 2. Interactive Multi-Timeframe Stock Detail Sheet
- **At Close & After Hours Quotes:** Side-by-side regular market and extended-hours price & percentage change display, plus exchange and currency info (`NASDAQ · USD`, `ASX · AUD`, `CCY · FX`).
- **8 Historical Timeframes:** Switch seamlessly between **`1D`**, **`1W`**, **`1M`**, **`3M`**, **`6M`**, **`YTD`**, **`1Y`**, and **`2Y`** with in-memory caching and parallel pre-warming.
- **Interactive Touch Scrubbing:** Slide your finger across the chart to inspect the exact price, percentage delta, and timestamp at any point on the timeline, accompanied by a volume bar strip and right-axis price grid.
- **3-Column Key Statistics Grid:** Displays `Open`, `High`, `Low` | `Vol.`, `P/E`, `Mkt Cap` (or `Prev`, `Chg`, `Chg %` for FX pairs) | `52W H`, `52W L`, `Avg Vol`.

### 3. Dedicated One UI Search & Watchlist Management
- **Bottom-Anchored Search Pill:** Dedicated search page (`SearchActivity`) with a bottom pill input bar and voice search (`🎤`).
- **Recent Searches & Popular ETFs/Stocks:** Quick-access **Recent searches** (with **Clear all**) and a **Popular ETFs & stocks** discovery section (`VOO`, `SPY`, `QQQ`, `VAS.AX`, `NVDA`, `AAPL`, `GOOG`, etc.), plus live Yahoo Finance ticker search.
- **3-Dots Floating Popover Menu:**
  - **Edit Watchlist:** Inline removal of symbols (`⊖`) with a `Done` action pill.
  - **Sort Watchlist By:** `Manual`, `Price Change`, `Percentage Change`, `Market Cap`, or `Symbol`.
  - **Watchlist Shows:** Toggle row badges between `Market Cap`, `Percentage Change`, and `Price Change`.

### 4. Samsung One UI Lock Screen Widgets (Native Blur + AOD)
Built using Samsung's `lockAndAod` widget specification with native translucent lock screen wallpaper blur (`@android:id/background` with `alpha` between `1` and `254`) and monotone AOD layouts:
- **Symbol with Graph (`2x1` Small):** Ticker symbol, directional arrow, change, and intraday sparkline.
- **Symbol (`1x1` Tiny):** Circular lock screen widget with directional arrow, ticker, and change.
- **Symbol (`2x1` Small):** Ticker header, change, and large live price readout.
- **Watchlist (`2x1` Small):** Compact 3-row watchlist summary on your lock screen.

### 5. One UI Home Screen Widgets (`2x1`, `2x2`, `4x1`, `4x2`)
Automatically synced to whichever symbol you select for your Lock Screen widget:
- **`2x1` Stock Pill:** Stadium capsule showing bold price and ticker `%` change.
- **`2x2` Stock Card:** Rounded squircle card with top-left ticker/name, middle color sparkline, and bottom-left price + `%` change.
- **`4x1` Stock Wide Pill:** Full-width capsule with ticker/name on the left, color sparkline in the center, and large price + `%` change on the right.
- **`4x2` Stock Large Card:** Large One UI card with centered header, hero price, change readout, and full-width sparkline.

---

## 📦 Building from Source

```bash
./gradlew assembleDebug assembleRelease
```

- **Debug APK:** `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK:** `app/build/outputs/apk/release/app-release.apk`
