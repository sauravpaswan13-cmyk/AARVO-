package com.aarvo

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(20, 0, 90), Color.rgb(54, 0, 168), Color.rgb(23, 0, 95))
            )
        }

        val title = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.WHITE)
            textSize = 42f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val tagline = TextView(this).apply {
            text = "Shop Smart • Live Better"
            setTextColor(Color.WHITE)
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 0)
        }
        root.addView(tagline, LinearLayout.LayoutParams(-1, -2))

        val progress = ProgressBar(this).apply { isIndeterminate = true }
        val progressParams = LinearLayout.LayoutParams(64, 64).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = 42
        }
        root.addView(progress, progressParams)

        val loading = TextView(this).apply {
            text = "Loading your world..."
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            alpha = 0.92f
            setPadding(0, 14, 0, 0)
        }
        root.addView(loading, LinearLayout.LayoutParams(-1, -2))

        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }, 1500L)
    }
}
