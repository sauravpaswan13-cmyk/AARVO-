package com.aarvo

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
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

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(10, 7, 28), Color.rgb(38, 15, 62), Color.rgb(9, 20, 52))
            )
        }

        // Fresh splash: the existing AARVO logo asset is reused unchanged.
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_logo_webp)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "AARVO"
        }
        root.addView(logo, FrameLayout.LayoutParams(210, 210).apply {
            gravity = Gravity.CENTER
            bottomMargin = 78
        })

        val glow = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, 0x35FFFFFF, 0xAAFFFFFF, 0x35FFFFFF, Color.TRANSPARENT)
            )
            alpha = 0.45f
        }
        root.addView(glow, FrameLayout.LayoutParams(70, 280).apply {
            gravity = Gravity.CENTER
        })

        val tagline = TextView(this).apply {
            text = "SHOP SMART  •  LIVE BETTER"
            setTextColor(Color.WHITE)
            textSize = 12f
            letterSpacing = 0.20f
            gravity = Gravity.CENTER
            alpha = 0.82f
        }
        root.addView(tagline, FrameLayout.LayoutParams(-2, 42).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            bottomMargin = 142
        })

        val track = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = 40f
                setColor(0x22FFFFFF)
                setStroke(1, 0x55FFFFFF)
            }
        }
        val fill = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFFE94B9A.toInt(), 0xFF9B5CFF.toInt(), 0xFF55A8FF.toInt())
            )
        }
        track.addView(fill, FrameLayout.LayoutParams(0, 8, Gravity.CENTER_VERTICAL))
        root.addView(track, FrameLayout.LayoutParams(290, 18).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            bottomMargin = 105
        })

        val loading = TextView(this).apply {
            text = "AARVO"
            setTextColor(0x99FFFFFF.toInt())
            textSize = 9f
            letterSpacing = 0.28f
            gravity = Gravity.CENTER
        }
        root.addView(loading, FrameLayout.LayoutParams(-2, 28).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            bottomMargin = 70
        })

        setContentView(root)

        glow.post {
            val distance = root.width + 180
            ValueAnimator.ofFloat(-140f, distance.toFloat()).apply {
                duration = 1500L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { glow.translationX = it.animatedValue as Float }
                start()
            }
        }

        track.post {
            ValueAnimator.ofInt(0, track.width).apply {
                duration = 1800L
                interpolator = LinearInterpolator()
                addUpdateListener {
                    fill.layoutParams = fill.layoutParams.apply { width = it.animatedValue as Int }
                    fill.requestLayout()
                }
                start()
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }, 2050L)
    }
}
