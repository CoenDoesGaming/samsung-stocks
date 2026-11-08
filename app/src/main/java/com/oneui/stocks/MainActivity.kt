package com.oneui.stocks

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val chartExecutor = Executors.newFixedThreadPool(4)

    private lateinit var rootCoordinator: CoordinatorLayout
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var expandedViewingBox: LinearLayout
    private lateinit var tvExpandedTitle: TextView
    private lateinit var tvCollapsedTitle: TextView
    private lateinit var tvHeaderDate: TextView
    private lateinit var btnDoneEditing: TextView
    private lateinit var btnMore: ImageButton
    private lateinit var mainScrollView: NestedScrollView
    private lateinit var tvSyncStatus: TextView

    private lateinit var sectionWatchlist: LinearLayout
    private lateinit var sectionNewsFull: LinearLayout

    private lateinit var watchlistContainer: LinearLayout
    private lateinit var moversContainer: LinearLayout
    private lateinit var newsFullContainer: LinearLayout

    private lateinit var floatingBottomNav: LinearLayout
    private lateinit var navTabStocks: LinearLayout
    private lateinit var navTabNews: LinearLayout

    private lateinit var iconTabStocks: ImageView
    private lateinit var iconTabNews: ImageView

    private lateinit var labelTabStocks: TextView
    private lateinit var labelTabNews: TextView

    private var isEditingWatchlist: Boolean = false

    private val colorActiveMint = Color.parseColor("#FF82DFB5")
    private val colorInactiveSage = Color.parseColor("#FFA3B8AE")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupWindowInsets()
        setupOneUiHeaderAndNavigation()

        renderAllFromCache()
        triggerLiveYahooRefresh(showToast = false)
    }

    override fun onResume() {
        super.onResume()
        renderAllFromCache()
        triggerLiveYahooRefresh(showToast = false)
    }

    private fun bindViews() {
        rootCoordinator = findViewById(R.id.root_coordinator)
        appBarLayout = findViewById(R.id.app_bar_layout)
        expandedViewingBox = findViewById(R.id.expanded_viewing_box)
        tvExpandedTitle = findViewById(R.id.tv_expanded_title)
        tvCollapsedTitle = findViewById(R.id.tv_collapsed_title)
        tvHeaderDate = findViewById(R.id.tv_header_date)
        btnDoneEditing = findViewById(R.id.btn_done_editing)
        btnMore = findViewById(R.id.btn_more)
        mainScrollView = findViewById(R.id.main_scroll_view)
        tvSyncStatus = findViewById(R.id.tv_sync_status)

        sectionWatchlist = findViewById(R.id.section_watchlist)
        sectionNewsFull = findViewById(R.id.section_news_full)

        watchlistContainer = findViewById(R.id.watchlist_container)
        moversContainer = findViewById(R.id.movers_container)
        newsFullContainer = findViewById(R.id.news_full_container)

        floatingBottomNav = findViewById(R.id.floating_bottom_nav)
        navTabStocks = findViewById(R.id.nav_tab_stocks)
        navTabNews = findViewById(R.id.nav_tab_news)

        iconTabStocks = findViewById(R.id.icon_tab_stocks)
        iconTabNews = findViewById(R.id.icon_tab_news)

        labelTabStocks = findViewById(R.id.label_tab_stocks)
        labelTabNews = findViewById(R.id.label_tab_news)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootCoordinator) { _, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            rootCoordinator.setPadding(bars.left, bars.top, bars.right, 0)

            val navLp = floatingBottomNav.layoutParams as? ViewGroup.MarginLayoutParams
            if (navLp != null) {
                navLp.bottomMargin = bars.bottom + dp(12)
                floatingBottomNav.layoutParams = navLp
            }

            mainScrollView.setPadding(
                0,
                dp(4),
                0,
                bars.bottom + dp(88)
            )
            windowInsets
        }
    }

    private fun setupOneUiHeaderAndNavigation() {
        val dateFormat = SimpleDateFormat("d MMMM", Locale.UK)
        tvHeaderDate.text = dateFormat.format(Date())

        appBarLayout.addOnOffsetChangedListener { appBar, verticalOffset ->
            val totalRange = appBar.totalScrollRange.takeIf { it > 0 } ?: return@addOnOffsetChangedListener
            val fraction = (abs(verticalOffset).toFloat() / totalRange.toFloat()).coerceIn(0f, 1f)

            expandedViewingBox.alpha = (1f - fraction * 1.45f).coerceIn(0f, 1f)
            tvCollapsedTitle.alpha = ((fraction - 0.65f) / 0.35f).coerceIn(0f, 1f)
        }

        // Done button to exit Edit Watchlist mode
        btnDoneEditing.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            setEditWatchlistMode(false)
        }

        // Search icon opens the dedicated Search Page (SearchActivity)
        findViewById<ImageButton>(R.id.btn_top_search).setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            startActivity(Intent(this, SearchActivity::class.java))
        }

        // 3-dots button opens the floating popover menu matching Image 2
        btnMore.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            showThreeDotsPopoverMenu(v)
        }

        // Compact 2-Tab Newer One UI Floating Navigation Bar (Stocks, News)
        navTabStocks.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            selectBottomTab(0)
        }
        navTabNews.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            selectBottomTab(1)
        }
    }

    private fun setEditWatchlistMode(enabled: Boolean) {
        isEditingWatchlist = enabled
        btnDoneEditing.visibility = if (enabled) View.VISIBLE else View.GONE
        renderAllFromCache()
    }

    private fun selectBottomTab(index: Int) {
        if (index != 0 && isEditingWatchlist) {
            isEditingWatchlist = false
            btnDoneEditing.visibility = View.GONE
        }
        sectionWatchlist.visibility = if (index == 0) View.VISIBLE else View.GONE
        sectionNewsFull.visibility = if (index == 1) View.VISIBLE else View.GONE

        val title = if (index == 0) "Stocks" else "News"
        tvExpandedTitle.text = title
        tvCollapsedTitle.text = title

        val tabs = listOf(navTabStocks, navTabNews)
        val icons = listOf(iconTabStocks, iconTabNews)
        val labels = listOf(labelTabStocks, labelTabNews)

        for (i in tabs.indices) {
            val selected = (i == index)
            tabs[i].setBackgroundResource(if (selected) R.drawable.bg_tab_selected_pill else 0)
            val tintColor = if (selected) colorActiveMint else colorInactiveSage
            ImageViewCompat.setImageTintList(icons[i], ColorStateList.valueOf(tintColor))
            labels[i].setTextColor(tintColor)
            labels[i].setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }

        mainScrollView.smoothScrollTo(0, 0)
    }

    private fun renderAllFromCache() {
        val quotes = StockPreferences.getSortedWatchlistQuotes(this)
        val badgeMode = StockPreferences.getBadgeMode(this)
        val inflater = LayoutInflater.from(this)

        watchlistContainer.removeAllViews()
        for (i in quotes.indices) {
            val quote = quotes[i]
            val rowView = inflater.inflate(R.layout.item_stock_row, watchlistContainer, false)

            val btnRemoveRow = rowView.findViewById<ImageButton>(R.id.btn_remove_row)
            val tvSymbol = rowView.findViewById<TextView>(R.id.tv_symbol)
            val tvName = rowView.findViewById<TextView>(R.id.tv_name)
            val sparkline = rowView.findViewById<SparklineView>(R.id.sparkline_view)
            val tvPrice = rowView.findViewById<TextView>(R.id.tv_price)
            val tvBadge = rowView.findViewById<TextView>(R.id.tv_badge)
            val divider = rowView.findViewById<View>(R.id.row_divider)

            tvSymbol.text = quote.symbol
            tvName.text = quote.name
            tvPrice.text = quote.formattedPrice()
            tvBadge.text = quote.badgeText(badgeMode)
            tvBadge.setBackgroundResource(
                if (quote.isPositive) R.drawable.bg_badge_green else R.drawable.bg_badge_red
            )

            sparkline.setSparklineData(
                points = quote.sparkline,
                previousClose = quote.previousClose.toFloat(),
                isPositive = quote.isPositive,
                monochromeLockStyle = false
            )

            // Show red remove button when Edit Watchlist mode is active
            btnRemoveRow.visibility = if (isEditingWatchlist) View.VISIBLE else View.GONE
            btnRemoveRow.setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                if (StockPreferences.getWatchlistSymbols(this).size <= 1) {
                    Toast.makeText(
                        this,
                        "Keep at least 1 symbol in your Watchlist",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    StockPreferences.removeSymbol(this, quote.symbol)
                    WidgetUpdateHelper.renderAllWidgetsFromCache(this)
                    renderAllFromCache()
                    Toast.makeText(
                        this,
                        "Removed ${quote.symbol}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            if (i == quotes.lastIndex) {
                divider.visibility = View.INVISIBLE
            }

            tvBadge.setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                val nextMode = StockPreferences.getBadgeMode(this).next()
                StockPreferences.setBadgeMode(this, nextMode)
                renderAllFromCache()
            }

            rowView.setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                if (isEditingWatchlist) {
                    btnRemoveRow.performClick()
                } else {
                    showStockDetailBottomSheet(quote)
                }
            }

            watchlistContainer.addView(rowView)
        }

        renderNewsAndMoversTab()
    }

    private fun renderNewsAndMoversTab() {
        val movers = StockPreferences.getBigMoversToday(this)
        val inflater = LayoutInflater.from(this)
        moversContainer.removeAllViews()

        for (i in movers.indices) {
            val quote = movers[i]
            val rowView = inflater.inflate(R.layout.item_stock_row, moversContainer, false)

            val tvSymbol = rowView.findViewById<TextView>(R.id.tv_symbol)
            val tvName = rowView.findViewById<TextView>(R.id.tv_name)
            val sparkline = rowView.findViewById<SparklineView>(R.id.sparkline_view)
            val tvPrice = rowView.findViewById<TextView>(R.id.tv_price)
            val tvBadge = rowView.findViewById<TextView>(R.id.tv_badge)
            val divider = rowView.findViewById<View>(R.id.row_divider)

            val dirArrow = if (quote.isPositive) "↗" else "↙"
            tvSymbol.text = "$dirArrow  ${quote.symbol}"
            tvName.text = "${quote.name} • Vol ${quote.volumeLabel}"
            tvPrice.text = quote.formattedPrice()
            tvBadge.text = quote.formattedPercentChange()
            tvBadge.setBackgroundResource(
                if (quote.isPositive) R.drawable.bg_badge_green else R.drawable.bg_badge_red
            )

            sparkline.setSparklineData(
                points = quote.sparkline,
                previousClose = quote.previousClose.toFloat(),
                isPositive = quote.isPositive,
                monochromeLockStyle = false
            )

            if (i == movers.lastIndex) {
                divider.visibility = View.INVISIBLE
            }

            rowView.setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                showStockDetailBottomSheet(quote)
            }

            moversContainer.addView(rowView)
        }

        val newsItems = StockPreferences.getNewsItems(this)
        newsFullContainer.removeAllViews()
        for (i in newsItems.indices) {
            newsFullContainer.addView(
                createNewsRowView(newsItems[i], showDivider = i != newsItems.lastIndex)
            )
        }
    }

    private fun createNewsRowView(item: NewsItem, showDivider: Boolean): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(11), 0, dp(11))
            isClickable = true
            isFocusable = true
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link)))
                }
            }
        }

        val pubTv = TextView(this).apply {
            text = "${item.publisher} • ${item.publishedAgo}"
            setTextColor(Color.parseColor("#FF96ABA2"))
            textSize = 12f
        }
        val titleTv = TextView(this).apply {
            text = item.title
            setTextColor(Color.WHITE)
            textSize = 15.5f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(3), 0, 0)
        }

        container.addView(pubTv)
        container.addView(titleTv)

        if (showDivider) {
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply {
                    topMargin = dp(11)
                }
                setBackgroundColor(Color.parseColor("#FF222826"))
            }
            container.addView(divider)
        }
        return container
    }

    // Floating 3-Dots Popover Menu matching Image 2 (Edit Watchlist, Sort Watchlist By, Watchlist Shows, Feedback)
    private fun showThreeDotsPopoverMenu(anchor: View) {
        val popupWidth = dp(256)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_popover_menu)
            setPadding(0, dp(8), 0, dp(8))
            elevation = dp(22).toFloat()
        }

        val popup = PopupWindow(
            card,
            popupWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = dp(22).toFloat()
        }

        fun addMenuRow(
            iconRes: Int,
            title: String,
            subtitle: String? = null,
            showChevron: Boolean = false,
            onClick: () -> Unit
        ) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(12), dp(16), dp(12))
                isClickable = true
                isFocusable = true
                setOnClickListener { v ->
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    popup.dismiss()
                    onClick()
                }
            }

            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                    marginEnd = dp(14)
                }
                setImageResource(iconRes)
            }
            row.addView(icon)

            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(this).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 16f
            })
            if (!subtitle.isNullOrEmpty()) {
                textCol.addView(TextView(this).apply {
                    text = subtitle
                    setTextColor(Color.parseColor("#FF8E8E93"))
                    textSize = 13.5f
                    setPadding(0, dp(2), 0, 0)
                })
            }
            row.addView(textCol)

            if (showChevron) {
                val chevron = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                        marginStart = dp(8)
                    }
                    setImageResource(R.drawable.ic_chevron_right)
                }
                row.addView(chevron)
            }

            card.addView(row)
        }

        fun addDivider() {
            card.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply {
                    topMargin = dp(4)
                    bottomMargin = dp(4)
                    marginStart = dp(18)
                    marginEnd = dp(18)
                }
                setBackgroundColor(Color.parseColor("#FF383A40"))
            })
        }

        // 1. Edit Watchlist
        addMenuRow(
            iconRes = R.drawable.ic_edit_pencil,
            title = if (isEditingWatchlist) "Done Editing" else "Edit Watchlist"
        ) {
            selectBottomTab(0)
            setEditWatchlistMode(!isEditingWatchlist)
        }

        addDivider()

        // 2. Sort Watchlist By >
        val currentSort = StockPreferences.getSortMode(this)
        addMenuRow(
            iconRes = R.drawable.ic_sort_arrows,
            title = "Sort Watchlist By",
            subtitle = currentSort.displayLabel,
            showChevron = true
        ) {
            showSortWatchlistSheet()
        }

        // 3. Watchlist Shows >
        val currentBadge = StockPreferences.getBadgeMode(this)
        addMenuRow(
            iconRes = R.drawable.ic_chart_shows,
            title = "Watchlist Shows",
            subtitle = currentBadge.displayLabel,
            showChevron = true
        ) {
            showWatchlistShowsSheet()
        }

        popup.showAsDropDown(anchor, -popupWidth + anchor.width - dp(6), dp(6))
    }

    private fun showSortWatchlistSheet() {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(26))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    dp(28).toFloat(), dp(28).toFloat(),
                    dp(28).toFloat(), dp(28).toFloat(),
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#FF181A1E"))
            }
        }

        root.addView(TextView(this).apply {
            text = "Sort Watchlist By"
            setTextColor(Color.WHITE)
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(10))
        })

        val currentSort = StockPreferences.getSortMode(this)
        for (mode in SortMode.values()) {
            val isSelected = (mode == currentSort)
            root.addView(TextView(this).apply {
                text = if (isSelected) "✓   ${mode.displayLabel}" else "      ${mode.displayLabel}"
                setTextColor(if (isSelected) colorActiveMint else Color.WHITE)
                textSize = 16f
                setTypeface(typeface, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                setPadding(dp(6), dp(13), dp(6), dp(13))
                setOnClickListener { v ->
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    StockPreferences.setSortMode(this@MainActivity, mode)
                    renderAllFromCache()
                    dialog.dismiss()
                }
            })
        }

        dialog.setContentView(root)
        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
        dialog.show()
    }

    private fun showWatchlistShowsSheet() {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(26))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    dp(28).toFloat(), dp(28).toFloat(),
                    dp(28).toFloat(), dp(28).toFloat(),
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#FF181A1E"))
            }
        }

        root.addView(TextView(this).apply {
            text = "Watchlist Shows"
            setTextColor(Color.WHITE)
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(10))
        })

        val currentBadge = StockPreferences.getBadgeMode(this)
        for (mode in BadgeMode.values()) {
            val isSelected = (mode == currentBadge)
            root.addView(TextView(this).apply {
                text = if (isSelected) "✓   ${mode.displayLabel}" else "      ${mode.displayLabel}"
                setTextColor(if (isSelected) colorActiveMint else Color.WHITE)
                textSize = 16f
                setTypeface(typeface, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                setPadding(dp(6), dp(13), dp(6), dp(13))
                setOnClickListener { v ->
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    StockPreferences.setBadgeMode(this@MainActivity, mode)
                    renderAllFromCache()
                    dialog.dismiss()
                }
            })
        }

        dialog.setContentView(root)
        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
        dialog.show()
    }

    private fun triggerLiveYahooRefresh(showToast: Boolean) {
        tvSyncStatus.text = "Syncing Yahoo Finance..."
        WidgetUpdateHelper.refreshFromYahooAsync(this) { result ->
            runOnUiThread {
                renderAllFromCache()
                if (result.isSuccess) {
                    tvSyncStatus.text = "Updated just now"
                    if (showToast) {
                        Toast.makeText(
                            this,
                            "Synced quotes & updated Lock Screen widgets",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    tvSyncStatus.text = "Live quotes"
                }
            }
        }
    }

    private fun showStockDetailBottomSheet(quote: StockQuote) {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(24))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    dp(28).toFloat(), dp(28).toFloat(),
                    dp(28).toFloat(), dp(28).toFloat(),
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#FF191A1D"))
            }
        }

        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(12)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(2).toFloat()
                setColor(Color.parseColor("#FF50535A"))
            }
        }
        root.addView(handle)

        // 1. Header Row: Symbol (GOOG) + Company Name (Alphabet Inc.) + Close Button
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleLine = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleLine.addView(TextView(this).apply {
            text = quote.symbol
            setTextColor(Color.WHITE)
            textSize = 29f
            setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
        })
        titleLine.addView(TextView(this).apply {
            text = quote.name
            setTextColor(Color.parseColor("#FF8E8E93"))
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(10), 0, 0, dp(2))
            maxLines = 1
        })

        val closeBtn = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(30))
            gravity = Gravity.CENTER
            text = "✕"
            setTextColor(Color.parseColor("#FFAEAEB2"))
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF2C2E33"))
            }
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                dialog.dismiss()
            }
        }

        headerRow.addView(titleLine)
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        // Hairline Divider under Symbol Header
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(10)
                bottomMargin = dp(10)
            }
            setBackgroundColor(Color.parseColor("#FF2C2E33"))
        })

        // 2. Price Row: [At Close (Price + MarketCap/Change)] | [After Hours (Price + %)]
        val quotesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Left Column: At Close
        val atCloseCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val atCloseTopLine = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
        }
        val tvAtClosePrice = TextView(this).apply {
            text = quote.formattedPrice()
            setTextColor(Color.WHITE)
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
        }
        atCloseTopLine.addView(tvAtClosePrice)

        val defaultBadgeText = if (quote.marketCapLabel != "--" && quote.marketCapLabel.isNotBlank()) {
            quote.marketCapLabel
        } else {
            quote.formattedPercentChange()
        }
        val tvPrimaryDelta = TextView(this).apply {
            text = defaultBadgeText
            setTextColor(
                if (quote.isPositive) Color.parseColor("#FF30D158") else Color.parseColor("#FFFF453A")
            )
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), 0, 0, 0)
        }
        atCloseTopLine.addView(tvPrimaryDelta)

        val defaultAtCloseSubtitle = if (quote.isForex) "Live Rate" else "At Close"
        val tvAtCloseLabel = TextView(this).apply {
            text = defaultAtCloseSubtitle
            setTextColor(Color.parseColor("#FF8E8E93"))
            textSize = 13.5f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(1), 0, 0)
        }
        atCloseCol.addView(atCloseTopLine)
        atCloseCol.addView(tvAtCloseLabel)
        quotesRow.addView(atCloseCol)

        // Vertical Divider between At Close and After Hours
        quotesRow.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(1), dp(36)).apply {
                marginStart = dp(16)
                marginEnd = dp(16)
            }
            setBackgroundColor(Color.parseColor("#FF2C2E33"))
        })

        // Right Column: After Hours
        val afterHoursCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val afterHoursTopLine = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
        }
        afterHoursTopLine.addView(TextView(this).apply {
            text = quote.formattedAfterHoursPrice()
            setTextColor(Color.WHITE)
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
        })
        afterHoursTopLine.addView(TextView(this).apply {
            text = quote.formattedAfterHoursPercent()
            setTextColor(
                if (quote.afterHoursChangePercent >= 0.0) {
                    Color.parseColor("#FF30D158")
                } else {
                    Color.parseColor("#FFFF453A")
                }
            )
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), 0, 0, 0)
        })
        afterHoursCol.addView(afterHoursTopLine)
        afterHoursCol.addView(TextView(this).apply {
            text = "After Hours"
            setTextColor(Color.parseColor("#FF8E8E93"))
            textSize = 13.5f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(1), 0, 0)
        })
        quotesRow.addView(afterHoursCol)
        root.addView(quotesRow)

        // 3. Exchange & Currency Label (e.g., NASDAQ · USD)
        root.addView(TextView(this).apply {
            text = quote.resolvedExchangeCurrencyLabel()
            setTextColor(Color.parseColor("#FF8E8E93"))
            textSize = 12.5f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(10))
        })

        // 4. Timeframe Selector Bar: 1D, 1W, 1M, 3M, 6M, YTD, 1Y, 2Y
        val rangeBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(36)
            ).apply {
                bottomMargin = dp(8)
            }
        }

        val detailChartView = StockDetailChartView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(248)
            ).apply {
                bottomMargin = dp(12)
            }
            setOnScrubChangedListener { scrubPrice, baseline, timeLabel ->
                if (scrubPrice != null) {
                    tvAtClosePrice.text = quote.formatValue(scrubPrice.toDouble())
                    val pct = if (baseline > 0f) ((scrubPrice - baseline) / baseline) * 100f else 0f
                    val sign = if (pct >= 0f) "+" else ""
                    tvPrimaryDelta.text = String.format(Locale.US, "%s%.2f%%", sign, pct)
                    tvPrimaryDelta.setTextColor(
                        if (pct >= 0f) Color.parseColor("#FF30D158") else Color.parseColor("#FFFF453A")
                    )
                    tvAtCloseLabel.text = timeLabel ?: defaultAtCloseSubtitle
                } else {
                    tvAtClosePrice.text = quote.formattedPrice()
                    tvPrimaryDelta.text = defaultBadgeText
                    tvPrimaryDelta.setTextColor(
                        if (quote.isPositive) Color.parseColor("#FF30D158") else Color.parseColor("#FFFF453A")
                    )
                    tvAtCloseLabel.text = defaultAtCloseSubtitle
                }
            }
        }

        val ranges = ChartRange.values()
        val rangeButtons = mutableListOf<TextView>()
        var activeRange: ChartRange = ChartRange.ONE_DAY
        var activeRequestGen = 0

        fun selectRange(range: ChartRange, isInitialOpen: Boolean = false) {
            activeRange = range
            val requestGen = ++activeRequestGen

            for (i in ranges.indices) {
                val btn = rangeButtons[i]
                val isSelected = (ranges[i] == range)
                if (isSelected) {
                    btn.background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(17).toFloat()
                        setColor(Color.parseColor("#FF32353B"))
                    }
                    btn.setTextColor(Color.WHITE)
                } else {
                    btn.background = null
                    btn.setTextColor(Color.parseColor("#FFE5E5EA"))
                }
            }

            // If this timeframe is already cached, render it immediately in a single step (no flash!)
            val cachedSeries = YahooFinanceClient.getCachedChartSeries(quote, range)
            if (cachedSeries != null) {
                val posOverride = if (range == ChartRange.ONE_DAY) quote.isPositive else null
                detailChartView.setChartSeries(cachedSeries, quote.isForex, posOverride)
                return
            }

            // Only show the cached 1D sparkline on initial sheet open while 1D loads
            if (isInitialOpen && range == ChartRange.ONE_DAY) {
                val initial1D = YahooFinanceClient.buildFallbackChartSeries(quote, ChartRange.ONE_DAY)
                detailChartView.setChartSeries(initial1D, quote.isForex, quote.isPositive)
            }

            // Fetch real Yahoo Finance chart series for the selected timeframe in background
            chartExecutor.execute {
                val liveSeries = YahooFinanceClient.fetchChartSeries(quote, range)
                runOnUiThread {
                    if (dialog.isShowing && activeRange == range && requestGen == activeRequestGen) {
                        val posOverride = if (range == ChartRange.ONE_DAY) quote.isPositive else null
                        detailChartView.setChartSeries(liveSeries, quote.isForex, posOverride)
                    }
                }
            }
        }

        for (r in ranges) {
            val btn = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(32), 1f)
                gravity = Gravity.CENTER
                text = r.label
                textSize = 12.5f
                setTypeface(typeface, Typeface.BOLD)
                setOnClickListener { v ->
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    if (activeRange != r) {
                        selectRange(r, isInitialOpen = false)
                    }
                }
            }
            rangeButtons.add(btn)
            rangeBar.addView(btn)
        }

        root.addView(rangeBar)
        root.addView(detailChartView)

        // Initialize with 1D selected and pre-warm other timeframes in parallel
        selectRange(ChartRange.ONE_DAY, isInitialOpen = true)
        for (r in ranges) {
            if (r != ChartRange.ONE_DAY) {
                chartExecutor.execute {
                    YahooFinanceClient.fetchChartSeries(quote, r)
                }
            }
        }

        // 5. Three-Column Key Statistics Grid (Open/High/Low | Vol./P/E/Mkt Cap | 52W H/52W L/Avg Vol)
        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(2)
            }
        }

        fun createStatsColumn(items: List<Pair<String, String>>): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                for (pair in items) {
                    val line = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, dp(3), 0, dp(3))
                    }
                    line.addView(TextView(this@MainActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        text = pair.first
                        setTextColor(Color.parseColor("#FF8E8E93"))
                        textSize = 12.5f
                        setTypeface(typeface, Typeface.BOLD)
                        maxLines = 1
                    })
                    line.addView(TextView(this@MainActivity).apply {
                        text = pair.second
                        setTextColor(Color.WHITE)
                        textSize = 12.5f
                        setTypeface(typeface, Typeface.BOLD)
                        maxLines = 1
                    })
                    addView(line)
                }
            }
        }

        fun createStatsDivider(): View {
            return View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(1), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                    marginStart = dp(10)
                    marginEnd = dp(10)
                    topMargin = dp(3)
                    bottomMargin = dp(3)
                }
                setBackgroundColor(Color.parseColor("#FF2C2E33"))
            }
        }

        val col1 = createStatsColumn(
            listOf(
                "Open" to quote.formatValue(quote.openPrice),
                "High" to quote.formatValue(quote.dayHigh),
                "Low" to quote.formatValue(quote.dayLow)
            )
        )
        val col2 = createStatsColumn(
            if (quote.isForex) {
                listOf(
                    "Prev" to quote.formatValue(quote.previousClose),
                    "Chg" to quote.formattedChange(),
                    "Chg %" to quote.formattedPercentChange()
                )
            } else {
                listOf(
                    "Vol." to quote.volumeLabel,
                    "P/E" to quote.peRatioLabel,
                    "Mkt Cap" to if (quote.marketCapLabel != "--") quote.marketCapLabel else quote.formattedPercentChange()
                )
            }
        )
        val col3 = createStatsColumn(
            listOf(
                "52W H" to quote.formatValue(quote.fiftyTwoWeekHigh),
                "52W L" to quote.formatValue(quote.fiftyTwoWeekLow),
                "Avg Vol" to quote.avgVolumeLabel
            )
        )

        statsRow.addView(col1)
        statsRow.addView(createStatsDivider())
        statsRow.addView(col2)
        statsRow.addView(createStatsDivider())
        statsRow.addView(col3)
        root.addView(statsRow)

        // 6. Action Buttons (Add to Watchlist if not added, and Use on Lock Screen Widgets)
        val watchlistSymbols = StockPreferences.getWatchlistSymbols(this)
        val isInWatchlist = watchlistSymbols.any { it.equals(quote.symbol, ignoreCase = true) }

        if (!isInWatchlist) {
            val btnAddWatchlist = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(44)
                ).apply {
                    topMargin = dp(16)
                }
                gravity = Gravity.CENTER
                text = "+ Add ${quote.symbol} to Watchlist"
                setTextColor(colorActiveMint)
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setBackgroundResource(R.drawable.bg_tab_selected_pill)
                setOnClickListener { v ->
                    v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    StockPreferences.addSymbol(this@MainActivity, quote.symbol)
                    renderAllFromCache()
                    triggerLiveYahooRefresh(showToast = false)
                    dialog.dismiss()
                }
            }
            root.addView(btnAddWatchlist)
        }

        val btnSetFeatured = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply {
                topMargin = if (isInWatchlist) dp(16) else dp(8)
            }
            gravity = Gravity.CENTER
            text = "Use ${quote.symbol} on Lock & Home Screen Widgets"
            setTextColor(colorActiveMint)
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_tab_selected_pill)
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                StockPreferences.addSymbol(this@MainActivity, quote.symbol)
                StockPreferences.setFeaturedSymbol(this@MainActivity, quote.symbol)
                WidgetUpdateHelper.renderAllWidgetsFromCache(this@MainActivity)
                renderAllFromCache()
                Toast.makeText(
                    this@MainActivity,
                    "Lock & Home Screen widgets set to ${quote.symbol}",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
        }
        root.addView(btnSetFeatured)

        dialog.setContentView(root)
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (sheet != null) {
                sheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }
        dialog.show()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
