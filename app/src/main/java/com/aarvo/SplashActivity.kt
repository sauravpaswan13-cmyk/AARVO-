package com.aarvo

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.util.Log
import androidx.activity.ComponentActivity

/**
 * Lightweight white AARVO splash. No bitmap/logo decoding is performed here,
 * so a damaged or oversized legacy logo cannot crash app startup.
 */
class SplashActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var openingLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            clipChildren = false
            clipToPadding = false
        }

        // Transparent emoji TextViews on a pure white background, floating all around.
        val products = listOf(
            Product("🎁", .03f, .04f, 27, -12f), Product("👟", .25f, .03f, 25, 13f),
            Product("📱", .53f, .04f, 27, -10f), Product("👕", .78f, .05f, 26, 12f),
            Product("🛍️", .88f, .17f, 29, -13f), Product("💄", .06f, .20f, 25, 11f),
            Product("👗", .25f, .19f, 26, -14f), Product("💻", .72f, .22f, 27, 12f),
            Product("🧴", .02f, .37f, 24, -10f), Product("🧊", .83f, .36f, 27, 13f),
            Product("🎀", .14f, .48f, 24, 14f), Product("🧸", .77f, .47f, 25, -12f),
            Product("📺", .02f, .60f, 27, 11f), Product("👚", .24f, .65f, 26, -13f),
            Product("🎧", .75f, .63f, 25, 12f), Product("👜", .89f, .70f, 27, -11f),
            Product("🧥", .06f, .80f, 25, -12f), Product("💍", .31f, .84f, 24, 12f),
            Product("🛒", .56f, .82f, 27, -13f), Product("🪞", .82f, .84f, 25, 11f),
            Product("👠", .18f, .94f, 25, -10f), Product("🎮", .67f, .94f, 25, 12f),
            Product("❄️", .94f, .49f, 23, -10f), Product("🧢", .46f, .14f, 24, 10f),
            Product("📦", .48f, .70f, 25, -11f), Product("🧦", .94f, .91f, 24, 13f)
        )

        products.forEachIndexed { index, item ->
            val icon = TextView(this).apply {
                text = item.emoji
                textSize = item.size.toFloat()
                gravity = Gravity.CENTER
                alpha = 0.48f
                includeFontPadding = false
                // No background, no card, no shadow: the emoji itself floats on white.
                setBackgroundColor(Color.TRANSPARENT)
            }
            root.addView(icon, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP or Gravity.START))
            root.post {
                if (isFinishing || isDestroyed) return@post
                val maxX = (root.width - icon.width).coerceAtLeast(0)
                val maxY = (root.height - icon.height).coerceAtLeast(0)
                icon.x = maxX * item.x
                icon.y = maxY * item.y
                icon.rotation = item.rotation
                ObjectAnimator.ofFloat(icon, View.TRANSLATION_Y, -dp(7).toFloat(), dp(7).toFloat()).apply {
                    duration = 1600L + (index % 7) * 140L
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
                ObjectAnimator.ofFloat(icon, View.ROTATION, item.rotation - 5f, item.rotation + 5f).apply {
                    duration = 2100L + (index % 5) * 170L
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
        }

        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.TRANSPARENT)
            elevation = dp(2).toFloat()
        }

        val trolley = TextView(this).apply {
            text = "🛒"
            textSize = 58f
            gravity = Gravity.CENTER
            includeFontPadding = false
            setBackgroundColor(Color.TRANSPARENT)
        }
        center.addView(trolley, LinearLayout.LayoutParams(dp(100), dp(76)))

        val letters = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.TRANSPARENT)
        }
        val colours = intArrayOf(
            Color.rgb(88, 56, 190), Color.rgb(0, 153, 218), Color.rgb(238, 89, 78),
            Color.rgb(255, 150, 32), Color.rgb(28, 165, 116)
        )
        "AARVO".forEachIndexed { index, char ->
            val letter = TextView(this).apply {
                text = char.toString()
                textSize = 39f
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                setTextColor(colours[index])
                gravity = Gravity.CENTER
                includeFontPadding = false
                alpha = 0f
                scaleX = .65f
                scaleY = .65f
                setBackgroundColor(Color.TRANSPARENT)
            }
            letters.addView(letter, LinearLayout.LayoutParams(-2, dp(58)).apply {
                marginStart = dp(1); marginEnd = dp(1)
            })
            letter.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    letter.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(320L)
                        .setInterpolator(AccelerateDecelerateInterpolator()).start()
                }
            }, 160L + index * 150L)
        }
        center.addView(letters, LinearLayout.LayoutParams(-2, -2))

        val tagline = TextView(this).apply {
            text = "SHOP SMART  •  LIVE BETTER"
            textSize = 11f
            letterSpacing = .12f
            setTextColor(Color.rgb(100, 100, 120))
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setBackgroundColor(Color.TRANSPARENT)
        }
        center.addView(tagline, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        root.addView(center, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
        setContentView(root)

        handler.postDelayed({ openPhoneLogin() }, 2200L)
    }

    private data class Product(
        val emoji: String, val x: Float, val y: Float, val size: Int, val rotation: Float
    )

    private fun openPhoneLogin() {
        if (openingLogin || isFinishing || isDestroyed) return
        openingLogin = true
        try {
            startActivity(Intent(this, PhoneAuthActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        } catch (t: Throwable) {
            openingLogin = false
            Log.e("AARVO-Splash", "Could not open phone login", t)
            // Keep splash alive and retry instead of closing the app.
            handler.postDelayed({ openPhoneLogin() }, 1500L)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
