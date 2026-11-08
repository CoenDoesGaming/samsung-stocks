package com.oneui.stocks

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class StockDetailChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var prices: List<Float> = listOf(285.8f, 289.1f, 286.8f, 287.1f, 285.3f, 284.5f, 285.2f, 283.93f)
    private var volumes: List<Long> = listOf(35L, 12L, 18L, 14L, 10L, 22L, 15L, 11L)
    private var baselinePrice: Float = 283.5f
    private var xLabels: List<String> = listOf("2", "3", "4", "5", "6", "7")
    private var pointLabels: List<String> = emptyList()
    private var isPositive: Boolean = true
    private var isForex: Boolean = false
    private var scrubIndex: Int = -1

    private var onScrubListener: ((scrubPrice: Float?, baseline: Float, timeLabel: String?) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = Color.parseColor("#FF25272B")
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = Color.parseColor("#FF33363C")
    }

    private val dashBaselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.1f * density
        pathEffect = DashPathEffect(floatArrayOf(3.5f * density, 3.5f * density), 0f)
    }

    private val volDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = Color.parseColor("#FF3A3D44")
        pathEffect = DashPathEffect(floatArrayOf(2.5f * density, 3f * density), 0f)
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.1f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFEBEBF0")
        textSize = sp(12.5f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val volBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#FF767A84")
    }

    private val scrubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * density
        color = Color.parseColor("#E6FFFFFF")
    }

    private val scrubHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#44FFFFFF")
    }

    private val scrubDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val tooltipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#F22A2D34")
    }

    private val tooltipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = Color.parseColor("#55FFFFFF")
    }

    private val tooltipPricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(12.5f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val tooltipRect = RectF()

    fun setOnScrubChangedListener(listener: (scrubPrice: Float?, baseline: Float, timeLabel: String?) -> Unit) {
        this.onScrubListener = listener
    }

    fun setChartSeries(series: StockChartSeries, isForex: Boolean, forcePositive: Boolean? = null) {
        this.prices = if (series.prices.size >= 2) series.prices else listOf(1f, 1f)
        this.volumes = series.volumes
        this.baselinePrice = series.baselinePrice
        this.xLabels = series.xLabels
        this.pointLabels = series.pointLabels
        this.isForex = isForex
        this.isPositive = forcePositive ?: series.isPositive
        this.scrubIndex = -1
        this.onScrubListener?.invoke(null, baselinePrice, null)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rightAxisWidth = 48f * density
        val plotW = (width - rightAxisWidth).coerceAtLeast(1f)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val x = event.x.coerceIn(0f, plotW)
                val idx = ((x / plotW) * (prices.size - 1)).toInt().coerceIn(0, prices.lastIndex)
                if (idx != scrubIndex) {
                    if (scrubIndex != -1) {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    scrubIndex = idx
                    val p = prices[idx]
                    val tLabel = pointLabels.getOrNull(idx)?.takeIf { it.isNotBlank() }
                    onScrubListener?.invoke(p, baselinePrice, tLabel)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                scrubIndex = -1
                onScrubListener?.invoke(null, baselinePrice, null)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 10f || h <= 10f || prices.size < 2) return

        val rightAxisWidth = 48f * density
        val xLabelsHeight = 25f * density
        val volStripHeight = 24f * density

        val plotLeft = 0f
        val plotTop = 28f * density
        val plotRight = (w - rightAxisWidth).coerceAtLeast(40f)
        val plotBottom = (h - xLabelsHeight - volStripHeight).coerceAtLeast(plotTop + 40f)
        val plotW = plotRight - plotLeft
        val plotH = plotBottom - plotTop

        val accentColor = if (isPositive) {
            Color.parseColor("#FF30D158")
        } else {
            Color.parseColor("#FFFF453A")
        }
        val fillTopColor = if (isPositive) {
            Color.parseColor("#4D30D158")
        } else {
            Color.parseColor("#4DFF453A")
        }
        val fillBottomColor = if (isPositive) {
            Color.parseColor("#0630D158")
        } else {
            Color.parseColor("#06FF453A")
        }

        var minVal = prices.minOrNull() ?: 0f
        var maxVal = prices.maxOrNull() ?: 1f
        minVal = min(minVal, baselinePrice)
        maxVal = max(maxVal, baselinePrice)
        val rawSpan = (maxVal - minVal).takeIf { it > 0.00001f } ?: (maxVal * 0.02f).coerceAtLeast(0.01f)
        val pad = rawSpan * 0.10f
        val yMin = minVal - pad
        val yMax = maxVal + pad
        val ySpan = (yMax - yMin).coerceAtLeast(0.00001f)

        // 1. Vertical Grid Columns (6 columns across plot area + right axis divider)
        val numCols = 6
        for (c in 1..numCols) {
            val gx = plotLeft + (plotW * c / numCols.toFloat())
            canvas.drawLine(gx, plotTop, gx, plotBottom + xLabelsHeight, gridPaint)
        }
        canvas.drawLine(plotRight, plotTop, plotRight, plotBottom + xLabelsHeight, borderPaint)

        // 2. Horizontal Grid Lines & Right-Axis Price Labels
        val numRows = 4
        for (r in 0 until numRows) {
            val frac = r.toFloat() / numRows.toFloat()
            val gy = plotTop + frac * plotH
            canvas.drawLine(plotLeft, gy, w, gy, gridPaint)

            val levelPrice = yMax - (frac + 0.14f) * ySpan
            val labelText = formatAxisPrice(levelPrice, ySpan)
            val textY = gy + 15f * density
            canvas.drawText(labelText, plotRight + 6f * density, textY, axisTextPaint)
        }
        canvas.drawLine(plotLeft, plotBottom, w, plotBottom, borderPaint)

        // 3. Dotted Baseline (Previous Close / Start of Period)
        dashBaselinePaint.color = if (isPositive) {
            Color.parseColor("#88469960")
        } else {
            Color.parseColor("#88A8443E")
        }
        val baseY = (plotBottom - ((baselinePrice - yMin) / ySpan) * plotH)
            .coerceIn(plotTop + 2f * density, plotBottom - 2f * density)
        val dashPath = Path().apply {
            moveTo(plotLeft, baseY)
            lineTo(plotRight, baseY)
        }
        canvas.drawPath(dashPath, dashBaselinePaint)

        // 4. Price Curve & Smooth Gradient Fill
        val linePath = Path()
        val fillPath = Path()
        val stepX = plotW / (prices.size - 1).toFloat()

        for (i in prices.indices) {
            val px = plotLeft + i * stepX
            val py = (plotBottom - ((prices[i] - yMin) / ySpan) * plotH)
                .coerceIn(plotTop + 2f * density, plotBottom - 2f * density)
            if (i == 0) {
                linePath.moveTo(px, py)
                fillPath.moveTo(px, plotBottom)
                fillPath.lineTo(px, py)
            } else {
                linePath.lineTo(px, py)
                fillPath.lineTo(px, py)
            }
            if (i == prices.lastIndex) {
                fillPath.lineTo(px, plotBottom)
                fillPath.close()
            }
        }

        fillPaint.shader = LinearGradient(
            0f,
            plotTop,
            0f,
            plotBottom,
            fillTopColor,
            fillBottomColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)

        linePaint.color = accentColor
        canvas.drawPath(linePath, linePaint)

        // 5. X-Axis Labels Row
        val xLabelBaselineY = plotBottom + 17f * density
        val maxLabels = min(numCols, xLabels.size)
        for (i in 0 until maxLabels) {
            val lbl = xLabels[i]
            if (lbl.isNotEmpty()) {
                val colX = plotLeft + (plotW * (i + 1) / (numCols + 0.55f))
                canvas.drawText(lbl, colX, xLabelBaselineY, axisTextPaint)
            }
        }

        // 6. Mini Volume Bar Strip at Bottom
        val volBottomY = h - 3f * density
        val volTopLimitY = plotBottom + xLabelsHeight + 4f * density
        val maxVolH = (volBottomY - volTopLimitY).coerceAtLeast(6f * density)

        canvas.drawLine(
            plotLeft + 1f * density,
            volBottomY - 12f * density,
            plotLeft + 1f * density,
            volBottomY,
            volBarPaint
        )
        val volDashPath = Path().apply {
            moveTo(plotLeft, volBottomY)
            lineTo(plotRight, volBottomY)
        }
        canvas.drawPath(volDashPath, volDashPaint)

        if (volumes.isNotEmpty()) {
            val vMax = (volumes.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
            val vStep = plotW / max(1, volumes.size - 1).toFloat()
            for (i in volumes.indices) {
                val v = volumes[i].toFloat()
                if (v <= 0f) continue
                val barH = ((v / vMax) * maxVolH).coerceAtLeast(2f * density)
                val vx = plotLeft + i * vStep
                canvas.drawLine(vx, volBottomY, vx, volBottomY - barH, volBarPaint)
            }
        }

        // 7. Interactive Touch Scrubbing Crosshair + Floating Price & Time Pill
        if (scrubIndex in prices.indices) {
            val sx = plotLeft + scrubIndex * stepX
            val priceAtFinger = prices[scrubIndex]
            val sy = (plotBottom - ((priceAtFinger - yMin) / ySpan) * plotH)
                .coerceIn(plotTop + 2f * density, plotBottom - 2f * density)

            canvas.drawLine(sx, plotTop, sx, plotBottom, scrubPaint)
            canvas.drawCircle(sx, sy, 8.5f * density, scrubHaloPaint)
            scrubDotPaint.color = accentColor
            canvas.drawCircle(sx, sy, 4.5f * density, scrubDotPaint)

            val formattedScrubPrice = formatScrubPrice(priceAtFinger)
            val timeStr = pointLabels.getOrNull(scrubIndex)?.takeIf { it.isNotBlank() }
            val pillText = if (timeStr != null) {
                "$formattedScrubPrice  •  $timeStr"
            } else {
                formattedScrubPrice
            }

            val textW = tooltipPricePaint.measureText(pillText)
            val pillPadH = 10f * density
            val pillW = textW + pillPadH * 2f
            val pillH = 22f * density
            val pillLeft = (sx - pillW / 2f).coerceIn(2f * density, max(2f * density, plotRight - pillW - 2f * density))
            val pillTop = 2f * density

            tooltipRect.set(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH)
            canvas.drawRoundRect(tooltipRect, 11f * density, 11f * density, tooltipBgPaint)
            canvas.drawRoundRect(tooltipRect, 11f * density, 11f * density, tooltipBorderPaint)

            val fm = tooltipPricePaint.fontMetrics
            val textBaseline = tooltipRect.centerY() - (fm.ascent + fm.descent) / 2f
            canvas.drawText(pillText, pillLeft + pillPadH, textBaseline, tooltipPricePaint)
        }
    }

    private fun formatScrubPrice(value: Float): String {
        return when {
            isForex && value < 10f -> String.format(Locale.US, "%.4f", value)
            value < 10f -> String.format(Locale.US, "%.3f", value)
            else -> String.format(Locale.US, "%.2f", value)
        }
    }

    private fun formatAxisPrice(value: Float, span: Float): String {
        return when {
            isForex && value < 10f -> String.format(Locale.US, "%.3f", value)
            value < 10f -> String.format(Locale.US, "%.2f", value)
            span < 3.5f -> String.format(Locale.US, "%.1f", value)
            else -> String.format(Locale.US, "%.0f", value)
        }
    }
}
