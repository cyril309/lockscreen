package com.lockscreen.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.View
import java.util.Calendar

class ClockView(
    context: Context,
    private val fixedText: String? = null,
    private val forcedStyle: Int? = null
) : View(context) {

    companion object {
        val numeralNames = listOf("123", "١٢٣", "۱۲۳", "१२३", "১২৩", "๑๒๓")
        private val numeralBases = intArrayOf(0, 0x0660, 0x06F0, 0x0966, 0x09E6, 0x0E50)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private var tint = Color.parseColor("#BCD2EE")

    // Styles: tall, tall rounded, compact, wide, tall outline, rounded outline.
    private val heightFrac = floatArrayOf(1f, 1f, 0.62f, 0.48f, 1f, 1f)
    private val widthFrac = floatArrayOf(0.66f, 0.66f, 0.84f, 0.92f, 0.66f, 0.66f)
    private val maxDwRatio = floatArrayOf(0.30f, 0.30f, 0.42f, 0.46f, 0.30f, 0.30f)
    private val rounded = booleanArrayOf(false, true, false, true, false, true)

    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 10000)
        }
    }

    fun setColor(c: Int) {
        tint = c
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (fixedText == null) handler.post(tick)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    private fun withAlpha(c: Int, a: Int): Int =
        Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

    private fun widthFactor(d: Int, numerals: Int): Float =
        if (numerals == 0 && d == 1) 0.55f else 1f

    override fun onDraw(canvas: Canvas) {
        val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        val style = (forcedStyle ?: prefs.getInt("clock_style", 0)).coerceIn(0, 5)
        val weight = prefs.getInt("clock_weight", 48).coerceIn(0, 100)
        val glass = prefs.getBoolean("clock_glass", true)
        val numerals = prefs.getInt("clock_numerals", 0).coerceIn(0, numeralBases.size - 1)
        val compact = fixedText == null && prefs.getInt("clock_layout", 0) == 1
        val sizeScale = if (fixedText != null) 1f else prefs.getInt("clock_size", 100).coerceIn(60, 100) / 100f
        val saved = prefs.getInt("clock_color", 0)
        val base = if (saved != 0) saved else tint
        val outline = style >= 4
        val density = resources.displayMetrics.density

        val digits: List<Int>
        val colonAt: Int
        if (fixedText != null) {
            digits = fixedText.map { it - '0' }
            colonAt = -1
        } else {
            val cal = Calendar.getInstance()
            var hour = cal.get(Calendar.HOUR)
            if (hour == 0) hour = 12
            val minute = cal.get(Calendar.MINUTE)
            val hourDigits = hour.toString().map { it - '0' }
            digits = hourDigits + listOf(minute / 10, minute % 10)
            colonAt = hourDigits.size
        }

        val h = height.toFloat() * heightFrac[style] * sizeScale * (if (compact) 0.26f else 1f)
        paint.strokeCap = if (rounded[style]) Paint.Cap.ROUND else Paint.Cap.BUTT
        paint.strokeJoin = if (rounded[style]) Paint.Join.ROUND else Paint.Join.MITER
        paint.color = base
        paint.shader = if (glass) {
            LinearGradient(
                0f, 0f, 0f, h,
                withAlpha(base, 0xF2), withAlpha(base, 0x99),
                Shader.TileMode.CLAMP
            )
        } else {
            null
        }

        var weightSum = 0f
        for (d in digits) weightSum += widthFactor(d, numerals)
        val gapUnits = 0.28f
        val colonUnits = if (colonAt >= 0) 0.3f else 0f
        val gapCount = if (colonAt >= 0) digits.size else digits.size - 1
        val units = weightSum + colonUnits + gapUnits * gapCount

        var dw = width * widthFrac[style] * (if (compact) 0.5f else 1f) / units
        val maxDw = h * maxDwRatio[style]
        if (dw > maxDw) dw = maxDw
        val gap = dw * gapUnits
        val colonW = dw * colonUnits
        val sw = dw * (0.16f + 0.22f * weight / 100f)
        val line = maxOf(density * 1.5f, dw * 0.035f)
        val total = weightSum * dw + colonW + gap * gapCount
        var x = (width - total) / 2f

        for (i in digits.indices) {
            if (i == colonAt) {
                paint.style = if (outline) Paint.Style.STROKE else Paint.Style.FILL
                paint.strokeWidth = line
                val cx = x + colonW / 2f
                canvas.drawCircle(cx, h * 0.32f, sw * 0.5f, paint)
                canvas.drawCircle(cx, h * 0.68f, sw * 0.5f, paint)
                x += colonW + gap
            }
            val cw = dw * widthFactor(digits[i], numerals)
            canvas.save()
            canvas.translate(x + sw / 2f, sw / 2f)
            if (numerals == 0) {
                val path = digitPath(digits[i], cw - sw, h - sw)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = sw
                if (outline) {
                    val outlinePath = Path()
                    paint.getFillPath(path, outlinePath)
                    paint.strokeWidth = line
                    canvas.drawPath(outlinePath, paint)
                } else {
                    canvas.drawPath(path, paint)
                }
            } else {
                val glyph = glyphPath(numeralBases[numerals] + digits[i], cw - sw, h - sw)
                if (outline) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = line
                } else {
                    paint.style = Paint.Style.FILL
                }
                canvas.drawPath(glyph, paint)
            }
            canvas.restore()
            x += cw + gap
        }
    }

    // Digits of other numeral systems come from the phone's own fonts, stretched tall to match the clock.
    private fun glyphPath(code: Int, w: Float, h: Float): Path {
        val s = String(Character.toChars(code))
        glyphPaint.textSize = 200f
        glyphPaint.typeface = Typeface.DEFAULT
        val raw = Path()
        glyphPaint.getTextPath(s, 0, s.length, 0f, 0f, raw)
        val b = RectF()
        raw.computeBounds(b, true)
        if (b.width() <= 0f || b.height() <= 0f || w <= 0f || h <= 0f) return raw
        val sy = h / b.height()
        val sx = minOf(sy * 0.42f, w / b.width())
        val m = Matrix()
        m.postTranslate(-b.left, -b.top)
        m.postScale(sx, sy)
        m.postTranslate((w - b.width() * sx) / 2f, 0f)
        raw.transform(m)
        return raw
    }

    private fun digitPath(d: Int, w: Float, h: Float): Path {
        val p = Path()
        val r = w / 2f
        when (d) {
            0 -> p.addRoundRect(RectF(0f, 0f, w, h), r, r, Path.Direction.CW)
            1 -> {
                p.moveTo(0f, h * 0.09f)
                p.lineTo(w, 0f)
                p.lineTo(w, h)
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
