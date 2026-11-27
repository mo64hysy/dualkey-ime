package io.github.user03715910.dualkey.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.abs

/**
 * A normal five-way flick key with an optional second layer:
 * hold for 300ms, then flick to enter the mapped symbol.
 */
class FlickKey(context: Context, private val values: List<String>, private val threshold: Float,
    private val feedback: (View) -> Unit, private val input: (String) -> Unit, private val previewEnabled: Boolean = true,
    label: String = values[0], description: String = "${values[0]}行",
    private val longPressValues: List<String>? = null) : AppCompatTextView(context) {
    private var downX = 0f
    private var downY = 0f
    private var direction = 0
    private var activePointer = -1
    private var symbolMode = false
    private var popup: PopupWindow? = null
    private var preview: FlickPreviewBubble? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = currentTextColor
        textAlign = Paint.Align.CENTER
        textSize = 9f * resources.displayMetrics.scaledDensity
        typeface = android.graphics.Typeface.DEFAULT
    }
    private val normalLongPress = Runnable {
        if (activePointer >= 0 && direction == 0 && performLongClick()) finishGesture()
    }
    private val symbolLongPress = Runnable {
        if (activePointer >= 0 && direction == 0) {
            symbolMode = true
            showPreview(longPressValues!!)
            updatePreview()
            invalidate()
        }
    }
    init {
        text = label
        textSize = 22f
        includeFontPadding = false
        setSingleLine(true)
        gravity = Gravity.CENTER
        isClickable = true
        contentDescription = (listOf(description) + listOf("左","上","右","下").zip(values.drop(1))
            .filter { it.second.isNotEmpty() }.map { (name,value) -> "$name$value" } +
            (longPressValues?.let { listOf("長押し記号") + listOf("中央","左","上","右","下").zip(it)
                .filter { pair -> pair.second.isNotEmpty() }.map { (name,value) -> "$name$value" } } ?: emptyList())
            ).joinToString("。")
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val symbols = longPressValues ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        val oldSize = labelPaint.textSize
        labelPaint.color = currentTextColor
        // Center-output symbol is placed at the upper-left; directional symbols surround the kana.
        val positions = listOf(
            w * .20f to h * .25f,
            w * .12f to h * .57f,
            w * .50f to h * .20f,
            w * .88f to h * .57f,
            w * .50f to h * .88f
        )
        symbols.forEachIndexed { index, value ->
            if (value.isNotEmpty()) canvas.drawText(value, positions[index].first, positions[index].second, labelPaint)
        }
        labelPaint.textSize = oldSize
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                popup?.dismiss(); popup=null; preview=null
                activePointer = event.getPointerId(0)
                downX = event.x; downY = event.y; direction = 0; symbolMode = false
                isPressed = true; feedback(this)
                parent.requestDisallowInterceptTouchEvent(true)
                if (longPressValues != null) postDelayed(symbolLongPress, 300L)
                else {
                    if (isLongClickable) postDelayed(normalLongPress, ViewConfiguration.getLongPressTimeout().toLong())
                    if (previewEnabled) showPreview(values)
                }
                updatePreview()
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(activePointer)
                if (index < 0) return true
                val x=event.getX(index); val y=event.getY(index)
                if (!symbolMode && maxOf(abs(x-downX),abs(y-downY))>touchSlop) {
                    removeCallbacks(symbolLongPress)
                    if (isLongClickable) removeCallbacks(normalLongPress)
                }
                direction = if (symbolMode) symbolDirectionAt(x,y) else directionAt(x,y)
                updatePreview(); invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (activePointer<0) return true
                val wasSymbolMode = symbolMode
                val releasedDirection = if (wasSymbolMode) symbolDirectionAt(event.x,event.y) else directionAt(event.x,event.y)
                val output = if (wasSymbolMode) longPressValues?.getOrNull(releasedDirection) else values.getOrNull(releasedDirection)
                finishGesture()
                performClick()
                output?.takeIf { it.isNotEmpty() }?.let(input)
                direction = 0
            }
            MotionEvent.ACTION_CANCEL -> finishGesture()
            MotionEvent.ACTION_POINTER_DOWN -> if (isLongClickable || longPressValues != null) finishGesture()
            MotionEvent.ACTION_POINTER_UP -> if (event.getPointerId(event.actionIndex)==activePointer) finishGesture()
        }
        return true
    }
    private fun showPreview(items: List<String>) {
        if (!previewEnabled) return
        popup?.dismiss()
        val bubble=FlickPreviewBubble(context,items)
        preview=bubble
        val density=resources.displayMetrics.density
        val bubbleWidth=(148*density).toInt(); val bubbleHeight=(160*density).toInt()
        popup = PopupWindow(bubble,bubbleWidth,bubbleHeight,false).apply {
            isTouchable = false
            isClippingEnabled = false
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val p = IntArray(2); getLocationOnScreen(p)
            val inWindow=IntArray(2); getLocationInWindow(inWindow)
            val margin=(4*density).toInt()
            val x=(p[0]+this@FlickKey.width/2-bubbleWidth/2).coerceIn(margin,(resources.displayMetrics.widthPixels-bubbleWidth-margin).coerceAtLeast(margin))
            val y=(p[1]-bubbleHeight+(6*density).toInt()).coerceAtLeast(margin)
            if (android.os.Build.VERSION.SDK_INT>=29) {
                setIsLaidOutInScreen(true)
                setIsClippedToScreen(true)
                showAtLocation(this@FlickKey,Gravity.NO_GRAVITY,x,y)
            } else {
                showAtLocation(this@FlickKey,Gravity.NO_GRAVITY,x-(p[0]-inWindow[0]),y-(p[1]-inWindow[1]))
            }
        }
    }
    private fun finishGesture() {
        removeCallbacks(normalLongPress); removeCallbacks(symbolLongPress)
        popup?.dismiss(); popup=null; preview=null
        isPressed=false; direction=0; activePointer=-1; symbolMode=false
        invalidate()
    }
    private fun updatePreview() { preview?.select(direction) }
    private fun directionAt(x: Float,y: Float): Int {
        val dx=x-downX; val dy=y-downY
        return if (maxOf(abs(dx),abs(dy))<threshold) 0
            else if (abs(dx)>abs(dy)) { if (dx<0) 1 else 3 } else { if (dy<0) 2 else 4 }
    }
    private fun symbolDirectionAt(x: Float,y: Float): Int {
        val symbols=longPressValues ?: return 0
        val dx=x-downX; val dy=y-downY
        val horizontal=symbols[1].isNotEmpty() || symbols[3].isNotEmpty()
        val vertical=symbols[2].isNotEmpty() || symbols[4].isNotEmpty()
        if (maxOf(abs(dx),abs(dy))<threshold) return 0
        return when {
            horizontal && !vertical -> if (dx<0) 1 else 3
            vertical && !horizontal -> if (dy<0) 2 else 4
            abs(dx)>abs(dy) -> if (dx<0) 1 else 3
            else -> if (dy<0) 2 else 4
        }
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onDetachedFromWindow() { finishGesture(); super.onDetachedFromWindow() }
}
