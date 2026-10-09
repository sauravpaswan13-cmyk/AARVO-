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
import android.widget.TextView
import androidx.activity.ComponentActivity

/** AARVO splash: original glossy A mark, followed by the Welcome Entry screen. */
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(255,255,255), Color.rgb(255,250,245), Color.rgb(247,241,255))
            )
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(20), dp(28), dp(20))
        }
        val logo = ImageView(this).apply {
            setImageBitmap(LogoUtils.loadTransparentLogo(this@SplashActivity))
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO original glossy A logo"
        }
        column.addView(logo, LinearLayout.LayoutParams(dp(244), dp(244)))
        val brand = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.rgb(13, 27, 62))
            textSize = 36f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.02f
        }
        column.addView(brand, LinearLayout.LayoutParams(-1, -2))
        val tagline = TextView(this).apply {
            text = "S H O P   M O R E   •   L I V E   B E T T E R"
            setTextColor(Color.rgb(13, 27, 62))
            textSize = 11f
            gravity = Gravity.CENTER
            letterSpacing = 0.04f
        }
        val tagLp = LinearLayout.LayoutParams(-1, -2)
        tagLp.topMargin = dp(6)
        column.addView(tagline, tagLp)
        root.addView(column, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1800L)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
