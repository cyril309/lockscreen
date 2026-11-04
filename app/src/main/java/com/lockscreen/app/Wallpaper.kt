package com.lockscreen.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object Wallpaper {
    private fun file(context: Context): File = File(context.filesDir, "wallpaper.jpg")

    fun save(context: Context, uri: Uri): Boolean {
        var bitmap: Bitmap? = null
        val destination = file(context)
        val temporary = File(context.filesDir, "wallpaper.tmp")
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

            var sample = 1
            while (bounds.outHeight / sample > 1800 || bounds.outWidth / sample > 1800) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return false
            bitmap = decoded

            val compressed = FileOutputStream(temporary).use {
                decoded.compress(Bitmap.CompressFormat.JPEG, 88, it)
            }
            if (!compressed) {
                temporary.delete()
                return false
            }
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (_: Exception) {
            temporary.delete()
            false
        } finally {
            bitmap?.recycle()
        }
    }

    fun load(context: Context): Bitmap? =
        file(context).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    fun clear(context: Context) {
        file(context).delete()
        File(context.filesDir, "wallpaper.tmp").delete()
    }
}
