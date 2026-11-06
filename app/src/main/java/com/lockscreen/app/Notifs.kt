package com.lockscreen.app

import android.app.Notification
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class NotifItem(
    val title: String,
    val text: String,
    val time: Long,
    val icon: Icon?,
    val color: Int,
    val pkg: String
)

class MaxHeightScroll(context: Context, private val maxH: Int) : ScrollView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST))
    }
}

object Notifs {

    @Volatile var items: List<NotifItem> = emptyList()

    // True while the full notification list is open, so the swipe-up gesture does not fire while scrolling.
    @Volatile var expanded: Boolean = false

    private fun timeText(t: Long): String {
        val mins = (System.currentTimeMillis() - t) / 60000
        return when {
            mins < 1 -> "now"
            mins < 60 -> "${mins}m ago"
            mins < 1440 -> "${mins / 60}h ago"
            else -> "${mins / 1440}d ago"
        }
    }

    @Suppress("DEPRECATION")
    private fun appLabel(context: Context, pkg: String): String? {
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun roundBox(color: String, radius: Int, stroke: Boolean, px: (Int) -> Int): GradientDrawable {
        val g = GradientDrawable()
        g.cornerRadius = px(radius).toFloat()
        g.setColor(Color.parseColor(color))
        if (stroke) g.setStroke(px(1), Color.parseColor("#33FFFFFF"))
        return g
    }

    private fun iconView(context: Context, item: NotifItem, px: (Int) -> Int): ImageView {
        val v = ImageView(context)
        val bg = GradientDrawable()
        bg.cornerRadius = px(10).toFloat()
        var shown = false
        try {
            v.setImageDrawable(context.packageManager.getApplicationIcon(item.pkg))
            shown = true
        } catch (e: Exception) {
        }
        if (!shown) {
            bg.setColor(if (item.color == 0) Color.parseColor("#5B6472") else item.color)
            v.setPadding(px(8), px(8), px(8), px(8))
            try {
                val dr = item.icon?.loadDrawable(context)
                if (dr != null) {
                    v.setImageDrawable(dr)
                    v.setColorFilter(Color.WHITE)
                }
            } catch (e: Exception) {
            }
        }
        v.background = bg
        v.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, px(10).toFloat())
            }
        }
        v.clipToOutline = true
        return v
    }

    private fun card(context: Context, item: NotifItem, hide: Boolean, lines: Int, px: (Int) -> Int): LinearLayout {
        val card = LinearLayout(context)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.setPadding(px(12), px(10), px(12), px(10))
        card.background = roundBox("#8C000000", 22, true, px)

        card.addView(iconView(context, item, px), LinearLayout.LayoutParams(px(38), px(38)))

        val texts = LinearLayout(context)
        texts.orientation = LinearLayout.VERTICAL

        val title = TextView(context)
        title.text = if (hide) "Notification" else item.title.ifEmpty { "New message" }
        title.textSize = 14f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.WHITE)
        title.maxLines = 1
        title.ellipsize = TextUtils.TruncateAt.END
        texts.addView(title)

        val body = TextView(context)
        body.text = if (hide) "Unlock to view" else item.text
        body.textSize = 13f
        body.setTextColor(Color.parseColor("#E6FFFFFF"))
        body.maxLines = lines
        body.ellipsize = TextUtils.TruncateAt.END
        texts.addView(body)

        val textParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        textParams.setMargins(px(10), 0, px(8), 0)
        card.addView(texts, textParams)

        val time = TextView(context)
        time.text = timeText(item.time)
        time.textSize = 12f
        time.setTextColor(Color.parseColor("#B3FFFFFF"))
        time.gravity = Gravity.TOP
        card.addView(time)
        return card
    }

    fun buildRow(context: Context, maxHeight: Int): LinearLayout {
        expanded = false
        val d = context.resources.displayMetrics.density
        val px: (Int) -> Int = { v -> (v * d).toInt() }
        val hide = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
            .getBoolean("hidetext", true)
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
        val match = ViewGroup.LayoutParams.MATCH_PARENT

        val outer = LinearLayout(context)
        outer.orientation = LinearLayout.VERTICAL
        outer.setPadding(px(14), 0, px(14), 0)

        fun render(open: Boolean) {
            outer.removeAllViews()
            expanded = open && items.isNotEmpty()
            if (items.isEmpty()) return

            if (!open) {
                val top = card(context, items[0], hide, 1, px)
                top.translationZ = 2f
                top.outlineProvider = null
                top.setOnClickListener { render(true) }

                val peekCount = minOf(2, items.size - 1)
                val peeks = ArrayList<View>()
                for (i in 1..peekCount) {
                    val p = View(context)
                    p.background = roundBox(if (i == 1) "#66000000" else "#40000000", 20, true, px)
                    p.translationZ = (2 - i).toFloat()
                    p.outlineProvider = null
                    p.setOnClickListener { render(true) }
                    peeks.add(p)
                }

                outer.addView(top, LinearLayout.LayoutParams(match, wrap))
                for (i in peeks.indices) {
                    val lp = LinearLayout.LayoutParams(match, px(26))
                    lp.setMargins(px(10 * (i + 1)), -px(18), px(10 * (i + 1)), 0)
                    outer.addView(peeks[i], lp)
                }

                if (items.size > 1) {
                    val rest = items.size - 1
                    val labels = ArrayList<String>()
                    if (!hide) {
                        for (n in items.drop(1)) {
                            val l = appLabel(context, n.pkg)
                            if (l != null && !labels.contains(l)) labels.add(l)
                            if (labels.size == 3) break
                        }
                    }
                    val more = TextView(context)
                    more.text = if (labels.isEmpty()) {
                        "$rest more notification" + (if (rest == 1) "" else "s")
                    } else {
                        "$rest more from " + labels.joinToString(", ")
                    }
                    more.textSize = 12f
                    more.setTextColor(Color.WHITE)
                    more.gravity = Gravity.CENTER
                    more.maxLines = 1
                    more.ellipsize = TextUtils.TruncateAt.END
                    more.setPadding(px(12), px(8), px(12), px(4))
                    more.setOnClickListener { render(true) }
                    outer.addView(more, LinearLayout.LayoutParams(match, wrap))
                }
            } else {
                val header = LinearLayout(context)
                header.orientation = LinearLayout.HORIZONTAL
                header.gravity = Gravity.CENTER_VERTICAL
                header.setPadding(px(4), 0, px(4), px(8))

                val title = TextView(context)
                title.text = "Notification Center"
                title.textSize = 16f
                title.setTypeface(null, Typeface.BOLD)
                title.setTextColor(Color.WHITE)
                header.addView(title, LinearLayout.LayoutParams(0, wrap, 1f))

                val less = TextView(context)
                less.text = "Show less"
                less.textSize = 12f
                less.setTextColor(Color.WHITE)
                less.setPadding(px(12), px(6), px(12), px(6))
                less.background = roundBox("#66000000", 16, true, px)
                less.setOnClickListener { render(false) }
                header.addView(less)

                val close = TextView(context)
                close.text = "✕"
                close.textSize = 12f
                close.setTextColor(Color.WHITE)
                close.gravity = Gravity.CENTER
                close.background = roundBox("#66000000", 16, true, px)
                close.setOnClickListener { render(false) }
                val cp = LinearLayout.LayoutParams(px(32), px(32))
                cp.setMargins(px(8), 0, 0, 0)
                header.addView(close, cp)
                outer.addView(header, LinearLayout.LayoutParams(match, wrap))

                val list = LinearLayout(context)
                list.orientation = LinearLayout.VERTICAL
                for (item in items) {
                    val lp = LinearLayout.LayoutParams(match, wrap)
                    lp.setMargins(0, 0, 0, px(8))
                    list.addView(card(context, item, hide, 2, px), lp)
                }
                val scroll = MaxHeightScroll(context, maxHeight - px(48))
                scroll.isVerticalScrollBarEnabled = false
                scroll.addView(list)
                outer.addView(scroll, LinearLayout.LayoutParams(match, wrap))
            }
        }
        render(false)
        return outer
    }
}

class NotifService : NotificationListenerService() {

    override fun onListenerDisconnected() {
        Notifs.items = emptyList()
        super.onListenerDisconnected()
    }

    override fun onListenerConnected() {
        refresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        refresh()
    }

    private fun refresh() {
        val found = ArrayList<NotifItem>()
        try {
            val active = activeNotifications
            if (active != null) {
                val sorted = active.sortedByDescending { it.postTime }
                for (n in sorted) {
                    if (n.packageName == packageName) continue
                    if (n.isOngoing) continue
                    if ((n.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) continue
                    val ex = n.notification.extras
                    val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                    val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                    if (title.isEmpty() && text.isEmpty()) continue
                    found.add(
                        NotifItem(
                            title,
                            text,
                            n.postTime,
                            n.notification.smallIcon,
                            n.notification.color,
                            n.packageName
                        )
                    )
                }
            }
        } catch (e: Exception) {
        }
        Notifs.items = found
    }
}
