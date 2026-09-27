package com.aarvo

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        // Premium, uncluttered AARVO launch screen.
        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(24, 12, 55),
                    Color.rgb(67, 20, 105),
                    Color.rgb(34, 28, 91)
                )
            )
        }

        // Very subtle ambient glow behind the real AARVO mark.
        val glow = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x3355C8FF)
            }
            alpha = 0.72f
        }
        root.addView(
            glow,
            FrameLayout.LayoutParams(dp(270), dp(270), Gravity.CENTER)
        )

        val content = FrameLayout(this).apply {
            foregroundGravity = Gravity.CENTER
        }

        val mark = ImageView(this).apply {
            // Keep the supplied AARVO logo asset; do not substitute a text-only logo.
            setImageResource(R.drawable.aarvo_entry_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO logo"
            elevation = dp(8).toFloat()
        }
        content.addView(
            mark,
            FrameLayout.LayoutParams(dp(190), dp(190), Gravity.CENTER)
        )

        val brand = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.WHITE)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.10f
            gravity = Gravity.CENTER
            includeFontPadding = false
            alpha = 0.96f
        }
        content.addView(
            brand,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                topMargin = dp(215)
                leftMargin = dp(32)
                rightMargin = dp(32)
            }
        )

        // A restrained premium light sweep across the logo.
        val laser = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    0x00000000,
                    0x00FFFFFF,
                    0x99FFFFFF,
                    0x00FFFFFF,
                    0x00000000
                )
            )
            rotation = -18f
            alpha = 0.0f
        }
        content.addView(
            laser,
            FrameLayout.LayoutParams(dp(30), dp(250), Gravity.CENTER)
        )

        root.addView(content, FrameLayout.LayoutParams(-1, -1))

        setContentView(root)

        laser.post {
            val startX = -dp(170).toFloat()
            val endX = dp(170).toFloat()
            ObjectAnimator.ofFloat(laser, View.TRANSLATION_X, startX, endX).apply {
                duration = 1100L
                startDelay = 450L
                repeatCount = 1
                repeatMode = ValueAnimator.RESTART
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: android.animation.Animator) {
                        laser.alpha = 0.75f
                    }
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        laser.alpha = 0.0f
                    }
                })
                start()
            }
        }

        // Give the premium splash enough time to be visible, then enter Welcome.
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(android.content.Intent(this, WelcomeActivity::class.java))
            finish()
        }, 2200L)
    }
}
