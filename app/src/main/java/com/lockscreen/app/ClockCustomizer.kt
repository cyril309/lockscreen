package com.lockscreen.app

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

object ClockCustomizer {

    private var panelRef: View? = null

    val isOpen: Boolean
        get() = panelRef?.isAttachedToWindow == true

    fun show(activity: Activity, root: FrameLayout, clock: ClockView) {
        if (isOpen) return
        val prefs = activity.getSharedPreferences("lock", Context.MODE_PRIVATE)
        val d = activity.resources.displayMetrics.density
        fun px(v: Int): Int = (v * d).toInt()
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
        val match = ViewGroup.LayoutParams.MATCH_PARENT
        val samples = ArrayList<ClockView>()

        fun refresh() {
            clock.invalidate()
            for (s in samples) s.invalidate()
        }

        fun label(text: String): TextView {
            val t = TextView(activity)
            t.text = text
            t.textSize = 13f
            t.setTextColor(Color.parseColor("#B3FFFFFF"))
            t.setPadding(0, px(12), 0, px(4))
            return t
        }

        fun slider(max: Int, start: Int, onChange: (Int) -> Unit): SeekBar {
            val bar = SeekBar(activity)
            bar.max = max
            bar.progress = start
            bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) onChange(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            return bar
        }

        val panel = LinearLayout(activity)
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(px(16), px(14), px(16), px(18))
        val panelBg = GradientDrawable()
        val r = px(28).toFloat()
        panelBg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        panelBg.setColor(Color.parseColor("#EE1B1E2B"))
        panel.background = panelBg
        panel.isClickable = true

        fun close() {
            root.removeView(panel)
            panelRef = null
        }

        val header = FrameLayout(activity)
        val title = TextView(activity)
        title.text = "Font & Color"
        title.textSize = 15f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        header.addView(title, FrameLayout.LayoutParams(match, wrap, Gravity.CENTER))
        val done = TextView(activity)
        done.text = "Done"
        done.textSize = 15f
        done.setTextColor(Color.parseColor("#8FB4FF"))
        done.setPadding(px(8), px(6), px(8), px(6))
        done.setOnClickListener { close() }
        header.addView(done, FrameLayout.LayoutParams(wrap, wrap, Gravity.END or Gravity.CENTER_VERTICAL))
        panel.addView(header)

        val styleRow = LinearLayout(activity)
        styleRow.orientation = LinearLayout.HORIZONTAL
        styleRow.gravity = Gravity.CENTER
        val boxes = ArrayList<FrameLayout>()
        fun markStyle() {
            val cur = prefs.getInt("clock_style", 0)
            for (i in boxes.indices) {
                val g = GradientDrawable()
                g.cornerRadius = px(12).toFloat()
                g.setColor(Color.parseColor(if (i == cur) "#33FFFFFF" else "#14FFFFFF"))
                if (i == cur) g.setStroke(px(2), Color.WHITE)
                boxes[i].background = g
            }
        }
        for (i in 0 until 4) {
            val box = FrameLayout(activity)
            val sample = ClockView(activity, "12", i)
            samples.add(sample)
            box.addView(sample, FrameLayout.LayoutParams(px(46), px(66), Gravity.CENTER))
            box.setOnClickListener {
                prefs.edit().putInt("clock_style", i).apply()
                markStyle()
                refresh()
            }
            val bp = LinearLayout.LayoutParams(px(70), px(86))
            bp.setMargins(px(5), px(12), px(5), 0)
            styleRow.addView(box, bp)
            boxes.add(box)
        }
        markStyle()
        panel.addView(styleRow)

        panel.addView(label("Weight"))
        panel.addView(slider(100, prefs.getInt("clock_weight", 48)) {
            prefs.edit().putInt("clock_weight", it).apply()
            refresh()
        })

        panel.addView(label("Size"))
        panel.addView(slider(40, prefs.getInt("clock_size", 100) - 60) {
            prefs.edit().putInt("clock_size", it + 60).apply()
            refresh()
        })

        panel.addView(label("Color"))
        val colorValues = ArrayList<Int>()
        colorValues.add(0)
        for (hex in listOf("#FFFFFF", "#111111", "#7FD3FF", "#8FA7FF", "#D1B3FF", "#D78BFF", "#FFA6C9", "#FFC9A3")) {
            colorValues.add(Color.parseColor(hex))
        }
        val swatches = ArrayList<View>()
        fun markColor() {
            val cur = prefs.getInt("clock_color", 0)
            for (i in swatches.indices) {
                val g = GradientDrawable()
                g.shape = GradientDrawable.OVAL
                g.setColor(if (colorValues[i] == 0) Color.parseColor("#55FFFFFF") else colorValues[i])
                if (colorValues[i] == cur) g.setStroke(px(3), Color.WHITE) else g.setStroke(px(1), Color.parseColor("#66FFFFFF"))
                swatches[i].background = g
            }
        }
        val colorRow = LinearLayout(activity)
        colorRow.orientation = LinearLayout.HORIZONTAL
        for (i in colorValues.indices) {
            val sw = TextView(activity)
            sw.gravity = Gravity.CENTER
            sw.textSize = 12f
            sw.setTextColor(Color.WHITE)
            if (colorValues[i] == 0) sw.text = "Auto"
            sw.setOnClickListener {
                prefs.edit().putInt("clock_color", colorValues[i]).apply()
                markColor()
                refresh()
            }
            val sp = LinearLayout.LayoutParams(px(36), px(36))
            sp.setMargins(px(4), px(4), px(4), px(4))
            colorRow.addView(sw, sp)
            swatches.add(sw)
        }
        markColor()
        val colorScroll = HorizontalScrollView(activity)
        colorScroll.isHorizontalScrollBarEnabled = false
        colorScroll.addView(colorRow)
        panel.addView(colorScroll)

        val modeRow = LinearLayout(activity)
        modeRow.orientation = LinearLayout.HORIZONTAL
        modeRow.gravity = Gravity.CENTER
        val glassBtn = TextView(activity)
        val solidBtn = TextView(activity)
        fun markMode() {
            val glass = prefs.getBoolean("clock_glass", true)
            val g1 = GradientDrawable()
            g1.cornerRadius = px(16).toFloat()
            g1.setColor(Color.parseColor(if (glass) "#55FFFFFF" else "#1AFFFFFF"))
            glassBtn.background = g1
            val g2 = GradientDrawable()
            g2.cornerRadius = px(16).toFloat()
            g2.setColor(Color.parseColor(if (glass) "#1AFFFFFF" else "#55FFFFFF"))
            solidBtn.background = g2
        }
        for (b in listOf(glassBtn, solidBtn)) {
            b.textSize = 14f
            b.gravity = Gravity.CENTER
            b.setTextColor(Color.WHITE)
            b.setPadding(px(26), px(9), px(26), px(9))
            val bp = LinearLayout.LayoutParams(wrap, wrap)
            bp.setMargins(px(6), px(14), px(6), 0)
            modeRow.addView(b, bp)
        }
        glassBtn.text = "Glass"
        solidBtn.text = "Solid"
        glassBtn.setOnClickListener {
            prefs.edit().putBoolean("clock_glass", true).apply()
            markMode()
            refresh()
        }
        solidBtn.setOnClickListener {
            prefs.edit().putBoolean("clock_glass", false).apply()
            markMode()
            refresh()
        }
        markMode()
        panel.addView(modeRow)

        panelRef = panel
        root.addView(panel, FrameLayout.LayoutParams(match, wrap, Gravity.BOTTOM))
    }
}
