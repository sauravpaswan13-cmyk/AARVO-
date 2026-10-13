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

        val root = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        root.addView(FlowingCornerLights(this), FrameLayout.LayoutParams(-1, -1))
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        center.addView(ImageView(this).apply {
            setImageResource(R.drawable.aarvo_welcome_hero)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO shopping trolley"
        }, LinearLayout.LayoutParams(dp(76), dp(64)).apply {
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
            handler.postDelayed({ openPhoneLogin() }, 1600L)
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

    /** Subtle animated orange light ribbons confined to opposite screen corners. */
    private class FlowingCornerLights(context: Context) : View(context) {
        private val density = resources.displayMetrics.density
        private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
            phase = (phase + 0.025f) % 1f
            canvas.save()
            // Top-left flowing orange ribbons with soft golden light.
            drawRibbon(canvas, floatArrayOf(
                0f, 0f, w * 0.23f, h * 0.015f, w * 0.20f, h * 0.075f, w * 0.43f, h * 0.105f
            ), 34f, 0x55FF8A00.toInt(), 20f)
            drawRibbon(canvas, floatArrayOf(
                0f, h * 0.095f, w * 0.15f, h * 0.045f, w * 0.19f, h * 0.018f, w * 0.48f, 0f
            ), 2.4f, 0xFFFFA126.toInt(), 0f)
            drawRibbon(canvas, floatArrayOf(
                0f, h * 0.13f, w * 0.11f, h * 0.065f, w * 0.22f, h * 0.025f, w * 0.40f, 0f
            ), 1.3f, 0xFFFFE2A8.toInt(), 0f)
            // Bottom-right matching light ribbons.
            drawRibbon(canvas, floatArrayOf(
                w, h, w * 0.76f, h * 0.985f, w * 0.79f, h * 0.925f, w * 0.56f, h * 0.895f
            ), 34f, 0x55FF8A00.toInt(), 20f)
            drawRibbon(canvas, floatArrayOf(
                w, h * 0.905f, w * 0.85f, h * 0.955f, w * 0.81f, h * 0.982f, w * 0.52f, h
            ), 2.4f, 0xFFFFA126.toInt(), 0f)
            drawRibbon(canvas, floatArrayOf(
                w, h * 0.87f, w * 0.89f, h * 0.935f, w * 0.78f, h * 0.975f, w * 0.60f, h
            ), 1.3f, 0xFFFFE2A8.toInt(), 0f)
            canvas.restore()
            postInvalidateDelayed(40L)
        }

        private fun drawRibbon(canvas: Canvas, p: FloatArray, strokeDp: Float, color: Int, blurDp: Float) {
            val path = Path().apply {
                moveTo(p[0], p[1])
                cubicTo(p[2], p[3], p[4], p[5], p[6], p[7])
            }
            glow.color = color
            glow.strokeWidth = strokeDp * density
            glow.maskFilter = if (blurDp > 0f) BlurMaskFilter(blurDp * density, BlurMaskFilter.Blur.NORMAL) else null
            canvas.drawPath(path, glow)
            glow.maskFilter = null
        }
    }
}
