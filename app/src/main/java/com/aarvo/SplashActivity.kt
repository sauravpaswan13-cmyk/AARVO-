package com.aarvo

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }

        val bytes = Base64.decode(WelcomeReferenceImage.WEBP_BASE64, Base64.DEFAULT)
        val artwork = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            // Android 10 devices can show GPU corruption on large animated/WebP-backed bitmaps.
            // Render this static full-screen artwork through the software pipeline for clean pixels.
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = "AARVO Splash"
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        root.addView(artwork, FrameLayout.LayoutParams(-1, -1))

        // Cover only the old static loading area, then draw the same loading box
        // programmatically so it fills smoothly from 0% to 100%.
        val progress = SplashProgressView(this)
        root.addView(progress, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1900L
            addUpdateListener { progress.fraction = it.animatedValue as Float }
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2150L)
    }

    private class SplashProgressView(context: android.content.Context) : View(context) {
        private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        var fraction: Float = 0f
            set(value) { field = value.coerceIn(0f, 1f); invalidate() }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()

            // Exact proportional position of the supplied splash artwork.
            val barW = w * 0.434f
            val barH = (h * 0.009f).coerceAtLeast(8f)
            val left = (w - barW) / 2f
            val top = h * 0.667f
            val radius = barH / 2f

            maskPaint.color = Color.WHITE
            canvas.drawRect(left - 8f, top - 18f, left + barW + 8f, top + h * 0.075f, maskPaint)

            trackPaint.color = 0xFFE1E4EA.toInt()
            canvas.drawRoundRect(left, top, left + barW, top + barH, radius, radius, trackPaint)

            fillPaint.shader = LinearGradient(
                left, top, left + barW, top,
                intArrayOf(0xFF1478F2.toInt(), 0xFF7A2CFF.toInt()),
                null, Shader.TileMode.CLAMP
            )
            val fillW = barW * fraction
            if (fillW > 0f) {
                canvas.drawRoundRect(left, top, left + fillW, top + barH, radius, radius, fillPaint)
            }

            textPaint.shader = null
            textPaint.color = 0xFF24304B.toInt()
            textPaint.textSize = (h * 0.0175f).coerceAtLeast(20f)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.letterSpacing = 0.24f
            canvas.drawText("LOADING...", w / 2f, h * 0.721f, textPaint)
        }
    }
}
