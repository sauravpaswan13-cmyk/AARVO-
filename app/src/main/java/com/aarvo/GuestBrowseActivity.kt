package com.aarvo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Guest-entry bridge: lets users browse the real marketplace without
 * verification. Authentication is requested when they try to purchase.
 */
class GuestBrowseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("aarvo_prefs", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("onboarded", true)
            .putBoolean("signed_in", false)
            .putBoolean("guest_mode", true)
            .remove("auth_token")
            .remove("user_name")
            .remove("user_role")
            .apply()

        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}
