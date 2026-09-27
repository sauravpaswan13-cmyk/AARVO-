package com.aarvo

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        val bytes = Base64.decode(SplashReferenceImage.WEBP_BASE64, Base64.DEFAULT)
        val image = android.widget.ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            scaleType = android.widget.ImageView.ScaleType.FIT_XY
            contentDescription = "AARVO premium splash screen"
        }
        root.addView(image, FrameLayout.LayoutParams(-1, -1))

        // Keep the supplied AARVO artwork/logo untouched; only add a subtle premium light sweep.
        val laser = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, 0x18FFFFFF, 0x72FFFFFF, 0x18FFFFFF, Color.TRANSPARENT)
            )
            alpha = 0.65f
        }
        root.addView(laser, FrameLayout.LayoutParams(110, -1))

        // Elegant loading treatment, deliberately added as an overlay so the reference artwork remains unchanged.
        val loading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, 0)
        }
        val label = TextView(this).apply {
            text = "SHOP SMART  •  LIVE BETTER"
            setTextColor(Color.WHITE)
            textSize = 11f
            letterSpacing = 0.18f
            gravity = Gravity.CENTER
            alpha = 0.88f
        }
        loading.addView(label, LinearLayout.LayoutParams(-2, 32))

        val track = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 30f
                setColor(0x331FFFFF)
                setStroke(1, 0x66FFFFFF)
            }
        }
        val fill = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFFE44D9B.toInt(), 0xFF8B5CF6.toInt(), 0xFF4F8CFF.toInt())
            )
        }
        track.addView(fill, FrameLayout.LayoutParams(0, 8, Gravity.CENTER_VERTICAL))
        loading.addView(track, LinearLayout.LayoutParams(300, 18))

        val lp = FrameLayout.LayoutParams(300, 62).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 78
        }
        root.addView(loading, lp)

        setContentView(root)

        laser.post {
            TranslateAnimation(-140f, root.width.toFloat() + 140f, 0f, 0f).also { sweep ->
                sweep.duration = 1700L
                sweep.repeatCount = TranslateAnimation.INFINITE
                laser.startAnimation(sweep)
            }
        }

        fill.post {
            val target = track.width
            fill.animate().setDuration(1700L).setUpdateListener {
                val w = (target * it.animatedFraction).toInt()
                fill.layoutParams = fill.layoutParams.apply { width = w }
                fill.requestLayout()
            }.start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }, 1900L)
    }
}
