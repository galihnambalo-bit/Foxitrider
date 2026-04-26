package com.foxitrider.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

enum class DrawingTool { PEN, HIGHLIGHTER, ERASER }

data class DrawPath(val path: Path, val paint: Paint)

class PdfDrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paths = mutableListOf<DrawPath>()
    private val undonePaths = mutableListOf<DrawPath>()
    private var currentPath = Path()
    private var currentTool = DrawingTool.PEN
    private var currentColor = Color.RED
    private var currentStrokeWidth = 8f
    private var visible = true
    private var lastX = 0f
    private var lastY = 0f

    private fun makePaint(): Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        when (currentTool) {
            DrawingTool.PEN -> {
                color = currentColor
                strokeWidth = currentStrokeWidth
                alpha = 255
            }
            DrawingTool.HIGHLIGHTER -> {
                color = currentColor
                strokeWidth = currentStrokeWidth * 5
                alpha = 80
            }
            DrawingTool.ERASER -> {
                color = Color.TRANSPARENT
                strokeWidth = currentStrokeWidth * 6
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!visible) return
        for (dp in paths) canvas.drawPath(dp.path, dp.paint)
        canvas.drawPath(currentPath, makePaint())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x; val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                undonePaths.clear()
                currentPath = Path()
                currentPath.moveTo(x, y)
                lastX = x; lastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
                lastX = x; lastY = y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(x, y)
                paths.add(DrawPath(Path(currentPath), makePaint()))
                currentPath.reset()
                invalidate()
            }
        }
        return true
    }

    fun setTool(tool: DrawingTool) { currentTool = tool }
    fun setColor(color: Int) { currentColor = color }
    fun setStrokeWidth(w: Float) { currentStrokeWidth = w }
    fun undo() { if (paths.isNotEmpty()) { undonePaths.add(paths.removeLast()); invalidate() } }
    fun redo() { if (undonePaths.isNotEmpty()) { paths.add(undonePaths.removeLast()); invalidate() } }
    fun clear() { paths.clear(); undonePaths.clear(); currentPath.reset(); invalidate() }
    fun isEmpty() = paths.isEmpty()
    fun isDrawingVisible() = visible
    fun toggleVisibility() { visible = !visible; invalidate() }

    fun getBitmap(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp))
        return bmp
    }
}
