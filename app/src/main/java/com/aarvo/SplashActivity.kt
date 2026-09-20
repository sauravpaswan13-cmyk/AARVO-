package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity

private class AarvoSplashLogoView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        paint.shader = LinearGradient(0f, h, w, 0f, Color.rgb(255, 226, 111), Color.rgb(255, 177, 0), Shader.TileMode.CLAMP)
        val a = Path().apply {
            moveTo(w * .20f, h * .82f)
            lineTo(w * .50f, h * .12f)
            lineTo(w * .82f, h * .82f)
        }
        canvas.drawPath(a, paint)
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = Color.WHITE
        canvas.drawCircle(w * .50f, h * .48f, w * .075f, paint)
        paint.style = Paint.Style.STROKE
        paint.shader = LinearGradient(0f, 0f, w, h, Color.rgb(255, 231, 126), Color.rgb(255, 178, 0), Shader.TileMode.CLAMP)
        paint.strokeWidth = 13f
        canvas.drawLine(w * .31f, h * .55f, w * .69f, h * .55f, paint)
    }
}

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(3, 22, 18))
        }
        root.addView(AarvoSplashLogoView(this), LinearLayout.LayoutParams(180, 160).apply { gravity = Gravity.CENTER })
        val title = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.rgb(255, 204, 54))
            textSize = 44f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = .08f
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))
        val tagline = TextView(this).apply {
            text = "BETTER CHOICE  •  BRIGHTER LIFE"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 0)
            letterSpacing = .10f
        }
        root.addView(tagline, LinearLayout.LayoutParams(-1, -2))
        val progress = ProgressBar(this).apply { isIndeterminate = true }
        root.addView(progress, LinearLayout.LayoutParams(48, 48).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = 44 })
        val loading = TextView(this).apply {
            text = "Preparing your AARVO experience..."
            setTextColor(Color.rgb(173, 194, 186))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }
        root.addView(loading, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }, 1500L)
    }
}
