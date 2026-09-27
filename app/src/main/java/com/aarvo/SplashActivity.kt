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
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
        }

        // Clean marketplace-style splash: AARVO's own logo and palette, without the old full-screen artwork.
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "AARVO logo"
        }
        root.addView(logo, FrameLayout.LayoutParams(210, 210).apply {
            gravity = Gravity.CENTER
            bottomMargin = 110
        })

        val brand = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.rgb(91, 33, 214))
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = .08f
            gravity = Gravity.CENTER
        }
        root.addView(brand, FrameLayout.LayoutParams(-1, 50).apply {
            gravity = Gravity.CENTER
            topMargin = 135
        })

        val accent = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.rgb(255, 85, 215),
                    Color.rgb(91, 33, 214),
                    Color.rgb(85, 200, 255),
                    Color.rgb(255, 212, 92)
                )
            )
        }
        root.addView(accent, FrameLayout.LayoutParams(150, 4).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            bottomMargin = 126
        })

        val track = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = 20f
                setColor(0xFFEAE6F2.toInt())
            }
        }
        val fill = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFFFF55D7.toInt(), 0xFF5B21D6.toInt(), 0xFF55C8FF.toInt())
            )
        }
        track.addView(fill, FrameLayout.LayoutParams(0, 6))
        root.addView(track, FrameLayout.LayoutParams(250, 6).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            bottomMargin = 88
        })

        val loading = TextView(this).apply {
            text = "Loading AARVO"
            setTextColor(0xFF77727F.toInt())
            textSize = 12f
            letterSpacing = .08f
            gravity = Gravity.CENTER
        }
        root.addView(loading, FrameLayout.LayoutParams(-1, 36).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            bottomMargin = 42
        })

        setContentView(root)

        track.post {
            ValueAnimator.ofInt(0, track.width).apply {
                duration = 1700L
                addUpdateListener {
                    fill.layoutParams = fill.layoutParams.apply {
                        width = it.animatedValue as Int
                    }
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
