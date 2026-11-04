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
    fun cached(context: Context): String? =
        context.getSharedPreferences("lock", Context.MODE_PRIVATE).getString("weather", null)

    fun refresh(context: Context, onDone: (String) -> Unit) {
        val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        val age = System.currentTimeMillis() - prefs.getLong("weather_time", 0L)
        if (age in 0 until 20 * 60 * 1000L && prefs.getString("weather", null) != null) return

        Thread {
            val location = lastLocation(context) ?: return@Thread
            var connection: HttpURLConnection? = null
            try {
                val address = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${location.latitude}" +
                    "&longitude=${location.longitude}" +
                    "&current=temperature_2m,weather_code"
                connection = URL(address).openConnection() as HttpURLConnection
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val current = JSONObject(body).getJSONObject("current")
                val temperature = current.getDouble("temperature_2m").roundToInt()
                val conditions = describeWeatherCode(current.getInt("weather_code"))
                val result = if (conditions.isEmpty()) "$temperature°" else "$temperature° $conditions"

                prefs.edit()
                    .putString("weather", result)
                    .putLong("weather_time", System.currentTimeMillis())
                    .apply()
                onDone(result)
            } catch (_: Exception) {
                // Weather is optional; keep the cached result and never block the screen.
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun lastLocation(context: Context): Location? {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) return null

        val manager = context.getSystemService(LocationManager::class.java)
        var best: Location? = null
        return try {
            for (provider in manager.getProviders(true)) {
                val location = manager.getLastKnownLocation(provider)
                if (location != null && (best == null || location.time > best.time)) best = location
            }
            best
        } catch (_: SecurityException) {
            null
        }
    }
}

internal fun describeWeatherCode(code: Int): String = when (code) {
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
