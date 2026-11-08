package com.oneui.stocks

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var points: List<Float> = listOf(1f, 1f)
    private var previousClose: Float = 1f
    private var isPositive: Boolean = false
    private var monochromeLockStyle: Boolean = false

    fun setSparklineData(
        points: List<Float>,
        previousClose: Float,
        isPositive: Boolean,
        monochromeLockStyle: Boolean = false
    ) {
        this.points = points
        this.previousClose = previousClose
        this.isPositive = isPositive
        this.monochromeLockStyle = monochromeLockStyle
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        SparklineRenderer.drawSparkline(
            canvas = canvas,
            width = width.toFloat(),
            height = height.toFloat(),
            points = points,
            previousClose = previousClose,
            isPositive = isPositive,
            monochromeLockStyle = monochromeLockStyle
        )
    }
}
