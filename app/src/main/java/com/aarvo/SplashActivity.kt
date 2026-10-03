package com.aarvo

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this)
        root.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFFFFFFFF.toInt(), 0xFFF7F2FF.toInt(), 0xFFF1F7FF.toInt()))

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_entry_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO"
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        root.addView(logo, FrameLayout.LayoutParams(dp(170), dp(170)).apply { gravity = Gravity.CENTER; topMargin = -dp(70) })

        val brand = TextView(this).apply {
            text = "AARVO"; textSize = 30f; setTextColor(0xFF25233A.toInt()); gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD); letterSpacing = 0.18f
        }
        root.addView(brand, FrameLayout.LayoutParams(-1, dp(48)).apply { gravity = Gravity.CENTER; topMargin = dp(115); leftMargin = dp(28); rightMargin = dp(28) })

        val progress = SplashProgressView(this)
        root.addView(progress, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1900L; interpolator = DecelerateInterpolator()
            addUpdateListener { progress.fraction = it.animatedValue as Float }; start()
        }
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); finish()
        }, 2150L)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private class SplashProgressView(context: android.content.Context) : View(context) {
        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        var fraction = 0f
            set(value) { field = value.coerceIn(0f, 1f); invalidate() }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            val barW = w * 0.62f; val barH = (h * 0.009f).coerceAtLeast(8f)
            val left = (w - barW) / 2f; val top = h * 0.755f; val radius = barH / 2f
            trackPaint.color = 0xFFDDE1EA.toInt()
            canvas.drawRoundRect(left, top, left + barW, top + barH, radius, radius, trackPaint)
            fillPaint.shader = LinearGradient(left, top, left + barW, top, intArrayOf(0xFF1478F2.toInt(), 0xFF7A2CFF.toInt(), 0xFFFF4FA3.toInt()), null, Shader.TileMode.CLAMP)
            val fillW = barW * fraction
            if (fillW > 0f) canvas.drawRoundRect(left, top, left + fillW, top + barH, radius, radius, fillPaint)
            textPaint.color = 0xFF3C3A4D.toInt(); textPaint.textSize = (h * 0.017f).coerceAtLeast(19f)
            textPaint.textAlign = Paint.Align.CENTER; textPaint.letterSpacing = 0.16f
            canvas.drawText("LOADING  " + (fraction * 100).toInt() + "%", w / 2f, top + dp(42), textPaint)
        }
        private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    }
}
