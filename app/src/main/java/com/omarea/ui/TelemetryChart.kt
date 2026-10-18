package com.omarea.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.util.AttributeSet
import kotlin.math.max
import kotlin.math.min

class TelemetryChart @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var series: List<List<Pair<Long, Double>>> = emptyList()
        set(value) { field = value; invalidate() }
    var unit = "W"
    var emptyText = "No samples for this metric"
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val left = 58f * density
        val bottom = height - 28f * density
        val top = 32f * density
        val right = width - 14f * density
        val points = series.flatten().filter { it.second.isFinite() }
        paint.textSize = 11f * density
        paint.color = Color.GRAY
        if (points.isEmpty()) { canvas.drawText(emptyText, left, height / 2f, paint); return }
        val low = min(0.0, points.minOf { it.second })
        val high = max(low + 1, points.maxOf { it.second })
        val end = max(1000L, points.maxOf { it.first })
        for (i in 0..4) {
            val y = top + (bottom - top) * i / 4
            paint.color = 0x33888888
            paint.strokeWidth = 0.5f * density
            canvas.drawLine(left, y, right, y, paint)
            paint.color = Color.GRAY
            canvas.drawText("%.1f".format(high - (high - low) * i / 4), 2f * density, y, paint)
        }
        canvas.drawText(unit, 2f * density, 12f * density, paint)
        canvas.drawText("0", left, height - 6f * density, paint)
        canvas.drawText("%.1f min".format(end / 60000.0), max(left, right - 65f * density), height - 6f * density, paint)
        fun y(value: Double) = (bottom - (value - low) / (high - low) * (bottom - top)).toFloat()
        for ((index, values) in series.withIndex()) {
            paint.color = if (index == 0) Color.rgb(225, 125, 20) else Color.rgb(30, 145, 225)
            paint.strokeWidth = 1.5f * density
            var lastX = -1f
            var lastY = 0f
            var lastTime = -1L
            // Keep each pixel's extrema so short frame-time spikes remain visible.
            var bucketX = -1f
            var bucketLow = 0.0
            var bucketHigh = 0.0
            fun flush() {
                if (bucketX >= 0) canvas.drawLine(bucketX, y(bucketLow), bucketX, y(bucketHigh), paint)
            }
            for ((time, value) in values) {
                if (!value.isFinite()) { flush(); bucketX = -1f; lastX = -1f; continue }
                val x = (left + time.toDouble() / end * (right - left)).toInt().toFloat()
                val currentY = y(value)
                if (lastX < 0) canvas.drawCircle(x, currentY, 2f * density, paint)
                if (lastX >= 0 && time - lastTime <= 10000) canvas.drawLine(lastX, lastY, x, currentY, paint)
                if (x != bucketX) { flush(); bucketX = x; bucketLow = value; bucketHigh = value }
                else { bucketLow = min(bucketLow, value); bucketHigh = max(bucketHigh, value) }
                lastX = x; lastY = currentY; lastTime = time
            }
            flush()
        }
    }
}
