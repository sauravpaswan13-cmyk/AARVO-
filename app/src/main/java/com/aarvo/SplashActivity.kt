package com.aarvo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

/**
 * Launcher redirect only: skip the decorative splash/welcome screen and open
 * the existing phone-number login screen directly. Authentication remains in
 * PhoneAuthActivity and is intentionally not reimplemented here.
 */
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            startActivity(Intent(this, PhoneAuthActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        } catch (error: Throwable) {
            Log.e("AARVO-Launcher", "Could not open phone login", error)
            finish()
        }
    }
}
