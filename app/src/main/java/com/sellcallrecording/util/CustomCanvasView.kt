package com.sellcallrecording.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class CustomView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var tcnt: String = "0"
    private var isSelected: Boolean = false

    private val circlePaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }

    private val circlePaint1 = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val textPaint = Paint().apply {
        color = Color.CYAN
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    fun setText(tcnt: String) {
        this.tcnt = tcnt
        invalidate()
    }

    fun setSelect(selected: Boolean) {
        isSelected = selected
        circlePaint.color = if (isSelected) Color.CYAN else Color.WHITE
        circlePaint1.color = if (isSelected) Color.CYAN else Color.WHITE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = width.coerceAtMost(height) / 2f - 10f

        canvas.drawCircle(centerX, centerY, radius, circlePaint)
        canvas.drawCircle(centerX, centerY, radius - 15f, circlePaint1)

        val density = context.resources.displayMetrics.density
        val baseTextSize = 16f * density

        textPaint.textSize = baseTextSize
        val maxTextWidth = (radius * 2) * 0.7f
        val maxTextHeight = (radius * 2) * 0.4f

        val padding = 8f * density

        while (textPaint.measureText(tcnt) > (maxTextWidth - padding) || textPaint.textSize > (maxTextHeight - padding)) {
            textPaint.textSize -= 1f
        }

        val textBounds = Rect()
        textPaint.getTextBounds(tcnt, 0, tcnt.length, textBounds)
        val textHeight = textBounds.height()
        val textY = centerY + textHeight / 2f

        canvas.drawText(tcnt, centerX, textY, textPaint)
    }




}
