package com.foxitrider.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

data class TextOverlay(
    var text: String,
    var x: Float,
    var y: Float,
    var fontSize: Float = 40f,
    var color: Int = Color.BLACK,
    var bgColor: Int = Color.WHITE,
    var id: Int = System.currentTimeMillis().toInt()
)

class PdfOverlayEditor @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    val overlays = mutableListOf<TextOverlay>()
    private var selectedOverlay: TextOverlay? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var onOverlayClick: ((TextOverlay) -> Unit)? = null

    private val textPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val bgPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val selectedPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLUE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
    }

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val tapped = findOverlayAt(e.x, e.y)
                if (tapped != null) {
                    selectedOverlay = tapped
                    onOverlayClick?.invoke(tapped)
                    invalidate()
                    return true
                }
                selectedOverlay = null
                invalidate()
                return false
            }
        })

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (overlay in overlays) {
            textPaint.textSize = overlay.fontSize
            textPaint.color = overlay.color
            bgPaint.color = overlay.bgColor

            val bounds = Rect()
            textPaint.getTextBounds(overlay.text, 0, overlay.text.length, bounds)
            val padding = 8f
            val bgRect = RectF(
                overlay.x - padding,
                overlay.y - bounds.height() - padding,
                overlay.x + bounds.width() + padding,
                overlay.y + padding
            )
            canvas.drawRect(bgRect, bgPaint)
            canvas.drawText(overlay.text, overlay.x, overlay.y, textPaint)

            if (overlay == selectedOverlay) {
                canvas.drawRect(bgRect, selectedPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val overlay = findOverlayAt(event.x, event.y)
                if (overlay != null) {
                    selectedOverlay = overlay
                    dragOffsetX = event.x - overlay.x
                    dragOffsetY = event.y - overlay.y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                selectedOverlay?.let {
                    it.x = event.x - dragOffsetX
                    it.y = event.y - dragOffsetY
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                // Keep selected
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findOverlayAt(x: Float, y: Float): TextOverlay? {
        textPaint.textSize = 40f
        for (overlay in overlays.reversed()) {
            textPaint.textSize = overlay.fontSize
            val bounds = Rect()
            textPaint.getTextBounds(overlay.text, 0, overlay.text.length, bounds)
            val padding = 16f
            val rect = RectF(
                overlay.x - padding,
                overlay.y - bounds.height() - padding,
                overlay.x + bounds.width() + padding,
                overlay.y + padding
            )
            if (rect.contains(x, y)) return overlay
        }
        return null
    }

    fun addTextOverlay(text: String, x: Float = width / 2f, y: Float = height / 2f, fontSize: Float = 40f) {
        overlays.add(TextOverlay(text, x, y, fontSize))
        invalidate()
    }

    fun deleteSelected() {
        selectedOverlay?.let {
            overlays.remove(it)
            selectedOverlay = null
            invalidate()
        }
    }

    fun clearAll() {
        overlays.clear()
        selectedOverlay = null
        invalidate()
    }

    fun setOnOverlayClickListener(listener: (TextOverlay) -> Unit) {
        onOverlayClick = listener
    }

    fun getSelectedOverlay() = selectedOverlay

    fun hasOverlays() = overlays.isNotEmpty()

    // Render overlays to bitmap at given scale
    fun renderToBitmap(pageWidth: Int, pageHeight: Int, viewWidth: Int, viewHeight: Int): List<Pair<TextOverlay, PointF>> {
        val scaleX = pageWidth.toFloat() / viewWidth
        val scaleY = pageHeight.toFloat() / viewHeight
        return overlays.map { overlay ->
            val scaledPoint = PointF(overlay.x * scaleX, overlay.y * scaleY)
            Pair(overlay, scaledPoint)
        }
    }
}
