package com.aarvo

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Context
import androidx.activity.ComponentActivity

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

        val root = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        center.addView(ShoppingCartView(this), LinearLayout.LayoutParams(dp(76), dp(64)).apply {
            bottomMargin = dp(7)
        })

        val letters = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
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
            }
            letters.addView(letter, LinearLayout.LayoutParams(-2, dp(58)).apply {
                marginStart = dp(1)
                marginEnd = dp(1)
            })
        }
        center.addView(letters, LinearLayout.LayoutParams(-2, -2))
        val tagline = TextView(this).apply {
            text = "SHOP SMART  •  LIVE BETTER"
            textSize = 11f
            letterSpacing = .12f
            setTextColor(Color.rgb(100, 100, 120))
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        center.addView(tagline, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        root.addView(center, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
        setContentView(root)
        handler.postDelayed({ openPhoneLogin() }, 1600L)
    }

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
            handler.postDelayed({ openPhoneLogin() }, 1500L)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private class ShoppingCartView(context: Context) : View(context) {
        private val scale = resources.displayMetrics.density
        private val purple = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(88, 56, 190)
            style = Paint.Style.STROKE
            strokeWidth = 4.5f * scale
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val orange = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 150, 32)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
            strokeCap = Paint.Cap.ROUND
        }
        private val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 153, 218)
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val basket = Path().apply {
                moveTo(cx - 19f * scale, height * .30f)
                lineTo(cx - 12f * scale, height * .61f)
                lineTo(cx + 19f * scale, height * .61f)
                lineTo(cx + 26f * scale, height * .39f)
                lineTo(cx - 9f * scale, height * .39f)
            }
            canvas.drawLine(cx - 27f * scale, height * .22f, cx - 19f * scale, height * .30f, purple)
            canvas.drawPath(basket, purple)
            canvas.drawLine(cx - 5f * scale, height * .66f, cx + 18f * scale, height * .66f, purple)
            canvas.drawLine(cx - 4f * scale, height * .44f, cx + 21f * scale, height * .44f, orange)
            canvas.drawLine(cx - 2f * scale, height * .52f, cx + 19f * scale, height * .52f, orange)
            canvas.drawCircle(cx - 2f * scale, height * .75f, 3.8f * scale, blue)
            canvas.drawCircle(cx + 17f * scale, height * .75f, 3.8f * scale, blue)
        }
    }
}
