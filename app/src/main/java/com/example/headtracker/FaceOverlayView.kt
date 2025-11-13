package com.example.headtracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var faceX = -1f
    private var faceY = -1f
    private var imageWidth = 0
    private var imageHeight = 0
    private var rotation = 0

    private val dotPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val dotRadius = 30f

    /**
     * Updates the face position with proper coordinate transformation
     *
     * @param x Face center X in image coordinates
     * @param y Face center Y in image coordinates
     * @param imgWidth Width of the camera image
     * @param imgHeight Height of the camera image
     * @param rot Rotation degrees of the image
     */
    fun setFacePosition(x: Float, y: Float, imgWidth: Int, imgHeight: Int, rot: Int) {
        faceX = x
        faceY = y
        imageWidth = imgWidth
        imageHeight = imgHeight
        rotation = rot
        invalidate() // Trigger redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Don't draw if no face detected
        if (faceX < 0 || faceY < 0 || imageWidth == 0 || imageHeight == 0) {
            return
        }

        // Transform coordinates from camera space to view space
        val screenCoords = transformCoordinates(faceX, faceY, imageWidth, imageHeight, rotation)

        // Draw the red dot
        canvas.drawCircle(screenCoords.first, screenCoords.second, dotRadius, dotPaint)
    }

    /**
     * Transforms camera image coordinates to screen coordinates
     * Handles rotation and aspect ratio differences
     */
    private fun transformCoordinates(
        x: Float,
        y: Float,
        imgWidth: Int,
        imgHeight: Int,
        rotation: Int
    ): Pair<Float, Float> {

        // Calculate scale factors
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // Normalize coordinates (0-1 range)
        var normalizedX = x / imgWidth
        var normalizedY = y / imgHeight

        // Handle rotation (back camera is typically rotated 90 degrees)
        when (rotation) {
            90, 270 -> {
                // For portrait mode with back camera
                // The image is rotated, so we need to swap and adjust coordinates
                val temp = normalizedX
                normalizedX = normalizedY
                normalizedY = 1f - temp

                // Account for aspect ratio difference
                val imageAspect = imgHeight.toFloat() / imgWidth.toFloat()
                val viewAspect = viewWidth / viewHeight

                if (imageAspect > viewAspect) {
                    // Image is wider - scale X
                    val scale = viewAspect / imageAspect
                    normalizedX = (normalizedX - 0.5f) * scale + 0.5f
                } else {
                    // Image is taller - scale Y
                    val scale = imageAspect / viewAspect
                    normalizedY = (normalizedY - 0.5f) * scale + 0.5f
                }
            }
            180 -> {
                normalizedX = 1f - normalizedX
                normalizedY = 1f - normalizedY
            }
            else -> {
                // 0 degrees or unknown - use as is
            }
        }

        // Convert to screen coordinates
        val screenX = normalizedX * viewWidth
        val screenY = normalizedY * viewHeight

        return Pair(screenX, screenY)
    }
}