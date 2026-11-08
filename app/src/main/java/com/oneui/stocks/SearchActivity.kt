package com.oneui.stocks

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class SearchActivity : AppCompatActivity() {

    private val bgExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val searchGeneration = AtomicInteger(0)
    private var pendingSearchRunnable: Runnable? = null

    private lateinit var searchRoot: LinearLayout
    private lateinit var tvSearchStatus: TextView
    private lateinit var sectionDefaultDiscovery: LinearLayout
    private lateinit var btnClearAllRecents: TextView
    private lateinit var tvNoRecentSearches: TextView
    private lateinit var recentSearchesCard: LinearLayout
    private lateinit var popularResultsCard: LinearLayout
    private lateinit var sectionLiveResults: LinearLayout
    private lateinit var tvResultsHeader: TextView
    private lateinit var searchResultsCard: LinearLayout
    private lateinit var etSearchInput: EditText

    private val colorActiveMint = Color.parseColor("#FF82DFB5")

    private val popularEtfsAndStocks = listOf(
        SearchSymbolResult("VOO", "Vanguard S&P 500 ETF", "ETF • NYSEArca"),
        SearchSymbolResult("SPY", "SPDR S&P 500 ETF Trust", "ETF • NYSEArca"),
        SearchSymbolResult("QQQ", "Invesco QQQ Trust (Nasdaq-100 ETF)", "ETF • NASDAQ"),
        SearchSymbolResult("IVV", "iShares Core S&P 500 ETF", "ETF • NYSEArca"),
        SearchSymbolResult("VAS.AX", "Vanguard Australian Shares Index ETF", "ETF • ASX"),
        SearchSymbolResult("NDQ.AX", "Betashares Nasdaq 100 ETF", "ETF • ASX"),
        SearchSymbolResult("VDHG.AX", "Vanguard Diversified High Growth ETF", "ETF • ASX"),
        SearchSymbolResult("NVDA", "NVIDIA Corporation", "Stock • NASDAQ"),
        SearchSymbolResult("MSFT", "Microsoft Corporation", "Stock • NASDAQ"),
        SearchSymbolResult("TSLA", "Tesla, Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("AMZN", "Amazon.com, Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("META", "Meta Platforms, Inc.", "Stock • NASDAQ"),
        SearchSymbolResult("CBA.AX", "Commonwealth Bank of Australia", "Stock • ASX"),
        SearchSymbolResult("BHP.AX", "BHP Group Limited", "Stock • ASX")
    )

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotEmpty()) {
                etSearchInput.setText(spoken)
                etSearchInput.setSelection(spoken.length)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchRoot = findViewById(R.id.search_root)
        tvSearchStatus = findViewById(R.id.tv_search_status)
        sectionDefaultDiscovery = findViewById(R.id.section_default_discovery)
        btnClearAllRecents = findViewById(R.id.btn_clear_all_recents)
        tvNoRecentSearches = findViewById(R.id.tv_no_recent_searches)
        recentSearchesCard = findViewById(R.id.recent_searches_card)
        popularResultsCard = findViewById(R.id.popular_results_card)
        sectionLiveResults = findViewById(R.id.section_live_results)
        tvResultsHeader = findViewById(R.id.tv_results_header)
        searchResultsCard = findViewById(R.id.search_results_card)
        etSearchInput = findViewById(R.id.et_search_input)

        setupWindowInsets()
        setupControls()
        renderDefaultDiscoveryView()

        etSearchInput.post {
            etSearchInput.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(etSearchInput, 0)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(searchRoot) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(systemBars.bottom, imeInsets.bottom)
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomInset)
            windowInsets
        }
    }

    private fun setupControls() {
        findViewById<ImageButton>(R.id.btn_search_back).setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            finish()
        }

        // "Clear all" on the right side of "Recent searches"
        btnClearAllRecents.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            StockPreferences.clearRecentSearches(this)
            renderDefaultDiscoveryView()
        }

        findViewById<ImageButton>(R.id.btn_search_mic).setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a stock ticker or ETF name")
                }
                voiceLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "Voice input not available on this device", Toast.LENGTH_SHORT).show()
            }
        }

        etSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                pendingSearchRunnable?.let { mainHandler.removeCallbacks(it) }
                if (query.isEmpty()) {
                    tvSearchStatus.text = ""
                    renderDefaultDiscoveryView()
                    return
                }

                // 1. Instant 0ms local catalog matches (Stocks, ETFs, FX, Indices, Crypto)
                val localMatches = YahooFinanceClient.searchLocalCatalog(query)
                if (localMatches.isNotEmpty()) {
                    renderLiveSearchResults("Results for \"$query\"", localMatches)
                }

                // 2. Debounced live Yahoo Finance API search (220ms)
                val runnable = Runnable {
                    executeLiveSearch(query)
                }
                pendingSearchRunnable = runnable
                mainHandler.postDelayed(runnable, 220L)
            }
        })

        etSearchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                val query = etSearchInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    pendingSearchRunnable?.let { mainHandler.removeCallbacks(it) }
                    executeLiveSearch(query)
                }
                true
            } else {
                false
            }
        }
    }

    private fun renderDefaultDiscoveryView() {
        sectionDefaultDiscovery.visibility = View.VISIBLE
        sectionLiveResults.visibility = View.GONE

        val watchlist = StockPreferences.getWatchlistSymbols(this)

        // 1. Recent Searches Section
        val recents = StockPreferences.getRecentSearches(this)
        if (recents.isEmpty()) {
            tvNoRecentSearches.visibility = View.VISIBLE
            recentSearchesCard.visibility = View.GONE
            btnClearAllRecents.visibility = View.GONE
        } else {
            tvNoRecentSearches.visibility = View.GONE
            recentSearchesCard.visibility = View.VISIBLE
            btnClearAllRecents.visibility = View.VISIBLE
            recentSearchesCard.removeAllViews()
            for (i in recents.indices) {
                val item = recents[i]
                val isAdded = watchlist.any { it.equals(item.symbol, ignoreCase = true) }
                recentSearchesCard.addView(
                    createResultRowView(
                        item = item,
                        isAdded = isAdded,
                        showDivider = i != recents.lastIndex
                    )
                )
            }
        }

        // 2. Popular ETFs & Stocks Section Underneath Recent Searches
        popularResultsCard.removeAllViews()
        for (i in popularEtfsAndStocks.indices) {
            val item = popularEtfsAndStocks[i]
            val isAdded = watchlist.any { it.equals(item.symbol, ignoreCase = true) }
            popularResultsCard.addView(
                createResultRowView(
                    item = item,
                    isAdded = isAdded,
                    showDivider = i != popularEtfsAndStocks.lastIndex
                )
            )
        }
    }

    private fun executeLiveSearch(query: String) {
        val gen = searchGeneration.incrementAndGet()
        tvSearchStatus.text = "Searching \"$query\"..."

        bgExecutor.execute {
            val results = YahooFinanceClient.searchSymbols(query)
            mainHandler.post {
                if (gen != searchGeneration.get()) return@post
                tvSearchStatus.text = ""
                if (results.isEmpty()) {
                    val fallback = listOf(
                        SearchSymbolResult(
                            symbol = query.uppercase(),
                            name = "Add \"${query.uppercase()}\" directly from Yahoo Finance",
                            exchange = "Ticker"
                        )
                    )
                    renderLiveSearchResults("Results for \"$query\"", fallback)
                } else {
                    renderLiveSearchResults("Results for \"$query\"", results)
                }
            }
        }
    }

    private fun renderLiveSearchResults(headerText: String, items: List<SearchSymbolResult>) {
        sectionDefaultDiscovery.visibility = View.GONE
        sectionLiveResults.visibility = View.VISIBLE
        tvResultsHeader.text = headerText

        searchResultsCard.removeAllViews()
        val watchlist = StockPreferences.getWatchlistSymbols(this)

        for (i in items.indices) {
            val item = items[i]
            val isAdded = watchlist.any { it.equals(item.symbol, ignoreCase = true) }
            searchResultsCard.addView(
                createResultRowView(
                    item = item,
                    isAdded = isAdded,
                    showDivider = i != items.lastIndex
                )
            )
        }
    }

    private fun createResultRowView(
        item: SearchSymbolResult,
        isAdded: Boolean,
        showDivider: Boolean
    ): View {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(11), 0, dp(11))
            isClickable = true
            isFocusable = true
        }

        val leftCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val symTv = TextView(this).apply {
            text = item.symbol
            setTextColor(Color.WHITE)
            textSize = 16.5f
            setTypeface(typeface, Typeface.BOLD)
        }
        val nameTv = TextView(this).apply {
            text = "${item.name} • ${item.exchange}"
            setTextColor(Color.parseColor("#FF96ABA2"))
            textSize = 13f
        }
        leftCol.addView(symTv)
        leftCol.addView(nameTv)

        val actionBtn = TextView(this).apply {
            text = if (isAdded) "Added" else "+ Add"
            setTextColor(if (isAdded) Color.WHITE else colorActiveMint)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(14), dp(7), dp(14), dp(7))
            setBackgroundResource(
                if (isAdded) R.drawable.bg_top_action_pill else R.drawable.bg_tab_selected_pill
            )
        }

        val onSelectSymbol = View.OnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            StockPreferences.addRecentSearch(this@SearchActivity, item)
            StockPreferences.addSymbol(this@SearchActivity, item.symbol)

            actionBtn.text = "Added"
            actionBtn.setTextColor(Color.WHITE)
            actionBtn.setBackgroundResource(R.drawable.bg_top_action_pill)

            if (etSearchInput.text.toString().trim().isEmpty()) {
                renderDefaultDiscoveryView()
            }

            bgExecutor.execute {
                runCatching {
                    val fetched = YahooFinanceClient.fetchSingleQuote(item.symbol)
                    StockPreferences.saveQuotes(this@SearchActivity, listOf(fetched))
                    WidgetUpdateHelper.renderAllWidgetsFromCache(this@SearchActivity)
                }
            }

            Toast.makeText(
                this@SearchActivity,
                "Added ${item.symbol} to Watchlist",
                Toast.LENGTH_SHORT
            ).show()
        }

        row.setOnClickListener(onSelectSymbol)
        actionBtn.setOnClickListener(onSelectSymbol)

        row.addView(leftCol)
        row.addView(actionBtn)
        wrapper.addView(row)

        if (showDivider) {
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                )
                setBackgroundColor(Color.parseColor("#FF222826"))
            }
            wrapper.addView(divider)
        }

        return wrapper
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
