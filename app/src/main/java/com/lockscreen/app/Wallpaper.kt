package com.lockscreen.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object Wallpaper {

    private fun file(context: Context): File = File(context.filesDir, "wallpaper.jpg")

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
            FileOutputStream(file(context)).use {
                bmp.compress(Bitmap.CompressFormat.JPEG, 88, it)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun load(context: Context): Bitmap? {
        val f = file(context)
        return if (f.exists()) BitmapFactory.decodeFile(f.path) else null
    }

    fun clear(context: Context) {
        file(context).delete()
    }
}
