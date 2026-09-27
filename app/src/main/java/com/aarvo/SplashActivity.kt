package com.aarvo

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(28))
        }

        // Use text for the brand mark here instead of the old raster logo, whose source
        // asset is visibly clipped on-device. This keeps the splash clean and scalable.
        val mark = TextView(this).apply {
            text = "A"
            setTextColor(Color.rgb(91, 33, 214))
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            includeFontPadding = true
        }
        content.addView(mark, LinearLayout.LayoutParams(dp(96), dp(82)).apply {
            bottomMargin = dp(4)
        })

        val brand = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.rgb(91, 33, 214))
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = .08f
            gravity = Gravity.CENTER
            includeFontPadding = true
        }
        content.addView(brand, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(22)
        })

        val accent = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(255,85,215), Color.rgb(91,33,214), Color.rgb(85,200,255), Color.rgb(255,212,92))
            )
        }
        content.addView(accent, LinearLayout.LayoutParams(dp(150), dp(4)).apply {
            bottomMargin = dp(30)
        })

        val track = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(0xFFEAE6F2.toInt())
            }
        }
        val fill = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFFFF55D7.toInt(), 0xFF5B21D6.toInt(), 0xFF55C8FF.toInt())
            )
        }
        track.addView(fill, FrameLayout.LayoutParams(0, dp(7)))
        content.addView(track, LinearLayout.LayoutParams(dp(250), dp(7)).apply {
            bottomMargin = dp(14)
        })

        val loading = TextView(this).apply {
            text = "Loading AARVO"
            setTextColor(0xFF77727F.toInt())
            textSize = 16f
            letterSpacing = .03f
            gravity = Gravity.CENTER
            includeFontPadding = true
        }
        content.addView(loading, LinearLayout.LayoutParams(-1, -2))

        scroll.addView(content, ScrollView.LayoutParams(-1, -1))
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        track.post {
            ValueAnimator.ofInt(0, track.width).apply {
                duration = 1700L
                addUpdateListener {
                    fill.layoutParams = fill.layoutParams.apply { width = it.animatedValue as Int }
                    fill.requestLayout()
                }
                start()
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(android.content.Intent(this, WelcomeActivity::class.java))
            finish()
        }, 2000L)
    }
}
