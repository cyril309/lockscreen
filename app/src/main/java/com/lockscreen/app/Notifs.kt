package com.lockscreen.app

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class NotifItem(
    val title: String,
    val text: String,
    val time: Long,
    val icon: Icon?,
    val color: Int,
    val pkg: String,
    val intent: PendingIntent?,
    val progress: Int,
    val progressMax: Int
)

class MaxHeightScroll(context: Context, private val maxH: Int) : ScrollView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST))
    }
}

object Notifs {

    @Volatile var items: List<NotifItem> = emptyList()

    // "Live activities": ongoing notifications such as downloads, calls, timers and media.
    @Volatile var live: List<NotifItem> = emptyList()

    // 0 = hidden, 1 = shown after you confirm it's you, 2 = always shown
    fun previewMode(context: Context): Int {
        val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        if (prefs.contains("preview_mode")) return prefs.getInt("preview_mode", 0).coerceIn(0, 2)
        return if (prefs.getBoolean("hidetext", true)) 0 else 2
    }

    @Volatile var expanded: Boolean = false

    private var listView: View? = null

    // True when a touch starts on the open notification list, so scrolling it does not trigger unlock.
    fun startsInList(y: Float): Boolean {
        val v = listView ?: return false
        if (!expanded || !v.isAttachedToWindow) return false
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        return y >= loc[1] && y <= loc[1] + v.height
    }

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

    // Unlock first (Android asks for your PIN or fingerprint), then open the app the notification belongs to.
    private fun openItem(context: Context, item: NotifItem) {
        val pi = item.intent ?: return
        fun fire() {
            try {
                pi.send()
            } catch (e: Exception) {
            }
        }
        val act = context as? Activity
        if (act == null) {
            fire()
            return
        }
        val km = act.getSystemService(KeyguardManager::class.java)
        if (!km.isKeyguardLocked) {
            fire()
            act.finish()
            return
        }
        km.requestDismissKeyguard(act, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                fire()
                act.finish()
            }
        })
    }

    // Android's own fingerprint, face or PIN prompt, like iOS revealing previews after Face ID.
    @SuppressLint("NewApi")
    @TargetApi(28)
    private fun authenticate(context: Context, onSuccess: () -> Unit) {
        val act = context as? Activity ?: return
        if (Build.VERSION.SDK_INT < 28) {
            Toast.makeText(act, "Needs Android 9 or newer", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val builder = BiometricPrompt.Builder(act)
                .setTitle("Show notification content")
                .setSubtitle("Confirm it's you")
            if (Build.VERSION.SDK_INT >= 30) {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                builder.setNegativeButton("Cancel", act.mainExecutor) { _, _ -> }
            }
            builder.build().authenticate(
                CancellationSignal(),
                act.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }
                }
            )
        } catch (e: Exception) {
            Toast.makeText(act, "Could not start the confirmation screen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun liveCard(context: Context, item: NotifItem, hide: Boolean, px: (Int) -> Int): LinearLayout {
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(px(12), px(10), px(12), px(10))
        col.background = roundBox("#99000000", 22, true, px)

        val top = LinearLayout(context)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(iconView(context, item, px), LinearLayout.LayoutParams(px(38), px(38)))

        val texts = LinearLayout(context)
        texts.orientation = LinearLayout.VERTICAL

        val title = TextView(context)
        title.text = if (hide) "Live activity" else item.title.ifEmpty { "Live activity" }
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
        body.maxLines = 1
        body.ellipsize = TextUtils.TruncateAt.END
        texts.addView(body)

        val tp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        tp.setMargins(px(10), 0, px(8), 0)
        top.addView(texts, tp)
        col.addView(top)

        if (!hide && item.progressMax > 0 && item.progress >= 0) {
            val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
            bar.max = item.progressMax
            bar.progress = item.progress
            bar.progressTintList = ColorStateList.valueOf(Color.WHITE)
            bar.progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#44FFFFFF"))
            val bp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(6))
            bp.setMargins(px(2), px(8), px(2), 0)
            col.addView(bar, bp)
        }
        col.setOnClickListener { openItem(context, item) }
        return col
    }

    private fun card(
        context: Context,
        item: NotifItem,
        hide: Boolean,
        lines: Int,
        tapToOpen: Boolean,
        px: (Int) -> Int
    ): LinearLayout {
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

        if (tapToOpen) card.setOnClickListener { openItem(context, item) }
        return card
    }

    fun buildRow(context: Context, maxHeight: Int): LinearLayout {
        expanded = false
        listView = null
        val d = context.resources.displayMetrics.density
        val px: (Int) -> Int = { v -> (v * d).toInt() }
        val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        val pMode = previewMode(context)
        var revealed = pMode == 2
        // 0 = stack, 1 = list, 2 = count
        val mode = prefs.getInt("notif_mode", 0).coerceIn(0, 2)
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
        val match = ViewGroup.LayoutParams.MATCH_PARENT

        val outer = LinearLayout(context)
        outer.orientation = LinearLayout.VERTICAL
        outer.setPadding(px(14), 0, px(14), 0)

        fun render(open: Boolean) {
            outer.removeAllViews()
            listView = null
            expanded = open && items.isNotEmpty()
            val hide = !revealed
            if (items.isEmpty() && live.isEmpty()) return

            if (pMode == 1 && !revealed) {
                val reveal = TextView(context)
                reveal.text = "Confirm it's you to show content"
                reveal.textSize = 12f
                reveal.setTextColor(Color.WHITE)
                reveal.gravity = Gravity.CENTER
                reveal.setPadding(px(14), px(7), px(14), px(7))
                reveal.background = roundBox("#66000000", 16, true, px)
                reveal.setOnClickListener {
                    authenticate(context) {
                        revealed = true
                        render(open)
                    }
                }
                val rp = LinearLayout.LayoutParams(wrap, wrap)
                rp.gravity = Gravity.CENTER_HORIZONTAL
                rp.setMargins(0, 0, 0, px(8))
                outer.addView(reveal, rp)
            }

            for (item in live.take(2)) {
                val lp = LinearLayout.LayoutParams(match, wrap)
                lp.setMargins(0, 0, 0, px(8))
                outer.addView(liveCard(context, item, hide, px), lp)
            }
            if (items.isEmpty()) return

            if (open) {
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
                    list.addView(card(context, item, hide, 2, true, px), lp)
                }
                val scroll = MaxHeightScroll(context, maxHeight - px(48))
                scroll.isVerticalScrollBarEnabled = false
                scroll.addView(list)
                outer.addView(scroll, LinearLayout.LayoutParams(match, wrap))
                listView = scroll
            } else if (mode == 2) {
                val pill = TextView(context)
                pill.text = "${items.size} notification" + (if (items.size == 1) "" else "s")
                pill.textSize = 14f
                pill.setTextColor(Color.WHITE)
                pill.gravity = Gravity.CENTER
                pill.setPadding(px(18), px(10), px(18), px(10))
                pill.background = roundBox("#8C000000", 24, true, px)
                pill.setOnClickListener { render(true) }
                val pp = LinearLayout.LayoutParams(wrap, wrap)
                pp.gravity = Gravity.CENTER_HORIZONTAL
                outer.addView(pill, pp)
            } else {
                val top = card(context, items[0], hide, 1, false, px)
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
            }
        }
        render(mode == 1)
        return outer
    }
}

