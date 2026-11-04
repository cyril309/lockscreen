package com.lockscreen.app

import android.app.Notification
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class NotifItem(
    val title: String,
    val text: String,
    val time: Long,
    val icon: Icon?,
    val color: Int
)

object Notifs {

    var items: List<NotifItem> = emptyList()

    private fun timeText(t: Long): String {
        val mins = (System.currentTimeMillis() - t) / 60000
        return when {
            mins < 1 -> "now"
            mins < 60 -> "${mins}m ago"
            mins < 1440 -> "${mins / 60}h ago"
            else -> "${mins / 1440}d ago"
        }
    }

    fun buildRow(context: Context): LinearLayout {
        val d = context.resources.displayMetrics.density
        fun px(v: Int): Int = (v * d).toInt()
        val hide = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
            .getBoolean("hidetext", false)

        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(px(16), 0, px(16), 0)

        val max = 2
        for (item in items.take(max)) {
            val card = LinearLayout(context)
            card.orientation = LinearLayout.HORIZONTAL
            card.gravity = Gravity.CENTER_VERTICAL
            card.setPadding(px(12), px(10), px(12), px(10))
            val cardBg = GradientDrawable()
            cardBg.cornerRadius = px(22).toFloat()
            cardBg.setColor(Color.parseColor("#66000000"))
            cardBg.setStroke(px(1), Color.parseColor("#33FFFFFF"))
            card.background = cardBg

            val iconView = ImageView(context)
            iconView.setPadding(px(8), px(8), px(8), px(8))
            val iconBg = GradientDrawable()
            iconBg.cornerRadius = px(10).toFloat()
            iconBg.setColor(if (item.color == 0) Color.parseColor("#5B6472") else item.color)
            iconView.background = iconBg
            try {
                val dr = item.icon?.loadDrawable(context)
                if (dr != null) {
                    iconView.setImageDrawable(dr)
                    iconView.setColorFilter(Color.WHITE)
                }
            } catch (e: Exception) {
            }
            card.addView(iconView, LinearLayout.LayoutParams(px(36), px(36)))

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
            body.maxLines = 1
            body.ellipsize = TextUtils.TruncateAt.END
            texts.addView(body)

            val textParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            textParams.setMargins(px(10), 0, px(8), 0)
            card.addView(texts, textParams)

            val time = TextView(context)
            time.text = timeText(item.time)
            time.textSize = 12f
            time.setTextColor(Color.parseColor("#B3FFFFFF"))
            card.addView(time)

            val cardParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            cardParams.setMargins(0, 0, 0, px(8))
            col.addView(card, cardParams)
        }

        if (items.size > max) {
            val more = TextView(context)
            more.text = "+" + (items.size - max) + " more"
            more.textSize = 12f
            more.setTextColor(Color.WHITE)
            more.setPadding(px(12), px(4), px(12), px(4))
            val moreBg = GradientDrawable()
            moreBg.cornerRadius = px(14).toFloat()
            moreBg.setColor(Color.parseColor("#55000000"))
            more.background = moreBg
            val moreParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            moreParams.gravity = Gravity.CENTER_HORIZONTAL
            col.addView(more, moreParams)
        }
        return col
    }
}

class NotifService : NotificationListenerService() {

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
                        NotifItem(title, text, n.postTime, n.notification.smallIcon, n.notification.color)
                    )
                }
            }
        } catch (e: Exception) {
        }
        Notifs.items = found
    }
}
