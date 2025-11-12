package com.example.headtracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var normX: Float = -1f
    private var normY: Float = -1f

    private val paintDot = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Optional: Add a circle border for better visibility
    private val paintBorder = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    fun setNormalizedPosition(x: Float, y: Float) {
        normX = x
        normY = y
        invalidate() // Trigger redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Don't draw if no face detected
        if (normX < 0f || normY < 0f) return

        // Convert normalized coordinates to screen pixels
        val px = normX * width
        val py = normY * height

        // Dynamic radius based on screen size (minimum 15dp for visibility)
        val radius = max(15f, width * 0.025f)

        // Draw white border for better visibility
        canvas.drawCircle(px, py, radius + 2f, paintBorder)

        // Draw red dot
        canvas.drawCircle(px, py, radius, paintDot)
    }
}