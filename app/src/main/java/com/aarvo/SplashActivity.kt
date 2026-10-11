package com.aarvo

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity

/** Premium branded splash using the existing PNG logo asset, then opens phone login directly. */
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.rgb(28, 20, 78)

        val background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(255, 255, 255), Color.rgb(247, 244, 255), Color.rgb(235, 229, 255))
        )
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
            setBackground(background)
        }

        val logo = ImageView(this).apply {
            setImageBitmap(LogoUtils.loadTransparentLogo(this@SplashActivity))
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO PNG logo"
        }
        root.addView(logo, LinearLayout.LayoutParams(dp(150), dp(150)))

        val brand = TextView(this).apply {
            text = "AARVO"
            textSize = 34f
            letterSpacing = 0.18f
            setTextColor(Color.rgb(55, 37, 130))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        root.addView(brand, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        val tagline = TextView(this).apply {
            text = "SHOP SMART  •  LIVE BETTER"
            textSize = 12f
            letterSpacing = 0.12f
            setTextColor(Color.rgb(111, 94, 166))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }
        root.addView(tagline, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })

        val accent = View(this).apply {
            setBackground(GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(104, 72, 214), Color.rgb(255, 150, 55))
            ).apply { cornerRadius = dp(3).toFloat() })
        }
        root.addView(accent, LinearLayout.LayoutParams(dp(58), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(28)
        })

        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.rgb(104, 72, 214))
        }
        root.addView(progress, LinearLayout.LayoutParams(dp(104), dp(3)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(24)
        })

        val loading = TextView(this).apply {
            text = "PREPARING YOUR EXPERIENCE"
            textSize = 10f
            letterSpacing = 0.12f
            setTextColor(Color.rgb(126, 116, 155))
            gravity = Gravity.CENTER
        }
        root.addView(loading, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        setContentView(root)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, PhoneAuthActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1400L)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
