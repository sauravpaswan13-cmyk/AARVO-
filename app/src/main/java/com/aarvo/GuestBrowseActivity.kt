package com.aarvo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Temporary test-entry bridge.
 * Opens the full AARVO application without requiring verification first.
 * Authentication remains available from inside the app after signing out.
 */
class GuestBrowseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("aarvo_prefs", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("onboarded", true)
            .putBoolean("signed_in", true)
            .putString("user_name", "AARVO Tester")
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
