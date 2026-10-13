package com.aarvo

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.BlurMaskFilter
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.app.AlertDialog
import android.widget.Toast
import java.io.File
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Context
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var openingLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashRecorder()
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        val pageColor = Color.parseColor("#F7F5FF")
        window.statusBarColor = pageColor
        window.navigationBarColor = pageColor
        val root = FrameLayout(this).apply { setBackgroundColor(pageColor) }
        // Corner orange frames removed per user specification
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        center.addView(ImageView(this).apply {
            setImageResource(R.drawable.aarvo_brand_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO brand logo"
        }, LinearLayout.LayoutParams(dp(110), dp(110)).apply {
            bottomMargin = dp(7)
        })

        val letters = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        "AARVO".forEach { char ->
            val letter = TextView(this).apply {
                text = char.toString()
                textSize = 39f
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                setTextColor(Color.rgb(255, 150, 32))
                gravity = Gravity.CENTER
                includeFontPadding = false
            }
            letters.addView(letter, LinearLayout.LayoutParams(-2, dp(58)).apply {
                marginStart = dp(1)
                marginEnd = dp(1)
            })
        }
        center.addView(letters, LinearLayout.LayoutParams(-2, -2))
        android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1800L
            repeatCount = android.animation.ValueAnimator.INFINITE
            addUpdateListener { va ->
                val frac = va.animatedFraction
                for (i in 0 until letters.childCount) {
                    val tv = letters.getChildAt(i) as? TextView ?: continue
                    val offset = (frac + i * 0.15f) % 1f
                    val wave = Math.sin(offset.toDouble() * Math.PI).toFloat().coerceIn(0f, 1f)
                    val r = (255 * (0.85f + 0.15f * wave)).toInt().coerceIn(0, 255)
                    val g = (120 + 90 * wave).toInt().coerceIn(0, 255)
                    val b = (20 * wave).toInt().coerceIn(0, 255)
                    tv.setTextColor(Color.rgb(r, g, b))
                }
            }
            start()
        }
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
        val report = File(filesDir, "aarvo_crash_report.txt")
        if (report.exists()) {
            showCrashReport(report)
        } else {
            handler.postDelayed({ openPhoneLogin() }, 1300L)
        }
    }

    private fun installCrashRecorder() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val report = File(filesDir, "aarvo_crash_report.txt")
                report.writeText(buildString {
                    appendLine("AARVO crash diagnostic")
                    appendLine("Time: ${java.util.Date()}")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
                    appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                    appendLine()
                    append(error.stackTraceToString())
                })
            } catch (_: Throwable) { }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun showCrashReport(report: File) {
        val details = runCatching { report.readText() }.getOrDefault("Crash report could not be read.")
        AlertDialog.Builder(this)
            .setTitle("AARVO crash report found")
            .setMessage("The previous launch crashed. Tap Copy report and send the copied text here so the exact cause can be fixed.\n\n" + details.take(1800))
            .setPositiveButton("Copy report") { dialog, _ ->
                runCatching {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("AARVO crash report", details))
                    report.delete()
                    Toast.makeText(this, "Crash report copied. Paste it into this chat.", Toast.LENGTH_LONG).show()
                }.onFailure { Toast.makeText(this, "Could not copy report", Toast.LENGTH_LONG).show() }
                dialog.dismiss()
                handler.postDelayed({ openPhoneLogin() }, 500L)
            }
            .setNegativeButton("Continue") { dialog, _ ->
                report.delete()
                dialog.dismiss()
                handler.postDelayed({ openPhoneLogin() }, 500L)
            }
            .setCancelable(false)
            .show()
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

    /** Glowing orange corner-frame ribbons, softly pulsing like flowing light. */
    private class FlowingCornerLights(context: Context) : View(context) {
        private val density = resources.displayMetrics.density
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private var phase = 0f

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return
            phase = (phase + 0.035f) % (Math.PI.toFloat() * 2f)
            val pulse = (0.72f + 0.28f * kotlin.math.sin(phase)).coerceIn(0.35f, 1f)
            canvas.save()

            // Broad translucent orange frame bands in the upper-left and lower-right.
            drawCurve(canvas, floatArrayOf(0f, 0f, w*.23f, h*.008f, w*.18f, h*.075f, w*.43f, h*.105f),
                38f, 0x33FF8A00, pulse, 18f)
            drawCurve(canvas, floatArrayOf(0f, h*.10f, w*.12f, h*.055f, w*.22f, h*.018f, w*.44f, 0f),
                12f, 0x44FF9B18, pulse, 8f)
            drawCurve(canvas, floatArrayOf(0f, h*.095f, w*.14f, h*.045f, w*.19f, h*.018f, w*.48f, 0f),
                2.6f, 0xFFFFA126.toInt(), pulse, 0f)
            drawCurve(canvas, floatArrayOf(0f, h*.13f, w*.11f, h*.065f, w*.22f, h*.025f, w*.40f, 0f),
                1.4f, 0xFFFFE7B5.toInt(), pulse, 0f)

            drawCurve(canvas, floatArrayOf(w, h, w*.77f, h*.992f, w*.82f, h*.925f, w*.57f, h*.895f),
                38f, 0x33FF8A00, pulse, 18f)
            drawCurve(canvas, floatArrayOf(w, h*.90f, w*.88f, h*.945f, w*.78f, h*.982f, w*.54f, h),
                12f, 0x44FF9B18, pulse, 8f)
            drawCurve(canvas, floatArrayOf(w, h*.905f, w*.85f, h*.955f, w*.81f, h*.982f, w*.52f, h),
                2.6f, 0xFFFFA126.toInt(), pulse, 0f)
            drawCurve(canvas, floatArrayOf(w, h*.87f, w*.89f, h*.935f, w*.78f, h*.975f, w*.60f, h),
                1.4f, 0xFFFFE7B5.toInt(), pulse, 0f)
            canvas.restore()
            postInvalidateDelayed(45L)
        }

        private fun drawCurve(canvas: Canvas, p: FloatArray, widthDp: Float, color: Int, pulse: Float, blurDp: Float) {
            val path = Path().apply {
                moveTo(p[0], p[1])
                cubicTo(p[2], p[3], p[4], p[5], p[6], p[7])
            }
            paint.color = color
            paint.alpha = (Color.alpha(color) * pulse).toInt().coerceIn(0, 255)
            paint.strokeWidth = widthDp * density
            paint.maskFilter = if (blurDp > 0f) BlurMaskFilter(blurDp * density, BlurMaskFilter.Blur.NORMAL) else null
            canvas.drawPath(path, paint)
            paint.maskFilter = null
            paint.alpha = 255
        }
    }
}
