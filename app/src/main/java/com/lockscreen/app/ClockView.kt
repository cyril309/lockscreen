package com.lockscreen.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.View
import java.util.Calendar

class ClockView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 10000)
        }
    }

    init {
        paint.color = Color.parseColor("#BCD2EE")
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
    }
    fun setColor(c: Int) {
        paint.color = c
        invalidate()
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(tick)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val cal = Calendar.getInstance()
        var hour = cal.get(Calendar.HOUR)
        if (hour == 0) hour = 12
        val minute = cal.get(Calendar.MINUTE)
        val hourDigits = hour.toString().map { it - '0' }
        val digits = hourDigits + listOf(minute / 10, minute % 10)

        val h = height.toFloat()
        val units = digits.size + 0.35f + 0.3f * (hourDigits.size + 2)
        var dw = width * 0.92f / units
        val maxDw = h * 0.21f
        if (dw > maxDw) dw = maxDw
        val gap = dw * 0.3f
        val colonW = dw * 0.35f
        val sw = dw * 0.28f
        val total = digits.size * dw + colonW + (hourDigits.size + 2) * gap
        var x = (width - total) / 2f

        for (i in digits.indices) {
            if (i == hourDigits.size) {
                paint.style = Paint.Style.FILL
                val cx = x + colonW / 2f
                canvas.drawCircle(cx, h * 0.25f, sw * 0.5f, paint)
                canvas.drawCircle(cx, h * 0.72f, sw * 0.5f, paint)
                x += colonW + gap
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = sw
            canvas.save()
            canvas.translate(x + sw / 2f, sw / 2f)
            canvas.drawPath(digitPath(digits[i], dw - sw, h - sw), paint)
            canvas.restore()
            x += dw + gap
        }
    }

    private fun digitPath(d: Int, w: Float, h: Float): Path {
        val p = Path()
        val r = w / 2f
        when (d) {
            0 -> p.addRoundRect(RectF(0f, 0f, w, h), r, r, Path.Direction.CW)
            1 -> {
                p.moveTo(w * 0.1f, h * 0.1f)
                p.lineTo(w / 2f, 0f)
                p.lineTo(w / 2f, h)
            }
            2 -> {
                p.moveTo(0f, r)
                p.quadTo(0f, 0f, w / 2f, 0f)
                p.quadTo(w, 0f, w, r)
                p.cubicTo(w, h * 0.5f, 0f, h * 0.5f, 0f, h)
                p.lineTo(w, h)
            }
            3 -> {
                p.moveTo(0f, r)
                p.quadTo(0f, 0f, w / 2f, 0f)
                p.quadTo(w, 0f, w, r)
                p.lineTo(w, h / 2f - r)
                p.quadTo(w, h / 2f, w / 2f, h / 2f)
                p.lineTo(w * 0.3f, h / 2f)
                p.moveTo(w / 2f, h / 2f)
                p.quadTo(w, h / 2f, w, h / 2f + r)
                p.lineTo(w, h - r)
                p.quadTo(w, h, w / 2f, h)
                p.quadTo(0f, h, 0f, h - r)
            }
            4 -> {
                p.moveTo(w * 0.75f, h)
                p.lineTo(w * 0.75f, 0f)
                p.lineTo(0f, h * 0.62f)
                p.lineTo(w, h * 0.62f)
            }
            5 -> {
                p.moveTo(w, 0f)
                p.lineTo(0f, 0f)
                p.lineTo(0f, h * 0.44f)
                p.lineTo(w / 2f, h * 0.44f)
                p.quadTo(w, h * 0.44f, w, h * 0.44f + r)
                p.lineTo(w, h - r)
                p.quadTo(w, h, w / 2f, h)
                p.quadTo(0f, h, 0f, h - r)
            }
            6 -> {
                p.addRoundRect(RectF(0f, h * 0.45f, w, h), r, r, Path.Direction.CW)
                p.moveTo(w, r)
                p.quadTo(w, 0f, w / 2f, 0f)
                p.quadTo(0f, 0f, 0f, r)
                p.lineTo(0f, h - r)
            }
            7 -> {
                p.moveTo(0f, 0f)
                p.lineTo(w, 0f)
                p.lineTo(w * 0.3f, h)
            }
            8 -> {
                p.addRoundRect(RectF(0f, 0f, w, h * 0.52f), r, r, Path.Direction.CW)
                p.addRoundRect(RectF(0f, h * 0.48f, w, h), r, r, Path.Direction.CW)
            }
            9 -> {
                p.addRoundRect(RectF(0f, 0f, w, h * 0.55f), r, r, Path.Direction.CW)
                p.moveTo(w, r)
                p.lineTo(w, h - r)
                p.quadTo(w, h, w / 2f, h)
                p.quadTo(0f, h, 0f, h - r)
            }
        }
        return p
    }
}
