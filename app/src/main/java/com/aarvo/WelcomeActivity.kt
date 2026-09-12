package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat

class WelcomeActivity : ComponentActivity() {
    private val ink = Color.rgb(18, 16, 32)
    private val muted = Color.rgb(103, 98, 118)
    private val violet = Color.rgb(91, 33, 214)
    private val magenta = Color.rgb(232, 34, 119)
    private val orange = Color.rgb(255, 145, 16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(this, false)
        window.statusBarColor = Color.rgb(248, 246, 252)
        window.navigationBarColor = Color.rgb(248, 246, 252)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 44, 24, 20)
            setBackgroundColor(Color.rgb(248, 246, 252))
        }

        // Strong premium brand header.
        root.addView(TextView(this).apply {
            text = "AARVO"
            textSize = 42f
            setTextColor(ink)
            setTypeface(typeface, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
            letterSpacing = .10f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 62))

        root.addView(TextView(this).apply {
            text = "SHOP  •  DISCOVER  •  LOVE"
            textSize = 11f
            setTextColor(violet)
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = .20f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 30))

        root.addView(TextView(this).apply {
            text = "Everything you want.\nOne premium destination."
            textSize = 27f
            setTextColor(ink)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 0)
        }, LinearLayout.LayoutParams(-1, 88))

        root.addView(ShoppingBagsView(this), LinearLayout.LayoutParams(-1, 360).apply {
            topMargin = 8
            bottomMargin = 8
        })

        root.addView(TextView(this).apply {
            text = "No verification needed to start shopping"
            textSize = 14f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 34))

        val continueShopping = Button(this).apply {
            text = "CONTINUE SHOPPING   ›"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(violet, magenta)).apply { cornerRadius = 30f }
            elevation = 8f
            setOnClickListener { enterApp() }
        }
        root.addView(continueShopping, LinearLayout.LayoutParams(-1, 62).apply { topMargin = 10; bottomMargin = 10 })

        val login = Button(this).apply {
            text = "LOGIN / SIGN UP"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(violet)
            isAllCaps = false
            background = GradientDrawable().apply {
                cornerRadius = 30f
                setColor(Color.TRANSPARENT)
                setStroke(2, violet)
            }
            setOnClickListener { openLogin() }
        }
        root.addView(login, LinearLayout.LayoutParams(-1, 58).apply { bottomMargin = 8 })

        root.addView(TextView(this).apply {
            text = "Secure shopping  •  Fast delivery  •  Easy returns"
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(-1, 30))

        setContentView(root)
    }

    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterApp() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", true).putBoolean("signed_in", false).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun openLogin() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", false).apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java))
        finish()
    }

    private class ShoppingBagsView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w / 2f
            val base = h * .86f

            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(237, 232, 248)
            canvas.drawOval(cx - w * .39f, h * .08f, cx + w * .39f, h * .91f, paint)

            drawBag(canvas, cx - w * .22f, base, w * .28f, h * .52f, Color.rgb(255, 159, 12), Color.rgb(255, 198, 43))
            drawBag(canvas, cx + w * .02f, base + 4, w * .27f, h * .57f, Color.rgb(232, 34, 119), Color.rgb(255, 73, 145))
            drawBag(canvas, cx + w * .25f, base + 8, w * .20f, h * .46f, Color.rgb(91, 33, 214), Color.rgb(122, 66, 238))

            paint.color = magenta()
            canvas.drawCircle(cx - w * .34f, h * .34f, 30f, paint)
            paint.color = Color.WHITE
            paint.textSize = 30f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("%", cx - w * .34f - 15, h * .34f + 10, paint)

            paint.color = violet()
            canvas.drawRoundRect(cx + w * .32f - 34, h * .23f, cx + w * .32f + 34, h * .23f + 68, 18f, 18f, paint)
            paint.color = Color.WHITE
            paint.textSize = 34f
            canvas.drawText("♡", cx + w * .32f - 18, h * .23f + 45, paint)
        }

        private fun violet() = Color.rgb(91, 33, 214)
        private fun magenta() = Color.rgb(232, 34, 119)

        private fun drawBag(canvas: Canvas, x: Float, bottom: Float, bagW: Float, bagH: Float, body: Int, top: Int) {
            val left = x - bagW / 2f
            val right = x + bagW / 2f
            val topY = bottom - bagH
            paint.style = Paint.Style.FILL
            paint.color = body
            canvas.drawRoundRect(left, topY + 30, right, bottom, 14f, 14f, paint)

            paint.color = top
            canvas.drawRect(left, topY + 30, right, topY + 58, paint)

            paint.color = Color.argb(42, 255, 255, 255)
            canvas.drawRect(left + bagW * .18f, topY + 30, left + bagW * .24f, bottom - 6, paint)
            canvas.drawRect(left + bagW * .52f, topY + 30, left + bagW * .58f, bottom - 6, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 9f
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = top
            val path = Path()
            path.moveTo(x - bagW * .28f, topY + 39)
            path.cubicTo(x - bagW * .26f, topY - 62, x + bagW * .26f, topY - 62, x + bagW * .28f, topY + 39)
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
        }
    }
}
