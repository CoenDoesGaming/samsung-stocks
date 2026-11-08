package com.oneui.stocks

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min

object SparklineRenderer {

    fun createLockWidgetGraphBitmap(
        points: List<Float>,
        previousClose: Float,
        widthPx: Int = 340,
        heightPx: Int = 92
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawSparkline(
            canvas = canvas,
            width = widthPx.toFloat(),
            height = heightPx.toFloat(),
            points = points,
            previousClose = previousClose,
            isPositive = (points.lastOrNull() ?: previousClose) >= previousClose,
            monochromeLockStyle = true
        )
        return bitmap
    }

    fun createColorWidgetGraphBitmap(
        points: List<Float>,
        previousClose: Float,
        isPositive: Boolean,
        widthPx: Int = 340,
        heightPx: Int = 96
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawSparkline(
            canvas = canvas,
            width = widthPx.toFloat(),
            height = heightPx.toFloat(),
            points = points,
            previousClose = previousClose,
            isPositive = isPositive,
            monochromeLockStyle = false
        )
        return bitmap
    }

    fun drawSparkline(
        canvas: Canvas,
        width: Float,
        height: Float,
        points: List<Float>,
        previousClose: Float,
        isPositive: Boolean,
        monochromeLockStyle: Boolean
    ) {
        if (width <= 4f || height <= 4f) return
        val safePoints = if (points.size >= 2) points else listOf(previousClose, previousClose)

        var minVal = safePoints.minOrNull() ?: previousClose
        var maxVal = safePoints.maxOrNull() ?: previousClose
        if (previousClose > 0f) {
            minVal = min(minVal, previousClose)
            maxVal = max(maxVal, previousClose)
        }
        val span = max(0.0001f, maxVal - minVal)

        val padX = 3f
        val padTop = 5f
        val padBottom = 4f
        val usableW = max(1f, width - padX * 2f)
        val usableH = max(1f, height - padTop - padBottom)

        fun yForValue(v: Float): Float {
            val norm = ((v - minVal) / span).coerceIn(0f, 1f)
            return padTop + usableH * (1f - norm)
        }

        val lineColor: Int
        val dashColor: Int
        val fillTopColor: Int
        val fillBottomColor: Int

        if (monochromeLockStyle) {
            lineColor = 0xCCFFFFFF.toInt()
            dashColor = 0x66FFFFFF
            fillTopColor = 0x42FFFFFF
            fillBottomColor = 0x05FFFFFF
        } else if (isPositive) {
            lineColor = 0xFF30D158.toInt()
            dashColor = 0x9930D158.toInt()
            fillTopColor = 0x5530D158
            fillBottomColor = 0x0030D158
        } else {
            lineColor = 0xFFFF3B30.toInt()
            dashColor = 0x99FF3B30.toInt()
            fillTopColor = 0x55FF3B30
            fillBottomColor = 0x00FF3B30
        }

        // 1. Horizontal Dashed Baseline at Previous Close
        val baseLineY = yForValue(previousClose).coerceIn(padTop, height - padBottom)
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = if (monochromeLockStyle) 2.2f else 2f
            color = dashColor
            pathEffect = DashPathEffect(floatArrayOf(6f, 5f), 0f)
        }
        val dashPath = Path().apply {
            moveTo(padX, baseLineY)
            lineTo(width - padX, baseLineY)
        }
        canvas.drawPath(dashPath, dashPaint)

        // 2. Build Line & Fill Paths
        val linePath = Path()
        val fillPath = Path()
        val stepX = usableW / (safePoints.size - 1).toFloat()

        for (i in safePoints.indices) {
            val x = padX + i * stepX
            val y = yForValue(safePoints[i])
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, height - 1f)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(padX + usableW, height - 1f)
        fillPath.close()

        // 3. Draw Gradient Fill Beneath Curve
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f,
                padTop,
                0f,
                height,
                fillTopColor,
                fillBottomColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(fillPath, fillPaint)

        // 4. Draw Crisp Sparkline Curve
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = if (monochromeLockStyle) 3.2f else 3.0f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = lineColor
        }
        canvas.drawPath(linePath, linePaint)
    }
}
