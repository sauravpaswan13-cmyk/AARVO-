package com.aarvo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.abs
import kotlin.math.max

/**
 * Startup-safe loader for the canonical AARVO entry logo.
 * Splash and Welcome must use the exact same original asset.
 */
object LogoUtils {
    fun loadTransparentLogo(context: Context): Bitmap {
        // IMPORTANT: aarvo_entry_logo is the canonical original AARVO logo.
        // Do not switch this to aarvo_logo: that is a different/general app asset.
        val resId = R.drawable.aarvo_entry_logo

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, bounds)

        val sourceMax = max(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (sourceMax > 192 * sample) sample *= 2

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sample
        }

        val decoded = BitmapFactory.decodeResource(context.resources, resId, options)
            ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        val bitmap = if (decoded.config == Bitmap.Config.ARGB_8888 && decoded.isMutable) {
            decoded
        } else {
            decoded.copy(Bitmap.Config.ARGB_8888, true).also { decoded.recycle() }
        }

        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return bitmap

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val corner = pixels[0]
        val cr = (corner ushr 16) and 0xFF
        val cg = (corner ushr 8) and 0xFF
        val cb = corner and 0xFF
        val blueBackdrop = cb > 105 && cb > cr + 10 && cb > cg + 5

        if (blueBackdrop) {
            for (i in pixels.indices) {
                val p = pixels[i]
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                if (b > 95 && b > r + 10 && b > g + 5 &&
                    abs(r - cr) + abs(g - cg) + abs(b - cb) <= 60
                ) {
                    pixels[i] = p and 0x00FFFFFF
                }
            }
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        }
        return bitmap
    }
}
