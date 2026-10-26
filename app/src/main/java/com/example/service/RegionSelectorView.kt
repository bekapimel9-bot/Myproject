package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class RegionSelectorView(
    context: Context,
    private val metrics: DisplayMetrics,
    private val onConfirmRegion: (left: Float, top: Float, right: Float, bottom: Float) -> Unit
) : FrameLayout(context) {

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#AA000000")
        isAntiAlias = true
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 3 * metrics.density
        isAntiAlias = true
    }

    private val selectedRect = RectF(
        metrics.widthPixels * 0.05f,
        metrics.heightPixels * 0.60f,
        metrics.widthPixels * 0.95f,
        metrics.heightPixels * 0.92f
    )

    private var activeDragMode = DragMode.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private enum class DragMode {
        NONE, MOVE, RESIZE_BOTTOM_RIGHT
    }

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)

        // Add instructions header
        val header = TextView(context).apply {
            text = "Seret kotak di atas area teks / dialog game Anda"
            setTextColor(Color.WHITE)
            textSize = 14f
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16 * metrics.density
                setColor(Color.parseColor("#E6141722"))
            }
            background = bg
            val p = (12 * metrics.density).toInt()
            setPadding(p * 2, p, p * 2, p)
        }
        val headerParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = (60 * metrics.density).toInt()
        }
        addView(header, headerParams)

        // Buttons Bar
        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btnCancel = Button(context).apply {
            text = "Batal"
            setTextColor(Color.WHITE)
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * metrics.density
                setColor(Color.parseColor("#334155"))
            }
            background = bg
            setOnClickListener {
                (parent as? android.view.ViewGroup)?.removeView(this@RegionSelectorView)
            }
        }

        val btnConfirm = Button(context).apply {
            text = "Simpan Area"
            setTextColor(Color.parseColor("#0C0E14"))
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * metrics.density
                setColor(Color.parseColor("#00E5FF"))
            }
            background = bg
            setOnClickListener {
                val left = selectedRect.left / metrics.widthPixels
                val top = selectedRect.top / metrics.heightPixels
                val right = selectedRect.right / metrics.widthPixels
                val bottom = selectedRect.bottom / metrics.heightPixels
                onConfirmRegion(left, top, right, bottom)
            }
        }

        val p = (8 * metrics.density).toInt()
        val btnParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(p, 0, p, 0)
        }
        buttonRow.addView(btnCancel, btnParams)
        buttonRow.addView(btnConfirm, btnParams)

        val rowParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (40 * metrics.density).toInt()
        }
        addView(buttonRow, rowParams)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw dark dimmed full screen
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        // Punch transparent hole for selected region
        canvas.drawRoundRect(selectedRect, 12 * metrics.density, 12 * metrics.density, clearPaint)
        // Draw neon border around region
        canvas.drawRoundRect(selectedRect, 12 * metrics.density, 12 * metrics.density, borderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y

                val handleSize = 60 * metrics.density
                val isNearBottomRight = (event.x >= selectedRect.right - handleSize &&
                        event.x <= selectedRect.right + handleSize &&
                        event.y >= selectedRect.bottom - handleSize &&
                        event.y <= selectedRect.bottom + handleSize)

                activeDragMode = if (isNearBottomRight) {
                    DragMode.RESIZE_BOTTOM_RIGHT
                } else if (selectedRect.contains(event.x, event.y)) {
                    DragMode.MOVE
                } else {
                    DragMode.NONE
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                lastTouchX = event.x
                lastTouchY = event.y

                when (activeDragMode) {
                    DragMode.MOVE -> {
                        selectedRect.offset(dx, dy)
                        // Constrain within screen
                        if (selectedRect.left < 0) selectedRect.offset(-selectedRect.left, 0f)
                        if (selectedRect.top < 0) selectedRect.offset(0f, -selectedRect.top)
                        if (selectedRect.right > width) selectedRect.offset(width - selectedRect.right, 0f)
                        if (selectedRect.bottom > height) selectedRect.offset(0f, height - selectedRect.bottom)
                        invalidate()
                    }
                    DragMode.RESIZE_BOTTOM_RIGHT -> {
                        selectedRect.right = (selectedRect.right + dx).coerceIn(selectedRect.left + 150f, width.toFloat())
                        selectedRect.bottom = (selectedRect.bottom + dy).coerceIn(selectedRect.top + 80f, height.toFloat())
                        invalidate()
                    }
                    DragMode.NONE -> {}
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                activeDragMode = DragMode.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
