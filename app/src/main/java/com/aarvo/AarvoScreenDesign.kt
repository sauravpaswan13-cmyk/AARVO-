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

    // Shared authentication-screen design tokens. Change appearance here,
    // not inside PhoneAuthActivity, so UI tuning stays separate from OTP logic.
    val authPageBackground: Int = Color.rgb(247, 245, 255) // #F7F5FF
    val authPurple: Int = Color.rgb(75, 22, 216) // #4B16D8
    val authDeepPurple: Int = Color.rgb(50, 16, 142) // #32108E
    val authOrange: Int = Color.rgb(255, 122, 0) // #FF7A00
    val authSoftBackground: Int = Color.rgb(240, 236, 255) // #F0ECFF
    val authBrandMuted: Int = Color.rgb(119, 113, 139) // #77718B
    val authTextPrimary: Int = Color.rgb(23, 19, 41) // #171329
    val authTextSecondary: Int = Color.rgb(112, 106, 128) // #706A80
    val authFieldLabel: Int = Color.rgb(40, 34, 59) // #28223B
    val authLabelMuted: Int = Color.rgb(129, 122, 147) // #817A93
    val authDivider: Int = Color.rgb(216, 209, 238) // #D8D1EE
    val authPlaceholder: Int = Color.rgb(154, 148, 167) // #9A94A7
    val authFieldText: Int = Color.rgb(33, 26, 50) // #211A32
    val authDisabled: Int = Color.rgb(216, 210, 231) // #D8D2E7
    val authNote: Int = Color.rgb(130, 123, 144) // #827B90
    val authFooter: Int = Color.rgb(95, 88, 109) // #5F586D
    val authFooterMuted: Int = Color.rgb(138, 132, 149) // #8A8495

    const val AUTH_PAGE_HORIZONTAL_PADDING_DP = 20
    const val AUTH_PAGE_VERTICAL_PADDING_DP = 28
    const val AUTH_LOGO_SIZE_DP = 48
    const val AUTH_LOGO_TEXT_GAP_DP = 12
    const val AUTH_HEADER_TOP_GAP_DP = 38
    const val AUTH_CARD_TOP_GAP_DP = 22
    const val AUTH_CARD_CORNER_DP = 28
    const val AUTH_CARD_PADDING_DP = 22
    const val AUTH_BUTTON_HEIGHT_DP = 56
}
