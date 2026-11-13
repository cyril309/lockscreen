package com.lockscreen.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

// The "depth effect": a cut-out of the subject of your wallpaper (a PNG with a transparent
// background, the same photo) is drawn in front of the clock, so the clock appears behind it.
object Depth {

    private fun file(context: Context): File = File(context.filesDir, "depth.png")

    fun save(context: Context, uri: Uri): Boolean {
        return try {
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (bounds.outHeight / sample > 1800 || bounds.outWidth / sample > 1800) {
                sample *= 2
            }
            val opts = BitmapFactory.Options()
            opts.inSampleSize = sample
            val bmp = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return false
            if (!bmp.hasAlpha()) return false
            FileOutputStream(file(context)).use {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // Returns null when there is no cut-out or the effect is switched off.
    fun load(context: Context): Bitmap? {
        val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("depth_on", true)) return null
        val f = file(context)
        return if (f.exists()) BitmapFactory.decodeFile(f.path) else null
    }

    fun clear(context: Context) {
        file(context).delete()
    }
}
