package com.aarvo

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Visual-only preview entry points for AARVO screen work.
 * These previews do not change runtime navigation, OTP, Firebase, or backend logic.
 */
@Preview(
    name = "AARVO Home Header",
    widthDp = 393,
    heightDp = 852,
    showBackground = true
)
@Composable
fun AarvoHomeHeaderPreview() {
    PremiumHomeHeader()
}
