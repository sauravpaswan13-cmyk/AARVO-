package com.aarvo

import android.graphics.Color

/**
 * Single source of truth for Splash and Welcome appearance.
 *
 * Change these values to tune entry screens without editing navigation/authentication.
 * Keep the logo in its own drawable resource; never redraw or recolor it in Kotlin.
 */
internal object AarvoScreenDesign {
    const val SHOW_BRAND_TEXT = false
    const val SHOW_TAGLINE = false
    const val SHOW_DECORATIVE_ILLUSTRATION = false
    const val SHOW_EMOJI_DECORATION = false

    const val LOGO_RESOURCE_NAME = "aarvo_entry_logo"
    const val SPLASH_LOGO_DP = 244
    const val WELCOME_LOGO_DP = 132
    const val PAGE_HORIZONTAL_PADDING_DP = 26
    const val PAGE_VERTICAL_PADDING_DP = 24
    const val BUTTON_HEIGHT_DP = 56
    const val BUTTON_GAP_DP = 12

    val pageBackground: Int = Color.WHITE
    val brandColor: Int = Color.rgb(13, 27, 62)
    val primaryButtonStart: Int = Color.rgb(0, 91, 255)
    val primaryButtonEnd: Int = Color.rgb(0, 72, 235)
    val secondaryBorder: Int = Color.rgb(0, 91, 255)
}
