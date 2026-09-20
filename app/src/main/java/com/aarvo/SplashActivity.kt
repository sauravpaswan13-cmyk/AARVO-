package com.aarvo

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * Crash-safe launcher.
 * The old welcome/onboarding screen is intentionally bypassed.
 * AARVO now opens directly to the mobile-number verification screen.
 */
class SplashActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(20, 0, 90))
            setPadding(32, 32, 32, 32)
        }

        root.addView(TextView(this).apply {
            text = "AARVO"
            textSize = 42f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2))

        root.addView(TextView(this).apply {
            text = "Shop Smart • Live Better"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2))

        root.addView(ProgressBar(this).apply {
            isIndeterminate = true
        }, LinearLayout.LayoutParams(-2, -2).apply {
            topMargin = 36
        })

        setContentView(root)

        handler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                startActivity(Intent(this, PhoneAuthActivity::class.java))
                finish()
            }
        }, 900L)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
