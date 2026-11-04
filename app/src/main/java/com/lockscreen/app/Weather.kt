package com.lockscreen.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

object Weather {

    fun cached(context: Context): String? {
        val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        return prefs.getString("weather", null)
    }

    fun refresh(context: Context, onDone: (String) -> Unit) {
        val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        val age = System.currentTimeMillis() - prefs.getLong("weather_time", 0L)
        if (age < 20 * 60 * 1000L && prefs.getString("weather", null) != null) return

        Thread {
            try {
                val loc = lastLocation(context)
                if (loc != null) {
                    val address = "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=" + loc.latitude +
                        "&longitude=" + loc.longitude +
                        "&current=temperature_2m,weather_code"
                    val conn = URL(address).openConnection() as HttpURLConnection
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()

                    val cur = JSONObject(body).getJSONObject("current")
                    val temp = cur.getDouble("temperature_2m").roundToInt()
                    val word = describe(cur.getInt("weather_code"))
                    val text = if (word.isEmpty()) "$temp°" else "$temp° $word"

                    prefs.edit()
                        .putString("weather", text)
                        .putLong("weather_time", System.currentTimeMillis())
                        .apply()
                    onDone(text)
                }
            } catch (e: Exception) {
            }
        }.start()
    }

    private fun lastLocation(context: Context): Location? {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (granted != PackageManager.PERMISSION_GRANTED) return null
        val lm = context.getSystemService(LocationManager::class.java)
        var best: Location? = null
        for (provider in lm.getProviders(true)) {
            try {
                val l = lm.getLastKnownLocation(provider)
                if (l != null && (best == null || l.time > best.time)) best = l
            } catch (e: Exception) {
            }
        }
        return best
    }

    private fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        in 51..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        in 95..99 -> "Storm"
        else -> ""
    }
}
