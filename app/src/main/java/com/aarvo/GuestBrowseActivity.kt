package com.aarvo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Guest-entry bridge: lets users browse the real marketplace without
 * verification. Authentication is required before an order can be created.
 */
class GuestBrowseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("aarvo_prefs", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("onboarded", true)
            .putBoolean("signed_in", true)
            .putBoolean("guest_mode", true)
            .putString("user_name", "Guest")
            .putString("user_role", "BUYER")
            .remove("auth_token")
            .apply()

        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}
