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
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 22f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        paint.shader = LinearGradient(0f, h, w, 0f, intArrayOf(Color.rgb(255,174,0), Color.rgb(255,104,31), Color.rgb(235,38,111), Color.rgb(93,40,205)), null, Shader.TileMode.CLAMP)
        val a = Path().apply { moveTo(w*0.20f,h*0.82f); lineTo(w*0.50f,h*0.12f); lineTo(w*0.82f,h*0.82f) }
        canvas.drawPath(a, paint)
        paint.style = Paint.Style.FILL; paint.shader = null; paint.color = Color.WHITE
        canvas.drawCircle(w*0.50f,h*0.48f,w*0.09f,paint)
        paint.style = Paint.Style.STROKE; paint.shader = LinearGradient(0f, 0f, w, h, Color.rgb(255,186,0), Color.rgb(255,57,126), Shader.TileMode.CLAMP); paint.strokeWidth = 16f
        canvas.drawLine(w*0.31f,h*0.55f,w*0.69f,h*0.55f,paint)
    }
}

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(32,32,32,32); setBackgroundColor(Color.rgb(39,0,116)) }
        root.addView(AarvoSplashLogoView(this), LinearLayout.LayoutParams(170,150).apply { gravity = Gravity.CENTER })
        val title = TextView(this).apply { text = "AARVO"; setTextColor(Color.WHITE); textSize = 44f; gravity = Gravity.CENTER; setTypeface(typeface, Typeface.BOLD) }
        root.addView(title, LinearLayout.LayoutParams(-1,-2))
        val tagline = TextView(this).apply { text = "Shop Smart  •  Live Better"; setTextColor(Color.WHITE); textSize = 17f; gravity = Gravity.CENTER; setPadding(0,8,0,0) }
        root.addView(tagline, LinearLayout.LayoutParams(-1,-2))
        val progress = ProgressBar(this).apply { isIndeterminate = true }
        root.addView(progress, LinearLayout.LayoutParams(58,58).apply { gravity=Gravity.CENTER_HORIZONTAL; topMargin=40 })
        val loading = TextView(this).apply { text="Loading your world..."; setTextColor(Color.WHITE); textSize=14f; gravity=Gravity.CENTER; alpha=.92f; setPadding(0,12,0,0) }
        root.addView(loading, LinearLayout.LayoutParams(-1,-2))
        setContentView(root)

        // AARVO opens directly into shopping without requiring verification.
        // Login/OTP remains available from Account and is required at purchase time.
        Handler(Looper.getMainLooper()).postDelayed({
            getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("onboarded", true)
                .putBoolean("signed_in", false)
                .putBoolean("guest_mode", true)
                .apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500L)
    }
}
