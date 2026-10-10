package com.aarvo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Loads the shared AARVO entry logo without modifying its pixels.
 *
 * Do not recolor, remove a background, crop, or downsample the source here.
 * Splash and Welcome use FIT_CENTER so the complete image keeps its aspect ratio.
 */
object LogoUtils {
    fun loadTransparentLogo(context: Context): Bitmap {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            // Prevent Android from density-scaling this bitmap resource.
            inScaled = false
        }

        return BitmapFactory.decodeResource(
            context.resources,
            R.drawable.aarvo_entry_logo,
            options
        ) ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
}
