package com.foxitrider.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

data class TextOverlay(
    var text: String,
    var x: Float,
    var y: Float,
    var fontSize: Float = 40f,
    var color: Int = Color.BLACK,
    var bgColor: Int = Color.WHITE,
    var isHighlight: Boolean = false,
    var width: Float = 200f,
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
    private var lastTouchTime = 0L

    private val textPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val bgPaint = Paint().apply { style = Paint.Style.FILL }
    private val selectedPaint = Paint().apply {
        style = Paint.Style.STROKE; color = Color.BLUE
        strokeWidth = 2f; pathEffect = DashPathEffect(floatArrayOf(8f, 4f), 0f)
    }
    private val handlePaint = Paint().apply { color = Color.BLUE; style = Paint.Style.FILL }

    // Pinch to scale
    private var initialDistance = 0f
    private var initialFontSize = 40f
    private var isScaling = false

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(d: ScaleGestureDetector): Boolean {
                selectedOverlay?.let { initialFontSize = it.fontSize }
                isScaling = true
                return true
            }
            override fun onScale(d: ScaleGestureDetector): Boolean {
                selectedOverlay?.let {
                    it.fontSize = (initialFontSize * d.scaleFactor)
                        .coerceIn(12f, 120f)
                    invalidate()
                }
                return true
            }
            override fun onScaleEnd(d: ScaleGestureDetector) { isScaling = false }
        })

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (overlay in overlays) {
            if (overlay.isHighlight) {
                drawHighlight(canvas, overlay)
            } else {
                drawTextBox(canvas, overlay)
            }
        }
    }

    private fun drawHighlight(canvas: Canvas, overlay: TextOverlay) {
        bgPaint.color = overlay.bgColor
        bgPaint.alpha = 120
        val h = overlay.fontSize
        val rect = RectF(overlay.x, overlay.y, overlay.x + overlay.width, overlay.y + h)
        canvas.drawRoundRect(rect, 4f, 4f, bgPaint)
        bgPaint.alpha = 255

        if (overlay == selectedOverlay) {
            canvas.drawRoundRect(rect, 4f, 4f, selectedPaint)
            // resize handle kanan
            canvas.drawCircle(overlay.x + overlay.width, overlay.y + h/2, 10f, handlePaint)
        }
    }

    private fun drawTextBox(canvas: Canvas, overlay: TextOverlay) {
        textPaint.textSize = overlay.fontSize
        textPaint.color = overlay.color
        bgPaint.color = overlay.bgColor
        bgPaint.alpha = 230

        val bounds = Rect()
        textPaint.getTextBounds(overlay.text, 0, overlay.text.length, bounds)
        val pad = 10f
        val bgRect = RectF(
            overlay.x - pad,
            overlay.y - bounds.height() - pad,
            overlay.x + bounds.width() + pad,
            overlay.y + pad
        )
        canvas.drawRoundRect(bgRect, 6f, 6f, bgPaint)
        canvas.drawText(overlay.text, overlay.x, overlay.y, textPaint)

        if (overlay == selectedOverlay) {
            canvas.drawRoundRect(bgRect, 6f, 6f, selectedPaint)
            // scale handle pojok kanan bawah
            canvas.drawCircle(bgRect.right, bgRect.bottom, 12f, handlePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (isScaling) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val now = System.currentTimeMillis()
                val x = event.x; val y = event.y
                val overlay = findOverlayAt(x, y)
                if (overlay != null) {
                    // double tap = edit
                    if (now - lastTouchTime < 300 && overlay == selectedOverlay) {
                        onOverlayClick?.invoke(overlay)
                    }
                    selectedOverlay = overlay
                    dragOffsetX = x - overlay.x
                    dragOffsetY = y - overlay.y
                } else {
                    selectedOverlay = null
                }
                lastTouchTime = now
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1) {
                    selectedOverlay?.let {
                        it.x = event.x - dragOffsetX
                        it.y = event.y - dragOffsetY
                        // clamp ke dalam view
                        it.x = it.x.coerceIn(0f, width.toFloat() - 50f)
                        it.y = it.y.coerceIn(50f, height.toFloat())
                        invalidate()
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findOverlayAt(x: Float, y: Float): TextOverlay? {
        for (overlay in overlays.reversed()) {
            if (overlay.isHighlight) {
                val h = overlay.fontSize
                val rect = RectF(overlay.x - 20, overlay.y - 20,
                    overlay.x + overlay.width + 20, overlay.y + h + 20)
                if (rect.contains(x, y)) return overlay
            } else {
                val paint = Paint().apply { textSize = overlay.fontSize }
                val bounds = Rect()
                paint.getTextBounds(overlay.text, 0, overlay.text.length, bounds)
                val pad = 20f
                val rect = RectF(
                    overlay.x - pad, overlay.y - bounds.height() - pad,
                    overlay.x + bounds.width() + pad, overlay.y + pad
                )
                if (rect.contains(x, y)) return overlay
            }
        }
        return null
    }

    fun addTextOverlay(text: String, x: Float, y: Float, fontSize: Float = 40f, color: Int = Color.BLACK) {
        overlays.add(TextOverlay(text, x, y, fontSize, color, Color.WHITE))
        selectedOverlay = overlays.last()
        invalidate()
    }

    fun addHighlight(x: Float, y: Float, width: Float = 200f, color: Int = Color.parseColor("#FDD835")) {
        overlays.add(TextOverlay("", x, y, 24f, color, color, isHighlight = true, width = width))
        selectedOverlay = overlays.last()
        invalidate()
    }

    fun deleteSelected() {
        selectedOverlay?.let { overlays.remove(it); selectedOverlay = null; invalidate() }
    }

    fun clearAll() { overlays.clear(); selectedOverlay = null; invalidate() }
    fun hasOverlays() = overlays.isNotEmpty()
    fun getSelectedOverlay() = selectedOverlay
    fun setOnOverlayClickListener(l: (TextOverlay) -> Unit) { onOverlayClick = l }
}
