package com.aarvo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Loads the shared AARVO logo image used by Splash and Welcome.
 * Keep the source pixels unchanged; views use FIT_CENTER to preserve the full image.
 */
object LogoUtils {
    fun loadTransparentLogo(context: Context): Bitmap {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }

        return BitmapFactory.decodeResource(
            context.resources,
            R.drawable.aarvo_top_logo,
            options
        ) ?: throw IllegalStateException("AARVO shared logo asset could not be decoded")
    }
}
