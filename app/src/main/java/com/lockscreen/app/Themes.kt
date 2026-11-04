package com.lockscreen.app

import android.graphics.Color

object Themes {
    val names = listOf("Sunset", "Night", "Forest", "Berry")

    private val gradients = listOf(
        listOf("#C8532B", "#CF6A35", "#B96A4C", "#3B3A52", "#4A6A96", "#8FB4D9", "#A9C8E6"),
        listOf("#0B1026", "#151B3D", "#26305E", "#3B4A86", "#5A6FB0"),
        listOf("#1D3B2A", "#2E5A3F", "#47745A", "#7A9A7C", "#C5D4B8"),
        listOf("#3A1442", "#6B1E5E", "#A33276", "#D8607F", "#F2A27B")
    )

    private val clockColors = listOf("#BCD2EE", "#D7E3FF", "#F2F5E6", "#FFE9F0")

    fun gradient(i: Int): IntArray = gradients[i].map { Color.parseColor(it) }.toIntArray()

    fun clock(i: Int): Int = Color.parseColor(clockColors[i])
}
