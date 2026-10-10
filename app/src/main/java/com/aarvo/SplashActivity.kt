package com.aarvo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity

/** Clean splash: one logo only, then the functional Welcome screen. */
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        val root = FrameLayout(this).apply { setBackgroundColor(AarvoScreenDesign.pageBackground) }
        val logo = ImageView(this).apply {
            setImageBitmap(LogoUtils.loadTransparentLogo(this@SplashActivity))
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO logo"
        }
        root.addView(logo, FrameLayout.LayoutParams(
            dp(AarvoScreenDesign.SPLASH_LOGO_DP),
            dp(AarvoScreenDesign.SPLASH_LOGO_DP),
            Gravity.CENTER
        ))
        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1400L)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
