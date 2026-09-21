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
            setPadding(28, 28, 28, 28)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(30, 0, 95), Color.rgb(74, 20, 190), Color.rgb(194, 35, 185), Color.rgb(35, 70, 190))
            )
        }

        root.addView(TextView(this).apply {
            text = "AARVO"
            textSize = 48f
            setTextColor(Color.rgb(255, 215, 90))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = .06f
            includeFontPadding = true
        }, LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 8
            bottomMargin = 2
        })

        root.addView(TextView(this).apply {
            text = "Shop Smart  •  Live Better"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 45))

        root.addView(ProgressBar(this).apply {
            isIndeterminate = true
        }, LinearLayout.LayoutParams(48, 48).apply { topMargin = 42 })

        root.addView(TextView(this).apply {
            text = "Loading your world..."
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 44))

        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, PhoneAuthActivity::class.java))
            finish()
        }, 1600)
    }
}
