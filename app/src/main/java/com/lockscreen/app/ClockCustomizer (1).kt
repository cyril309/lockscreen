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
        var needsRebuild = false

        fun refresh() {
            clock.invalidate()
            for (s in samples) s.invalidate()
        }

        fun label(text: String): TextView {
            val t = TextView(activity)
            t.text = text
            t.textSize = 13f
            t.setTextColor(Color.parseColor("#B3FFFFFF"))
            t.setPadding(0, px(14), 0, px(4))
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

        // A row of tappable chips. selected() says which are on; onClick runs when one is tapped.
        fun chips(labels: List<String>, selected: () -> Set<Int>, onClick: (Int) -> Unit): View {
            val row = LinearLayout(activity)
            row.orientation = LinearLayout.HORIZONTAL
            val views = ArrayList<TextView>()
            fun mark() {
                val sel = selected()
                for (i in views.indices) {
                    val g = GradientDrawable()
                    g.cornerRadius = px(16).toFloat()
                    g.setColor(Color.parseColor(if (i in sel) "#66FFFFFF" else "#1AFFFFFF"))
                    if (i in sel) g.setStroke(px(1), Color.WHITE)
                    views[i].background = g
                }
            }
            for (i in labels.indices) {
                val t = TextView(activity)
                t.text = labels[i]
                t.textSize = 14f
                t.setTextColor(Color.WHITE)
                t.gravity = Gravity.CENTER
                t.setPadding(px(16), px(8), px(16), px(8))
                t.setOnClickListener {
                    onClick(i)
                    mark()
                }
                val lp = LinearLayout.LayoutParams(wrap, wrap)
                lp.setMargins(px(4), px(4), px(4), px(4))
                row.addView(t, lp)
                views.add(t)
            }
            mark()
            val scroll = HorizontalScrollView(activity)
            scroll.isHorizontalScrollBarEnabled = false
            scroll.addView(row)
            return scroll
        }

        val panel = LinearLayout(activity)
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(px(16), px(14), px(16), px(30))
        val panelBg = GradientDrawable()
        val r = px(28).toFloat()
        panelBg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        panelBg.setColor(Color.parseColor("#FA1B1E2B"))
        panel.background = panelBg
        panel.isClickable = true

        val content = LinearLayout(activity)
        content.orientation = LinearLayout.VERTICAL

        fun close() {
            root.removeView(panel)
            panelRef = null
            if (needsRebuild) activity.recreate()
        }

        val header = FrameLayout(activity)
        val title = TextView(activity)
        title.text = "Customize"
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

        // Clock style
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
        for (i in 0 until 6) {
            val box = FrameLayout(activity)
            val sample = ClockView(activity, "12", i)
            samples.add(sample)
            box.addView(sample, FrameLayout.LayoutParams(px(46), px(60), Gravity.CENTER))
            box.setOnClickListener {
                prefs.edit().putInt("clock_style", i).apply()
                markStyle()
                refresh()
            }
            val bp = LinearLayout.LayoutParams(px(70), px(80))
            bp.setMargins(px(5), px(8), px(5), 0)
            styleRow.addView(box, bp)
            boxes.add(box)
        }
        markStyle()
        val styleScroll = HorizontalScrollView(activity)
        styleScroll.isHorizontalScrollBarEnabled = false
        styleScroll.addView(styleRow)
        content.addView(styleScroll)

        content.addView(label("Weight"))
        content.addView(slider(100, prefs.getInt("clock_weight", 48)) {
            prefs.edit().putInt("clock_weight", it).apply()
            refresh()
        })

        content.addView(label("Size"))
        content.addView(slider(40, prefs.getInt("clock_size", 100) - 60) {
            prefs.edit().putInt("clock_size", it + 60).apply()
            refresh()
        })

        // Color
        content.addView(label("Color"))
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
        content.addView(colorScroll)

        // Glass or solid
        content.addView(label("Fill"))
        content.addView(chips(
            listOf("Glass", "Solid"),
            { setOf(if (prefs.getBoolean("clock_glass", true)) 0 else 1) }
        ) {
            prefs.edit().putBoolean("clock_glass", it == 0).apply()
            refresh()
        })

        // Numerals
        content.addView(label("Numerals"))
        content.addView(chips(
            ClockView.numeralNames,
            { setOf(prefs.getInt("clock_numerals", 0)) }
        ) {
            prefs.edit().putInt("clock_numerals", it).apply()
            refresh()
        })

        // Layout
        content.addView(label("Clock layout"))
        content.addView(chips(
            listOf("Large", "Compact"),
            { setOf(prefs.getInt("clock_layout", 0)) }
        ) {
            prefs.edit().putInt("clock_layout", it).apply()
            needsRebuild = true
            refresh()
        })
        content.addView(chips(
            listOf("Date capsule"),
            { if (prefs.getBoolean("date_capsule", false)) setOf(0) else emptySet() }
        ) {
            prefs.edit().putBoolean("date_capsule", !prefs.getBoolean("date_capsule", false)).apply()
            needsRebuild = true
        })

        // Widgets
        content.addView(label("Widgets"))
        content.addView(chips(
            Widgets.names,
            {
                val on = Widgets.enabled(activity)
                Widgets.ids.indices.filter { Widgets.ids[it] in on }.toSet()
            }
        ) {
            val cur = Widgets.enabled(activity).toMutableSet()
            val id = Widgets.ids[it]
            if (id in cur) cur.remove(id) else cur.add(id)
            Widgets.save(activity, cur)
            needsRebuild = true
        })

        // Notifications
        content.addView(label("Notifications"))
        content.addView(chips(
            listOf("Stack", "List", "Count"),
            { setOf(prefs.getInt("notif_mode", 0)) }
        ) {
            prefs.edit().putInt("notif_mode", it).apply()
            needsRebuild = true
        })

        val maxH = (activity.resources.displayMetrics.heightPixels * 0.55f).toInt()
        val scroll = MaxHeightScroll(activity, maxH)
        scroll.isVerticalScrollBarEnabled = false
        scroll.addView(content)
        panel.addView(scroll)

        panelRef = panel
        root.addView(panel, FrameLayout.LayoutParams(match, wrap, Gravity.BOTTOM))
    }
}
