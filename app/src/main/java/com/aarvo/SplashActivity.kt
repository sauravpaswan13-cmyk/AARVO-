package com.aarvo

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(36), dp(28), dp(36))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(27, 0, 88),
                    Color.rgb(74, 20, 190),
                    Color.rgb(194, 35, 185),
                    Color.rgb(35, 70, 190)
                )
            )
        }

        root.addView(TextView(this).apply {
            text = "AARVO"
            textSize = 52f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = .08f
            setShadowLayer(dp(18).toFloat(), 0f, dp(5).toFloat(), Color.argb(170, 0, 0, 0))
        }, LinearLayout.LayoutParams(-1, dp(78)))

        root.addView(TextView(this).apply {
            text = "SHOP • DISCOVER • LIVE BETTER"
            textSize = 12f
            setTextColor(Color.rgb(255, 220, 110))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = .14f
        }, LinearLayout.LayoutParams(-1, dp(32)))

        root.addView(TextView(this).apply {
            text = "Your premium shopping world"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            alpha = .92f
        }, LinearLayout.LayoutParams(-1, dp(38)).apply {
            topMargin = dp(6)
        })

        val accent = TextView(this).apply {
            text = "✦"
            textSize = 22f
            setTextColor(Color.rgb(255, 215, 90))
            gravity = Gravity.CENTER
        }
        root.addView(accent, LinearLayout.LayoutParams(-1, dp(42)).apply {
            topMargin = dp(18)
        })

        root.addView(ProgressBar(this).apply {
            isIndeterminate = true
            indeterminateTintList = ColorStateList.valueOf(Color.WHITE)
        }, LinearLayout.LayoutParams(dp(34), dp(34)).apply {
            topMargin = dp(10)
        })

        root.addView(TextView(this).apply {
            text = "Loading your world..."
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            alpha = .82f
        }, LinearLayout.LayoutParams(-1, dp(42)).apply {
            topMargin = dp(4)
        })

        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }, 1600)
    }
}