class NotifService : NotificationListenerService() {

    override fun onListenerDisconnected() {
        Notifs.items = emptyList()
        Notifs.live = emptyList()
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

    private fun isLive(n: StatusBarNotification): Boolean {
        if (n.notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0) return true
        val category = n.notification.category ?: return false
        return category in setOf("progress", "transport", "call", "stopwatch", "navigation")
    }

    private fun refresh() {
        val found = ArrayList<NotifItem>()
        val liveFound = ArrayList<NotifItem>()
        // When previews are off, the text is never kept in memory at all.
        val keepText = Notifs.previewMode(this) != 0
        try {
            val active = activeNotifications
            if (active != null) {
                val sorted = active.sortedByDescending { it.postTime }
                for (n in sorted) {
                    if (n.packageName == packageName) continue
                    if ((n.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) continue
                    val ex = n.notification.extras
                    val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                    val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                    if (title.isEmpty() && text.isEmpty()) continue
                    val max = ex.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
                    val progress = ex.getInt(Notification.EXTRA_PROGRESS, 0)
                    val item = NotifItem(
                        if (keepText) title else "",
                        if (keepText) text else "",
                        n.postTime,
                        n.notification.smallIcon,
                        n.notification.color,
                        n.packageName,
                        n.notification.contentIntent,
                        if (max > 0) progress else -1,
                        max
                    )
                    if (n.isOngoing) {
                        if (isLive(n)) liveFound.add(item)
                    } else {
                        found.add(item)
                    }
                }
            }
        } catch (e: Exception) {
        }
        Notifs.items = found
        Notifs.live = liveFound
    }
}
