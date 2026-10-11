package com.aarvo

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * Lightweight native splash: no network work, no Compose, and no indefinite loading.
 * Shows floating shopping-category icons and animated colourful AARVO lettering,
 * then opens phone login directly.
 */
class SplashActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var openedLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.WHITE

        val root = FrameLayout(this).apply {
            setBackground(GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.WHITE, Color.rgb(250, 252, 255), Color.rgb(255, 249, 244))
            ))
        }

        // Floating shopping categories, spaced around the brand and independently animated.
        val products = listOf(
            Product("🎁", 0.08f, 0.12f, 22, -16f),
            Product("👟", 0.72f, 0.11f, 25, 14f),
            Product("📱", 0.12f, 0.31f, 27, 12f),
            Product("👕", 0.76f, 0.30f, 27, -13f),
            Product("🛍️", 0.04f, 0.52f, 28, 15f),
            Product("💄", 0.81f, 0.48f, 25, -15f),
            Product("👗", 0.14f, 0.70f, 27, -12f),
            Product("💻", 0.74f, 0.69f, 25, 12f),
            Product("🧴", 0.08f, 0.84f, 23, -13f),
            Product("🧊", 0.76f, 0.84f, 25, 14f),
            Product("🧥", 0.88f, 0.58f, 24, 12f),
            Product("🎀", 0.88f, 0.22f, 23, -12f),
            Product("🧸", 0.28f, 0.10f, 22, 10f),
            Product("🪞", 0.62f, 0.86f, 23, -12f)
        )
        products.forEachIndexed { index, item ->
            val icon = TextView(this).apply {
                text = item.emoji
                textSize = item.size.toFloat()
                gravity = Gravity.CENTER
                elevation = dp(5).toFloat()
                setShadowLayer(dp(5).toFloat(), 0f, dp(2).toFloat(), Color.argb(38, 50, 40, 90))
            }
            val params = FrameLayout.LayoutParams(dp(54), dp(54), Gravity.TOP or Gravity.START)
            root.addView(icon, params)
            root.post {
                val maxX = (root.width - icon.width).coerceAtLeast(0)
                val maxY = (root.height - icon.height).coerceAtLeast(0)
                icon.x = maxX * item.x
                icon.y = maxY * item.y
                icon.rotation = item.rotation
                val bob = ObjectAnimator.ofPropertyValuesHolder(
                    icon,
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -dp(9).toFloat(), dp(9).toFloat()),
                    PropertyValuesHolder.ofFloat(View.ROTATION, item.rotation - 5f, item.rotation + 5f)
                ).apply {
                    duration = (1500L + index * 95L)
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                }
                bob.start()
            }
        }

        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val logo = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO PNG logo"
            // A missing/invalid logo must never crash the splash screen.
            try {
                setImageBitmap(LogoUtils.loadTransparentLogo(this@SplashActivity))
            } catch (_: Throwable) {
                visibility = View.GONE
            }
        }
        center.addView(logo, LinearLayout.LayoutParams(dp(92), dp(92)).apply {
            bottomMargin = dp(5)
        })

        // Premium shopping trolley sits directly above the colourful AARVO wordmark.
        val trolley = TextView(this).apply {
            text = "🛒"
            textSize = 43f
            gravity = Gravity.CENTER
            elevation = dp(6).toFloat()
            setShadowLayer(dp(3).toFloat(), dp(1).toFloat(), dp(3).toFloat(), Color.argb(60, 60, 45, 110))
        }
        center.addView(trolley, LinearLayout.LayoutParams(dp(84), dp(58)).apply {
            bottomMargin = dp(0)
        })
        trolley.post {
            trolley.animate().translationY(-dp(3).toFloat()).setDuration(700)
                .withEndAction {
                    if (!isFinishing && !isDestroyed) {
                        trolley.animate().translationY(0f).setDuration(700).start()
                    }
                }.start()
        }

        val letters = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val letterColours = intArrayOf(
            Color.rgb(88, 56, 190), Color.rgb(0, 153, 218),
            Color.rgb(238, 89, 78), Color.rgb(255, 150, 32), Color.rgb(28, 165, 116)
        )
        "AARVO".forEachIndexed { index, char ->
            val letter = TextView(this).apply {
                text = char.toString()
                textSize = 37f
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                setTextColor(letterColours[index])
                setShadowLayer(dp(1.5f).toFloat(), dp(2).toFloat(), dp(3).toFloat(), Color.rgb(175, 175, 190))
                gravity = Gravity.CENTER
                alpha = 0f
                scaleX = 0.45f
                scaleY = 0.45f
            }
            letters.addView(letter, LinearLayout.LayoutParams(-2, dp(55)).apply {
                marginStart = dp(1)
                marginEnd = dp(1)
            })
            letter.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    letter.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(360).setInterpolator(AccelerateDecelerateInterpolator()).start()
                }
            }, 180L + index * 170L)
        }
        center.addView(letters, LinearLayout.LayoutParams(-2, -2))

        val tagline = TextView(this).apply {
            text = "SHOP SMART  •  LIVE BETTER"
            textSize = 11f
            letterSpacing = 0.13f
            setTextColor(Color.rgb(103, 100, 125))
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        center.addView(tagline, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(9)
        })

        val shine = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(88, 56, 190), Color.rgb(0, 170, 205), Color.rgb(255, 150, 32))
            ).apply { cornerRadius = dp(4).toFloat() }
        }
        center.addView(shine, LinearLayout.LayoutParams(dp(82), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(18)
        })
        root.addView(center, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
        setContentView(root)

        // Keep splash duration bounded; it must never sit on an endless loading indicator.
        handler.postDelayed({ openPhoneLogin() }, 2400L)
    }

    private data class Product(
        val emoji: String,
        val x: Float,
        val y: Float,
        val size: Int,
        val rotation: Float
    )

    private fun openPhoneLogin() {
        if (openedLogin || isFinishing || isDestroyed) return
        openedLogin = true
        try {
            startActivity(Intent(this, PhoneAuthActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        } catch (t: Throwable) {
            // If launching login fails, leave the branded screen visible rather than
            // immediately closing the whole app; logs will identify the underlying issue.
            openedLogin = false
            android.util.Log.e("AARVO-Splash", "Could not open phone login", t)
            handler.postDelayed({ openPhoneLogin() }, 1200L)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
}
