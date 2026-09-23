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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(18, 7, 58)
        window.navigationBarColor = Color.rgb(12, 5, 40)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(32), dp(28), dp(32))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(8, 5, 30),
                    Color.rgb(32, 12, 76),
                    Color.rgb(76, 25, 126),
                    Color.rgb(25, 38, 105),
                    Color.rgb(7, 6, 34)
                )
            )
        }

        val logo = ImageView(this).apply {
            setImageResource(com.aarvo.R.drawable.aarvo_top_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0f
            scaleX = .78f
            scaleY = .78f
            elevation = dp(10).toFloat()
            contentDescription = "AARVO logo"
        }
        root.addView(logo, LinearLayout.LayoutParams(dp(144), dp(144)).apply {
            bottomMargin = dp(14)
        })

        val name = TextView(this).apply {
            text = "AARVO"
            textSize = 46f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.create("sans-serif", Typeface.BOLD))
            gravity = Gravity.CENTER
            letterSpacing = .13f
            alpha = 0f
            setShadowLayer(dp(16).toFloat(), 0f, dp(4).toFloat(), Color.argb(190, 0, 0, 0))
        }
        root.addView(name, LinearLayout.LayoutParams(-1, dp(62)))

        root.addView(TextView(this).apply {
            text = "CURATED SHOPPING  •  AARVO"
            textSize = 11f
            setTextColor(Color.rgb(255, 220, 112))
            setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
            gravity = Gravity.CENTER
            letterSpacing = .16f
            alpha = .88f
        }, LinearLayout.LayoutParams(-1, dp(30)).apply {
            topMargin = dp(4)
        })

        val divider = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.rgb(255, 215, 90),
                    Color.TRANSPARENT
                )
            )
            alpha = 0f
        }
        root.addView(divider, LinearLayout.LayoutParams(dp(150), dp(2)).apply {
            topMargin = dp(20)
            bottomMargin = dp(18)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "A premium marketplace for everyday life."
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            alpha = .86f
        }, LinearLayout.LayoutParams(-1, dp(36)))

        val loading = TextView(this).apply {
            text = "AARVO  •  LOADING"
            textSize = 10f
            setTextColor(Color.argb(205, 255, 255, 255))
            gravity = Gravity.CENTER
            letterSpacing = .12f
            alpha = 0f
        }
        root.addView(loading, LinearLayout.LayoutParams(-1, dp(32)).apply {
            topMargin = dp(12)
        })

        setContentView(root)

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(650)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        name.animate()
            .alpha(1f)
            .setStartDelay(260)
            .setDuration(500)
            .start()

        divider.animate()
            .alpha(1f)
            .setStartDelay(520)
            .setDuration(450)
            .start()

        loading.animate()
            .alpha(1f)
            .setStartDelay(700)
            .setDuration(450)
            .start()

        val pulse = ObjectAnimator.ofFloat(logo, "alpha", 1f, .82f, 1f).apply {
            duration = 1400
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        pulse.start()

        Handler(Looper.getMainLooper()).postDelayed({
            pulse.cancel()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }, 1900)
    }
}
