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
import android.widget.FrameLayout
import android.widget.ImageView
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

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(18, 8, 46), Color.rgb(61, 15, 104), Color.rgb(22, 27, 82))
            )
        }

        val glow = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x3055C8FF.toInt())
            }
            alpha = 0.85f
        }
        root.addView(glow, FrameLayout.LayoutParams(dp(310), dp(310), Gravity.CENTER))

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_entry_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO"
            elevation = dp(10).toFloat()
        }
        root.addView(logo, FrameLayout.LayoutParams(dp(210), dp(210), Gravity.CENTER))

        val laser = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    0x00000000.toInt(), 0x00FFFFFF.toInt(),
                    0xB8FFFFFF.toInt(), 0x00FFFFFF.toInt(), 0x00000000.toInt()
                )
            )
            rotation = -18f
        }
        root.addView(laser, FrameLayout.LayoutParams(dp(34), dp(280), Gravity.CENTER))

        logo.alpha = 0f
        logo.scaleX = .92f
        logo.scaleY = .92f
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(520L).start()

        laser.post {
            ObjectAnimator.ofFloat(laser, View.TRANSLATION_X, -dp(190).toFloat(), dp(190).toFloat()).apply {
                duration = 1200L
                startDelay = 350L
                repeatCount = 1
                repeatMode = ValueAnimator.RESTART
                start()
            }
        }

        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(android.content.Intent(this, WelcomeActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2200L)
    }
}
